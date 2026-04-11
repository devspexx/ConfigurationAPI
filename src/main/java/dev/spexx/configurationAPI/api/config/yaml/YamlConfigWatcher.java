package dev.spexx.configurationAPI.api.config.yaml;

import dev.spexx.configurationAPI.api.event.ConfigDeletedEvent;
import dev.spexx.configurationAPI.api.event.ConfigRegisteredEvent;
import dev.spexx.configurationAPI.api.event.ConfigReloadedEvent;
import dev.spexx.configurationAPI.api.exceptions.ConfigException;
import dev.spexx.configurationAPI.api.utils.FileChecksum;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Watches registered {@link YamlConfig} files for changes.
 *
 * <p>Only files registered through this watcher are monitored.
 * File system events for unrelated files are ignored.</p>
 *
 * <p>Events are dispatched on the main server thread.</p>
 *
 * @since 1.3.0
 */
public class YamlConfigWatcher {

    private static final long DEBOUNCE_MS = 200;
    private final @NotNull JavaPlugin javaPlugin;
    private final @NotNull WatchService watchService;
    private final @NotNull Map<Path, WatchKey> directories = new ConcurrentHashMap<>();
    private final @NotNull Map<Path, YamlConfig> watchedFiles = new ConcurrentHashMap<>();
    private final @NotNull Map<Path, Long> lastModified = new ConcurrentHashMap<>();
    private final @NotNull Map<WatchKey, Path> watchKeys = new ConcurrentHashMap<>();
    private Thread watcherThread;
    private volatile boolean running = false;

    /**
     * Creates a new watcher instance.
     *
     * @param javaPlugin the plugin used for scheduling and logging
     * @throws ConfigException if initialization fails
     */
    public YamlConfigWatcher(@NotNull JavaPlugin javaPlugin) throws ConfigException {
        this.javaPlugin = javaPlugin;
        try {
            this.watchService = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            throw new ConfigException("Failed to initialize WatchService", e);
        }
    }

    /**
     * Registers a configuration for monitoring.
     *
     * <p>Only registered configurations will produce events.</p>
     *
     * @param config the configuration to watch
     * @throws ConfigException if already registered or invalid
     */
    public void watch(@NotNull YamlConfig config) throws ConfigException {
        Path path = config.getFile().toPath().toAbsolutePath().normalize();

        if (watchedFiles.containsKey(path)) {
            throw new ConfigException("File is already being watched: " + path);
        }

        Path directory = path.getParent();
        if (directory == null) {
            throw new ConfigException("File has no parent directory: " + path);
        }

        try {
            directories.computeIfAbsent(directory, dir -> {
                try {
                    WatchKey key = dir.register(
                            watchService,
                            StandardWatchEventKinds.ENTRY_MODIFY,
                            StandardWatchEventKinds.ENTRY_DELETE,
                            StandardWatchEventKinds.ENTRY_CREATE
                    );

                    watchKeys.put(key, dir);
                    return key;

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            watchedFiles.put(path, config);

            // REGISTER EVENT
            Bukkit.getScheduler().runTask(javaPlugin, () ->
                    Bukkit.getPluginManager().callEvent(
                            new ConfigRegisteredEvent(
                                    config.getFile().getName(),
                                    config.get(),
                                    config.getCachedChecksum()
                            )
                    )
            );

        } catch (RuntimeException e) {
            throw new ConfigException("Failed to register file watcher: " + path, e);
        }
    }

    /**
     * Starts the watcher thread.
     *
     * @throws ConfigException if already running
     */
    public void start() throws ConfigException {
        if (running) {
            throw new ConfigException("Watcher is already running");
        }

        running = true;

        watcherThread = new Thread(this::run, "YamlConfigWatcher");
        watcherThread.setDaemon(true);
        watcherThread.start();
    }

    /**
     * Stops the watcher and releases resources.
     */
    public void stop() {
        running = false;

        try {
            watchService.close();
        } catch (IOException ignored) {
        }

        if (watcherThread != null) {
            watcherThread.interrupt();
        }
    }

    /**
     * Returns whether the watcher is running.
     *
     * @return true if running
     */
    public boolean isRunning() {
        return running;
    }

    private void run() {
        var scheduler = Bukkit.getScheduler();
        var pluginManager = Bukkit.getPluginManager();

        while (running) {
            WatchKey key;

            try {
                key = watchService.take();
            } catch (InterruptedException | ClosedWatchServiceException e) {
                return;
            }

            @Nullable Path dir = watchKeys.get(key);
            if (dir == null) {
                key.reset();
                continue;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                Path changed = dir.resolve((Path) event.context())
                        .toAbsolutePath()
                        .normalize();

                YamlConfig config = watchedFiles.get(changed);
                if (config == null) {
                    continue;
                }

                // DELETE
                if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {

                    String name = config.getFile().getName();

                    watchedFiles.remove(changed);

                    scheduler.runTask(javaPlugin, () ->
                            pluginManager.callEvent(
                                    new ConfigDeletedEvent(name, changed)
                            )
                    );

                    continue;
                }

                // MODIFY / CREATE
                if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY
                        || event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {

                    Path filePath = config.getFile().toPath();

                    // skip if file was deleted
                    if (!Files.exists(filePath)) {
                        continue;
                    }

                    @Nullable String oldChecksum = config.getCachedChecksum();
                    @Nullable String newChecksum;

                    try {
                        newChecksum = FileChecksum.computeSha256(config.getFile());
                    } catch (Exception e) {
                        javaPlugin.getLogger().log(Level.SEVERE,
                                "Failed to compute checksum for " + config.getFile(), e);
                        continue;
                    }

                    if (oldChecksum != null && oldChecksum.equals(newChecksum)) {
                        continue;
                    }

                    long now = System.currentTimeMillis();
                    long last = lastModified.getOrDefault(changed, 0L);
                    if (now - last < DEBOUNCE_MS) {
                        continue;
                    }

                    lastModified.put(changed, now);

                    try {
                        config.reload();

                        String updatedChecksum = config.getCachedChecksum();

                        scheduler.runTask(javaPlugin, () ->
                                pluginManager.callEvent(
                                        new ConfigReloadedEvent(
                                                config.getFile().getName(),
                                                config.get(),
                                                oldChecksum,
                                                updatedChecksum
                                        )
                                )
                        );

                    } catch (Exception e) {
                        javaPlugin.getLogger().log(Level.SEVERE,
                                "Failed to reload config: " + config.getFile(), e);
                    }
                }
            }

            boolean valid = key.reset();

            if (!valid) {
                Path removed = watchKeys.remove(key);
                if (removed != null) {
                    directories.remove(removed);
                }
            }
        }
    }
}