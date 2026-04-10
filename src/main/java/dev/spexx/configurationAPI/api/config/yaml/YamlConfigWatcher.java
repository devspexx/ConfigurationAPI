package dev.spexx.configurationAPI.api.config.yaml;

import dev.spexx.configurationAPI.api.event.ConfigReloadEvent;
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

/**
 * Watches {@link YamlConfig} files for changes.
 *
 * <p>Registered configurations are monitored for file system modifications.
 * Files located in the same directory share a single {@link WatchKey}.</p>
 *
 * <p>Modification events are debounced to prevent duplicate reloads.</p>
 *
 * @since 1.3.0
 */
public class YamlConfigWatcher {

    private final @NotNull JavaPlugin javaPlugin;

    private final @NotNull WatchService watchService;

    /**
     * Maps directories to their {@link WatchKey}.
     *
     * <p>Each directory is registered only once, even if multiple files within it are watched.</p>
     *
     * @since 1.3.0
     */
    private final @NotNull Map<Path, WatchKey> directories = new ConcurrentHashMap<>();

    /**
     * Maps absolute file paths to their corresponding {@link YamlConfig}.
     *
     * @since 1.3.0
     */
    private final Map<Path, YamlConfig> watchedFiles = new ConcurrentHashMap<>();

    /**
     * Tracks last modification timestamps used for debounce logic.
     *
     * @since 1.3.0
     */
    private final Map<Path, Long> lastModified = new ConcurrentHashMap<>();

    /**
     * Maps {@link WatchKey} instances to their corresponding directory {@link Path}.
     *
     * <p>This reverse mapping allows constant-time (O(1)) resolution of a directory
     * from a {@link WatchKey}, avoiding linear scans over registered directories.</p>
     *
     * <p>The map is populated when directories are registered and cleaned up when
     * {@link WatchKey}s become invalid.</p>
     *
     * @since 1.3.0
     */
    private final @NotNull Map<WatchKey, Path> watchKeys = new ConcurrentHashMap<>();

    private volatile boolean running = false;

    /**
     * Creates a new watcher instance.
     *
     * @param javaPlugin the plugin instance used for scheduling synchronous tasks
     * @throws ConfigException if the watcher cannot be initialized
     * @since 1.3.0
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
     * Registers a configuration for watching.
     *
     * <p>If multiple configurations are located in the same directory,
     * the directory is registered only once.</p>
     *
     * @param config the configuration to watch
     * @throws ConfigException if already registered or invalid
     * @since 1.3.0
     */
    public void watch(@NotNull YamlConfig config) throws ConfigException {
        Path path = config.getFile().toPath().toAbsolutePath();

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
                            StandardWatchEventKinds.ENTRY_DELETE
                    );

                    // for reverse mapping
                    watchKeys.put(key, dir);

                    return key;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            watchedFiles.put(path, config);

        } catch (RuntimeException e) {
            throw new ConfigException("Failed to register file watcher: " + path, e);
        }
    }

    /**
     * Starts the watcher thread.
     *
     * @throws ConfigException if already running
     * @since 1.3.0
     */
    public void start() throws ConfigException {
        if (running) {
            throw new ConfigException("Watcher is already running");
        }

        running = true;

        Thread thread = new Thread(this::run, "YamlConfigWatcher");
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Stops the watcher thread.
     *
     * @since 1.3.0
     */
    public void stop() {
        running = false;

        try {
            watchService.close();
        } catch (IOException ignored) {
        }
    }

    /**
     * Internal watcher loop.
     *
     * <p>Resolves {@link WatchKey} instances back to their corresponding directory
     * and processes events for registered files only.</p>
     *
     * @since 1.3.0
     */
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
                Path changed = dir.resolve((Path) event.context()).toAbsolutePath();

                // null safety
                YamlConfig config = watchedFiles.get(changed);
                if (config == null) {
                    continue;
                }

                if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
                    watchedFiles.remove(changed);
                    continue;
                }

                if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {

                    @Nullable String oldChecksum = config.getCachedChecksum();
                    @Nullable String newChecksum;

                    try {
                        newChecksum = FileChecksum.computeSha256(config.getFile());
                    } catch (Exception e) {
                        e.printStackTrace();
                        continue;
                    }

                    // skip if nothing actually changed
                    if (oldChecksum != null && oldChecksum.equals(newChecksum)) {
                        continue;
                    }

                    // debounce
                    long now = System.currentTimeMillis();
                    long last = lastModified.getOrDefault(changed, 0L);
                    if (now - last < 200) {
                        continue;
                    }

                    lastModified.put(changed, now);

                    try {
                        config.reload();

                        String updatedChecksum = config.getCachedChecksum();

                        scheduler.runTask(javaPlugin, () ->
                                pluginManager.callEvent(
                                        new ConfigReloadEvent(
                                                config.getFile().getName(),
                                                config.get(),
                                                oldChecksum,
                                                updatedChecksum
                                        )
                                )
                        );

                    } catch (Exception e) {
                        e.printStackTrace();
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