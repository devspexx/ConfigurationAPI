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
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.nio.file.StandardWatchEventKinds.*;

/**
 * Global watcher responsible for monitoring multiple configuration files
 * using a single {@link WatchService} instance.
 *
 * <p>This class acts as the single source of truth for all tracked
 * {@link YamlConfig} instances. It maintains the latest configuration
 * snapshots and updates them automatically when file system changes
 * are detected.</p>
 *
 * <h2>Behavior</h2>
 * <ul>
 *     <li>{@link StandardWatchEventKinds#ENTRY_CREATE} &mdash; loads or reloads configuration</li>
 *     <li>{@link StandardWatchEventKinds#ENTRY_MODIFY} &mdash; reloads configuration if content changed</li>
 *     <li>{@link StandardWatchEventKinds#ENTRY_DELETE} &mdash; removes configuration from tracking</li>
 * </ul>
 *
 * <h2>Threading</h2>
 * <p>File system events are processed on a dedicated daemon thread.
 * Bukkit events are dispatched synchronously on the main server thread.</p>
 *
 * <h2>Consistency</h2>
 * <p>Configuration instances are replaced atomically. Consumers will always
 * observe either the previous or the updated snapshot, never a partially
 * updated state.</p>
 *
 * <h2>Change Detection</h2>
 * <p>Changes are detected using SHA-256 checksums. Identical file contents
 * will not trigger reloads even if file system events occur.</p>
 *
 * @since 1.0.5
 */
public final class GlobalConfigWatcher {

    /**
     * Minimum time between reload attempts for the same file, in milliseconds.
     */
    private static final long DEBOUNCE_MS = 300;

    private final @NotNull JavaPlugin plugin;
    private final @NotNull WatchService watchService;

    /**
     * Current configuration snapshots indexed by normalized path.
     */
    private final Map<Path, YamlConfig> configs = new ConcurrentHashMap<>();

    /**
     * Last known checksums for tracked files.
     */
    private final Map<Path, String> checksums = new ConcurrentHashMap<>();

    /**
     * Last reload timestamps used for debounce protection.
     */
    private final Map<Path, Long> lastReload = new ConcurrentHashMap<>();

    /**
     * Set of directories currently registered with the watch service.
     */
    private final Set<Path> watchedDirs = ConcurrentHashMap.newKeySet();

    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread thread;

    /**
     * Creates a new watcher instance.
     *
     * @param plugin owning plugin instance, must not be {@code null}
     * @throws IOException if the watch service cannot be initialized
     */
    public GlobalConfigWatcher(@NotNull JavaPlugin plugin) throws IOException {
        this.plugin = plugin;
        this.watchService = FileSystems.getDefault().newWatchService();
    }

    /**
     * Registers a configuration file for monitoring.
     *
     * <p>The parent directory of the file is registered with the
     * {@link WatchService} if not already tracked.</p>
     *
     * @param config configuration snapshot to register, must not be {@code null}
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
     * Unregisters a configuration file from monitoring.
     *
     * <p>This removes all associated state for the specified path.</p>
     *
     * @param path configuration path, must not be {@code null}
     */
    public void unregister(@NotNull Path path) {
        Path normalized = normalize(path);

        configs.remove(normalized);
        checksums.remove(normalized);
        lastReload.remove(normalized);
    }

    /**
     * Starts the watcher thread.
     *
     * <p>This method is idempotent. Calling it multiple times has no effect.</p>
     */
    public void start() {
        if (running.get()) {
            return;
        }

        running.set(true);

        thread = new Thread(this::run, "GlobalConfigWatcher");
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Stops the watcher and releases system resources.
     *
     * <p>The watcher thread is interrupted and the underlying
     * {@link WatchService} is closed.</p>
     */
    public void stop() {
        running.set(false);

        try {
            watchService.close();
        } catch (IOException ignored) {
        }

        if (thread != null) {
            thread.interrupt();
        }
    }

    /**
     * Main watcher loop that processes file system events.
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
                    handleFileEvent(path, event.kind());
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
     * Handles a file system event for a tracked configuration file.
     *
     * @param path affected file path, must not be {@code null}
     * @param kind event type, must not be {@code null}
     */
    private void handleFileEvent(@NotNull Path path, WatchEvent.@NotNull Kind<?> kind) {

        File file = path.toFile();

        if (!configs.containsKey(path)) {
            return;
        }

        // Handle deletion
        if (kind == ENTRY_DELETE) {
            if (configs.remove(path) != null) {
                checksums.remove(path);
                lastReload.remove(path);
                plugin.getLogger().info("[ConfigWatcher] Removed: " + path.getFileName());
            }
            return;
        }

        // Ignore if file does not exist
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

                var scheduler = plugin.getServer().getScheduler();
                var pluginManager = plugin.getServer().getPluginManager();

                scheduler.runTask(plugin, () ->
                        pluginManager.callEvent(new ConfigReloadedEvent(
                                oldConfig, newConfig, oldChecksum, newChecksum, timeMs
                )));
            }

            plugin.getLogger().info("[ConfigWatcher] Reloaded: " + file.getName());

        } catch (Exception e) {
            plugin.getLogger().warning(
                    "[ConfigWatcher] Failed to load: " + file.getName()
            );
        }
    }

    /**
     * Returns the current configuration snapshot for the specified path.
     *
     * @param path configuration path, must not be {@code null}
     * @return configuration snapshot, or {@code null} if not tracked
     */
    public @Nullable YamlConfig get(@NotNull Path path) {
        return configs.get(normalize(path));
    }

    /**
     * Returns all currently tracked configuration snapshots.
     *
     * @return collection of configurations, never {@code null}
     */
    public @NotNull Collection<YamlConfig> getAll() {
        return configs.values();
    }

    /**
     * Normalizes a path to ensure consistent identity.
     *
     * @param path raw path, must not be {@code null}
     * @return normalized absolute path
     */
    private static @NotNull Path normalize(@NotNull Path path) {
        return path.toAbsolutePath().normalize();
    }
}