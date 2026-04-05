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
import java.util.logging.Level;

import static java.nio.file.StandardWatchEventKinds.*;

/**
 * Global watcher responsible for monitoring multiple configuration files.
 *
 * <p>This class maintains a thread-safe registry of configuration snapshots and
 * automatically reloads them when changes are detected on disk.</p>
 *
 * <p>Reload operations are validated before being applied. If a configuration
 * fails to load (for example due to invalid YAML), the previous snapshot is
 * preserved and no reload event is fired.</p>
 *
 * @since 1.1.0
 */
public final class GlobalConfigWatcher {

    /**
     * Minimum delay between reload attempts for the same file.
     *
     * <p>This prevents excessive reloads caused by rapid file system events
     * (for example, editors writing files in multiple steps).</p>
     */
    private static final long DEBOUNCE_MS = 300;

    /**
     * Owning plugin instance used for scheduling and logging.
     */
    private final @NotNull JavaPlugin plugin;

    /**
     * Underlying {@link WatchService} used to monitor file system changes.
     */
    private final @NotNull WatchService watchService;

    /**
     * Active configuration snapshots indexed by normalized file path.
     *
     * <p>Each entry represents the latest known valid configuration state.</p>
     */
    private final Map<Path, YamlConfig> configs = new ConcurrentHashMap<>();

    /**
     * Cached checksums used to detect content changes.
     */
    private final Map<Path, String> checksums = new ConcurrentHashMap<>();

    /**
     * Tracks the last reload time for each file to apply debounce protection.
     */
    private final Map<Path, Long> lastReload = new ConcurrentHashMap<>();

    /**
     * Set of directories currently registered with the watch service.
     *
     * <p>Directories are registered once to avoid redundant registrations.</p>
     */
    private final Set<Path> watchedDirs = ConcurrentHashMap.newKeySet();

    /**
     * Indicates whether the watcher thread is currently running.
     */
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * Background thread responsible for processing file system events.
     */
    private Thread thread;

    /**
     * Creates a new watcher instance.
     *
     * @param plugin owning plugin instance, must not be {@code null}
     * @throws IOException if the watch service cannot be initialized
     *
     * @since 1.1.0
     */
    public GlobalConfigWatcher(@NotNull JavaPlugin plugin) throws IOException {
        this.plugin = plugin;
        this.watchService = FileSystems.getDefault().newWatchService();
    }

    /**
     * Registers a configuration file for monitoring.
     *
     * <p>This method performs the following actions:</p>
     * <ul>
     *     <li>Normalizes the file path for consistent identity</li>
     *     <li>Adds the configuration snapshot to the registry</li>
     *     <li>Computes and stores its checksum</li>
     *     <li>Registers the parent directory with the watch service if needed</li>
     * </ul>
     *
     * <p>If the configuration is already registered, the method returns
     * without performing any additional work.</p>
     *
     * @param config configuration snapshot to register, must not be {@code null}
     * @throws IOException if directory registration fails
     *
     * @since 1.1.0
     */
    public void register(@NotNull YamlConfig config) throws IOException {

        // Normalize path to ensure consistent lookup
        Path path = normalize(config.file().toPath());
        Path dir = path.getParent();

        // Prevent duplicate registration
        if (configs.putIfAbsent(path, config) != null) {
            return;
        }

        // Store checksum for change detection
        checksums.put(path, FileChecksum.sha256(config.file()));

        // Register directory only once
        if (watchedDirs.add(dir)) {
            dir.register(watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
        }
    }

    /**
     * Unregisters a configuration file from monitoring.
     *
     * <p>This removes all associated state, including:</p>
     * <ul>
     *     <li>Configuration snapshot</li>
     *     <li>Checksum cache</li>
     *     <li>Debounce tracking</li>
     * </ul>
     *
     * @param path configuration file path, must not be {@code null}
     *
     * @since 1.1.0
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
     * <p>This method is idempotent. Calling it multiple times will not create
     * additional threads.</p>
     *
     * @since 1.1.0
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
     * <p>This method:</p>
     * <ul>
     *     <li>Stops the watcher loop</li>
     *     <li>Closes the {@link WatchService}</li>
     *     <li>Interrupts the watcher thread</li>
     * </ul>
     *
     * @since 1.1.0
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
     * Handles a file system event affecting a tracked configuration file.
     *
     * <p>This method performs the following steps:</p>
     * <ul>
     *     <li>Validates that the file is still tracked</li>
     *     <li>Applies debounce logic to prevent excessive reloads</li>
     *     <li>Computes a checksum to detect actual content changes</li>
     *     <li>Loads and validates the YAML configuration</li>
     *     <li>Applies rollback protection if the configuration is invalid</li>
     *     <li>Replaces the previous snapshot atomically</li>
     *     <li>Dispatches a {@link ConfigReloadedEvent} on successful reload</li>
     * </ul>
     *
     * <h3>Validation</h3>
     * <p>If the YAML file is syntactically invalid or results in an empty
     * configuration while the file is non-empty, the reload is aborted and the
     * previous configuration snapshot is preserved.</p>
     *
     * <h3>Event Dispatch</h3>
     * <p>The reload event is only fired if both the previous configuration and
     * checksum are available. This ensures that events always represent a valid
     * transition from one known state to another.</p>
     *
     * @param path the affected file path, must not be {@code null}
     * @param kind the type of file system event, must not be {@code null}
     *
     * @since 1.1.0
     */
    private void handleFileEvent(@NotNull Path path, WatchEvent.@NotNull Kind<?> kind) {

        // Ignore files that are not currently tracked
        if (!configs.containsKey(path)) {
            return;
        }

        File file = path.toFile();

        // Handle file deletion
        if (kind == ENTRY_DELETE) {
            configs.remove(path);
            checksums.remove(path);
            lastReload.remove(path);
            return;
        }

        // Ignore events for files that no longer exist
        if (!file.exists()) {
            return;
        }

        try {
            // Compute new checksum for change detection
            String newChecksum = FileChecksum.sha256(file);

            // Retrieve previous checksum (may be null if not initialized properly)
            String oldChecksum = checksums.get(path);

            // Skip reload if content has not changed
            if (oldChecksum != null && oldChecksum.equals(newChecksum)) {
                return;
            }

            // Load YAML configuration from disk
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

            // Validate YAML: prevent silent failures from replacing valid config
            if (yaml.getKeys(false).isEmpty() && file.length() > 0) {
                plugin.getLogger().warning(
                        "[ConfigWatcher] Invalid YAML detected, keeping previous configuration: " + file.getName()
                );
                return;
            }

            long start = System.nanoTime();

            // Retrieve previous snapshot (may be null on first load)
            YamlConfig oldConfig = configs.get(path);

            // Create new immutable snapshot
            YamlConfig newConfig = new YamlConfig(file, yaml);

            // Atomically replace configuration
            configs.put(path, newConfig);
            checksums.put(path, newChecksum);

            int timeMs = (int) ((System.nanoTime() - start) / 1_000_000);

            /*
             * Only fire event if we have a valid previous state.
             * This avoids passing null values into the event and ensures
             * a meaningful state transition.
             */
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

        } catch (Exception e) {
            // Log full stack trace for debugging
            plugin.getLogger().log(
                    Level.WARNING,
                    "[ConfigWatcher] Failed to reload configuration: " + file.getName(),
                    e
            );
        }
    }

    /**
     * Main watcher loop responsible for processing file system events.
     *
     * <p>This method continuously listens for file system events and dispatches
     * them to {@link #handleFileEvent(Path, WatchEvent.Kind)}.</p>
     *
     * <p>It applies debounce logic to prevent excessive reloads and ensures that
     * unexpected errors do not terminate the watcher thread.</p>
     *
     * @since 1.1.0
     */
    private void run() {
        while (running.get()) {
            try {
                // Wait for next file system event
                WatchKey key = watchService.take();
                Path dir = (Path) key.watchable();

                for (WatchEvent<?> event : key.pollEvents()) {

                    if (event.kind() == OVERFLOW) {
                        continue;
                    }

                    // Resolve full path of affected file
                    Path path = normalize(dir.resolve((Path) event.context()));

                    long now = System.currentTimeMillis();
                    long last = lastReload.getOrDefault(path, 0L);

                    // Apply debounce protection
                    if (now - last < DEBOUNCE_MS) {
                        continue;
                    }

                    lastReload.put(path, now);

                    // Delegate to handler
                    handleFileEvent(path, event.kind());
                }

                key.reset();

            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "[ConfigWatcher] Error", e);
            }
        }
    }

    /**
     * Returns the current configuration snapshot for the given path.
     *
     * <p>The returned value represents the latest known valid configuration
     * state. If the configuration is not tracked, {@code null} is returned.</p>
     *
     * @param path configuration path, must not be {@code null}
     * @return configuration snapshot or {@code null} if not tracked
     *
     * @since 1.1.0
     */
    public @Nullable YamlConfig get(@NotNull Path path) {
        return configs.get(normalize(path));
    }

    /**
     * Returns all currently tracked configuration snapshots.
     *
     * <p>The returned collection is backed by the internal data structure and
     * reflects the current state at the time of invocation.</p>
     *
     * @return collection of configuration snapshots, never {@code null}
     *
     * @since 1.1.0
     */
    public @NotNull Collection<YamlConfig> getAll() {
        return configs.values();
    }

    /**
     * Normalizes a file path to ensure consistent identity.
     *
     * <p>This method converts the path to an absolute path and removes any
     * redundant elements such as {@code "."} or {@code ".."}.</p>
     *
     * @param path raw path, must not be {@code null}
     * @return normalized absolute path
     *
     * @since 1.1.0
     */
    private static @NotNull Path normalize(@NotNull Path path) {
        return path.toAbsolutePath().normalize();
    }
}