package dev.spexx.configurationAPI.watcher;

import dev.spexx.configurationAPI.config.YamlConfig;
import dev.spexx.configurationAPI.difference.ConfigLineDiff;
import dev.spexx.configurationAPI.events.ConfigReloadedEvent;
import dev.spexx.configurationAPI.util.FileChecksum;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Watches a single configuration file and triggers atomic reload operations
 * when file system changes are detected.
 *
 * <p>This class monitors the parent directory of the target configuration file
 * using {@link WatchService} and filters events specific to the file name.</p>
 *
 * <p>In addition to change detection, this watcher performs line-level diff
 * analysis between file snapshots, producing structured {@link ConfigLineDiff}
 * instances for downstream consumers.</p>
 *
 * <p>Reload operations are delegated to an external callback, typically managed
 * by {@link dev.spexx.configurationAPI.manager.ConfigManager}, ensuring that
 * configuration replacement is performed atomically.</p>
 *
 * @apiNote
 * This watcher runs on a dedicated daemon thread and schedules reload execution
 * on the main server thread. Consumers should avoid performing blocking work
 * inside event handlers triggered by this watcher.
 *
 * @implSpec
 * Change detection is guarded by SHA-256 checksums computed via
 * {@link FileChecksum}. Diff computation is performed using a simple
 * line-by-line comparison of file content snapshots.
 *
 * @implNote
 * The watcher stores the previous file content in memory to enable diff
 * computation. This approach prioritizes deterministic behavior and simplicity
 * over minimal memory usage. The tracked {@link YamlConfig} reference is
 * declared {@code volatile} to ensure visibility across threads during
 * atomic swap updates.
 *
 * @since 1.0.0
 */
public final class YamlConfigWatcher {

    /**
     * Owning plugin instance used for scheduling and event dispatching.
     */
    private final JavaPlugin plugin;

    /**
     * Callback invoked to perform a configuration reload.
     *
     * <p>This callback is expected to trigger an atomic replacement of the
     * configuration instance within the managing component.</p>
     */
    private final Runnable reloadCallback;

    /**
     * The currently tracked configuration instance.
     *
     * <p>Declared {@code volatile} to ensure visibility across threads.</p>
     */
    private volatile YamlConfig yamlConfig;

    /**
     * The last known checksum of the configuration file.
     */
    private volatile String lastChecksum;

    /**
     * The last known full content of the configuration file.
     */
    private volatile String lastContent;

    /**
     * The underlying watch service used for file system monitoring.
     */
    private WatchService watchService;

    /**
     * Dedicated thread responsible for processing watch events.
     */
    private Thread thread;

    /**
     * Indicates whether the watcher is currently running.
     */
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * Timestamp of the last processed reload event, used for debounce control.
     */
    private volatile long lastReload = 0;

    /**
     * Minimum interval in milliseconds between consecutive reload attempts.
     */
    private static final long DEBOUNCE_MS = 300;

    /**
     * Constructs a new {@code YamlConfigWatcher}.
     *
     * @param plugin         the owning plugin instance, must not be {@code null}
     * @param yamlConfig     the configuration to monitor, must not be {@code null}
     * @param reloadCallback the callback responsible for performing reload logic,
     *                       must not be {@code null}
     */
    public YamlConfigWatcher(@NotNull JavaPlugin plugin,
                             @NotNull YamlConfig yamlConfig,
                             @NotNull Runnable reloadCallback) {
        this.plugin = plugin;
        this.yamlConfig = yamlConfig;
        this.reloadCallback = reloadCallback;
        this.lastChecksum = FileChecksum.sha256(yamlConfig.file());
        this.lastContent = readContentSafe(yamlConfig.file());
    }

    /**
     * Starts monitoring the configuration file for changes.
     *
     * @throws IOException if the {@link WatchService} cannot be initialized
     * @throws IllegalStateException if the configuration file has no parent directory
     */
    public void start() throws IOException {

        if (running.get()) return;

        Path filePath = yamlConfig.file().toPath().toAbsolutePath().normalize();
        Path dir = filePath.getParent();

        if (dir == null) {
            throw new IllegalStateException("No parent directory: " + filePath);
        }

        watchService = FileSystems.getDefault().newWatchService();

        dir.register(
                watchService,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_DELETE
        );

        running.set(true);

        thread = new Thread(this::run, "Watcher-" + filePath.getFileName());
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Stops monitoring the configuration file and releases associated resources.
     *
     * <p>This method is safe to invoke multiple times.</p>
     */
    public void stop() {
        running.set(false);

        try {
            if (watchService != null) watchService.close();
        } catch (IOException ignored) {}

        if (thread != null) thread.interrupt();
    }

    /**
     * Updates the tracked configuration reference.
     *
     * <p>This is typically invoked after an atomic configuration swap.</p>
     *
     * @param config the new configuration instance, must not be {@code null}
     */
    public void updateConfig(@NotNull YamlConfig config) {
        this.yamlConfig = config;
    }

    /**
     * Recomputes and updates the stored checksum for the current configuration file.
     */
    public void updateChecksum() {
        this.lastChecksum = FileChecksum.sha256(yamlConfig.file());
    }

    /**
     * Main event processing loop executed by the watcher thread.
     *
     * <p>This method blocks on {@link WatchService#take()} and processes file system
     * events until the watcher is stopped.</p>
     */
    private void run() {

        while (running.get()) {
            try {
                WatchKey key = watchService.take();

                for (WatchEvent<?> event : key.pollEvents()) {

                    if (event.kind() == StandardWatchEventKinds.OVERFLOW) continue;

                    Path changed = (Path) event.context();

                    if (!changed.equals(yamlConfig.file().toPath().getFileName())) continue;

                    long now = System.currentTimeMillis();
                    if (now - lastReload < DEBOUNCE_MS) continue;

                    lastReload = now;

                    reload();
                }

                if (!key.reset()) break;

            } catch (Exception ignored) {
                break;
            }
        }
    }

    /**
     * Performs checksum validation, computes a diff snapshot, and triggers
     * a configuration reload if the file content has changed.
     *
     * <p>The reload operation is executed on the main server thread and followed
     * by dispatching a {@link ConfigReloadedEvent} containing diff metadata.</p>
     */
    private void reload() {

        File file = yamlConfig.file();

        String newChecksum = FileChecksum.sha256(file);
        if (newChecksum.equals(lastChecksum)) return;

        String oldChecksum = lastChecksum;

        String newContent = readContentSafe(file);

        List<ConfigLineDiff> diffs = new ArrayList<>();

        String[] oldLines = lastContent.split("\n", -1);
        String[] newLines = newContent.split("\n", -1);

        int max = Math.max(oldLines.length, newLines.length);

        int changed = 0;
        int added = 0;
        int removed = 0;

        for (int i = 0; i < max; i++) {

            String oldLine = i < oldLines.length ? oldLines[i] : "";
            String newLine = i < newLines.length ? newLines[i] : "";

            if (!oldLine.equals(newLine)) {

                diffs.add(new ConfigLineDiff(i + 1, oldLine, newLine));

                if (i >= oldLines.length) {
                    added++;
                } else if (i >= newLines.length) {
                    removed++;
                } else {
                    changed++;
                }
            }
        }

        lastChecksum = newChecksum;
        lastContent = newContent;

        final int finalChanged = changed;
        final int finalAdded = added;
        final int finalRemoved = removed;
        final List<ConfigLineDiff> finalDiffs = List.copyOf(diffs);

        plugin.getServer().getScheduler().runTask(plugin, () -> {

            reloadCallback.run();

            YamlConfig updated = this.yamlConfig;

            plugin.getServer().getPluginManager().callEvent(
                    new ConfigReloadedEvent(
                            updated,
                            oldChecksum,
                            newChecksum,
                            finalChanged,
                            finalAdded,
                            finalRemoved,
                            finalDiffs
                    )
            );
        });
    }

    /**
     * Reads the full content of a file as a string.
     *
     * <p>If the file cannot be read, an empty string is returned.</p>
     *
     * @param file the file to read
     * @return file content, or empty string if reading fails
     */
    private @NotNull String readContentSafe(File file) {
        try {
            return Files.readString(file.toPath());
        } catch (Exception e) {
            return "";
        }
    }
}