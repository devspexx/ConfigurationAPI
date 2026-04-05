package dev.spexx.configurationAPI.watcher;

import dev.spexx.configurationAPI.config.YamlConfig;
import dev.spexx.configurationAPI.events.ConfigReloadedEvent;
import dev.spexx.configurationAPI.util.FileChecksum;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.nio.file.StandardWatchEventKinds.*;

/**
 * Global watcher responsible for monitoring multiple configuration files
 * using a single {@link WatchService} instance.
 *
 * <p>This implementation tracks configuration files by their normalized
 * {@link Path} and performs atomic replacement of {@link YamlConfig} instances
 * when changes are detected.</p>
 *
 * <h2>Behavior</h2>
 * <ul>
 *     <li>{@code ENTRY_CREATE} → loads new configuration if tracked</li>
 *     <li>{@code ENTRY_MODIFY} → reloads configuration if content changed</li>
 *     <li>{@code ENTRY_DELETE} → removes configuration from tracking</li>
 * </ul>
 *
 * <h2>Threading</h2>
 * <p>File system events are processed on a dedicated daemon thread.
 * Bukkit events are dispatched synchronously on the main server thread.</p>
 *
 * @apiNote
 * This watcher replaces {@link YamlConfig} instances rather than mutating them,
 * ensuring thread-safe access for all consumers.
 *
 * @implSpec
 * Uses SHA-256 checksum comparison to detect content changes and a debounce
 * window to suppress duplicate file system events.
 *
 * @since 1.0.5
 */
public final class GlobalConfigWatcher {

    private static final long DEBOUNCE_MS = 300;

    private final @NotNull JavaPlugin plugin;
    private final @NotNull WatchService watchService;

    private final Map<Path, YamlConfig> configs = new ConcurrentHashMap<>();
    private final Map<Path, String> checksums = new ConcurrentHashMap<>();
    private final Map<Path, Long> lastReload = new ConcurrentHashMap<>();
    private final Set<Path> watchedDirs = ConcurrentHashMap.newKeySet();

    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread thread;

    /**
     * Creates a new watcher instance.
     *
     * @param plugin owning plugin instance
     * @throws IOException if the watch service cannot be initialized
     */
    public GlobalConfigWatcher(@NotNull JavaPlugin plugin) throws IOException {
        this.plugin = plugin;
        this.watchService = FileSystems.getDefault().newWatchService();
    }

    /**
     * Registers a configuration for monitoring.
     *
     * <p>The parent directory is registered with the {@link WatchService}
     * if not already tracked.</p>
     *
     * @param config configuration snapshot
     * @throws IOException if directory registration fails
     */
    public void register(@NotNull YamlConfig config) throws IOException {

        Path path = normalize(config.file().toPath());
        Path dir = path.getParent();

        configs.put(path, config);
        checksums.put(path, FileChecksum.sha256(config.file()));

        if (watchedDirs.add(dir)) {
            dir.register(watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
        }
    }

    /**
     * Starts the watcher thread.
     */
    public void start() {
        if (running.get()) return;

        running.set(true);

        thread = new Thread(this::run, "GlobalConfigWatcher");
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Stops the watcher and releases system resources.
     */
    public void stop() {
        running.set(false);

        try {
            watchService.close();
        } catch (IOException ignored) {}

        if (thread != null) {
            thread.interrupt();
        }
    }

    /**
     * Main watcher loop.
     */
    private void run() {

        while (running.get()) {
            try {
                WatchKey key = watchService.take();
                Path dir = (Path) key.watchable();

                for (WatchEvent<?> event : key.pollEvents()) {

                    if (event.kind() == OVERFLOW) {
                        continue;
                    }

                    Path path = normalize(dir.resolve((Path) event.context()));

                    long now = System.currentTimeMillis();
                    long last = lastReload.getOrDefault(path, 0L);

                    if (now - last < DEBOUNCE_MS) {
                        continue;
                    }

                    lastReload.put(path, now);

                    handle(path, event.kind());
                }

                if (!key.reset()) {
                    plugin.getLogger().warning("[ConfigWatcher] Watch key no longer valid.");
                }

            } catch (ClosedWatchServiceException e) {
                return;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                plugin.getLogger().warning("[ConfigWatcher] Unexpected error: " + e.getMessage());
            }
        }
    }

    /**
     * Handles a file system event for a specific path.
     *
     * @param path affected file path
     * @param kind event type
     */
    private void handle(@NotNull Path path, WatchEvent.@NotNull Kind<?> kind) {

        File file = path.toFile();

        // DELETE
        if (kind == ENTRY_DELETE) {
            if (configs.remove(path) != null) {
                checksums.remove(path);
                plugin.getLogger().info("[ConfigWatcher] Removed: " + path.getFileName());
            }
            return;
        }

        // CREATE / MODIFY
        if (!file.exists()) {
            return;
        }

        try {
            String newChecksum = FileChecksum.sha256(file);
            @Nullable String oldChecksum = checksums.get(path);

            if (oldChecksum != null && oldChecksum.equals(newChecksum)) {
                return;
            }

            long start = System.nanoTime();

            @Nullable YamlConfig oldConfig = configs.get(path);

            YamlConfig newConfig = new YamlConfig(
                    file,
                    YamlConfiguration.loadConfiguration(file)
            );

            configs.put(path, newConfig);
            checksums.put(path, newChecksum);

            int timeMs = (int) ((System.nanoTime() - start) / 1_000_000);

            if (oldConfig != null && oldChecksum != null) {
                plugin.getServer().getScheduler().runTask(plugin, () ->
                        plugin.getServer().getPluginManager().callEvent(
                                new ConfigReloadedEvent(
                                        oldConfig,
                                        newConfig,
                                        oldChecksum,
                                        newChecksum,
                                        timeMs
                                )
                        )
                );
            }

            plugin.getLogger().info("[ConfigWatcher] Reloaded: " + file.getName());

        } catch (Exception e) {
            plugin.getLogger().warning(
                    "[ConfigWatcher] Failed to load: " + file.getName()
            );
        }
    }

    /**
     * Returns the current configuration snapshot for the given path.
     *
     * @param path configuration path
     * @return configuration snapshot or {@code null} if not tracked
     */
    public @Nullable YamlConfig get(@NotNull Path path) {
        return configs.get(normalize(path));
    }

    /**
     * Normalizes a path to ensure consistent identity.
     *
     * @param path raw path
     * @return normalized absolute path
     */
    private static @NotNull Path normalize(@NotNull Path path) {
        return path.toAbsolutePath().normalize();
    }
}