package dev.spexx.configurationAPI.watcher;

import dev.spexx.configurationAPI.config.YamlConfig;
import dev.spexx.configurationAPI.difference.ConfigChangeSummary;
import dev.spexx.configurationAPI.difference.ConfigLineDifference;
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
 * analysis between file snapshots, producing structured {@link ConfigLineDifference}
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
 * {@link FileChecksum}. Diff computation is performed using a manual
 * line parsing algorithm with O(n) complexity.
 *
 * @implNote
 * A custom line-splitting implementation is used instead of
 * {@link String#split(String)} to avoid regex overhead. Diff lists are lazily
 * allocated and skipped entirely when no changes are detected.
 *
 * @since 1.0.0
 */
public final class YamlConfigWatcher {

    private final JavaPlugin plugin;
    private final Runnable reloadCallback;

    private volatile YamlConfig yamlConfig;
    private volatile String lastChecksum;
    private volatile String lastContent;

    private WatchService watchService;
    private Thread thread;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile long lastReload = 0;

    private static final long DEBOUNCE_MS = 300;

    /**
     * Constructs a new {@code YamlConfigWatcher}.
     *
     * @param plugin         the owning plugin instance
     * @param yamlConfig     the configuration to monitor
     * @param reloadCallback the reload callback
     *
     * @since 1.0.0
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
     * Starts monitoring the configuration file.
     *
     * @throws IOException if watcher cannot be initialized
     *
     * @since 1.0.0
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
     * Stops the watcher.
     *
     * @since 1.0.0
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
     * <p>This method is typically invoked after an atomic configuration swap
     * to ensure the watcher operates on the latest {@link YamlConfig} instance.</p>
     *
     * @param config the new configuration instance, must not be {@code null}
     *
     * @apiNote
     * This method does not trigger a reload. It only updates the internal reference
     * used for subsequent change detection.
     *
     * @implSpec
     * The reference is updated using a {@code volatile} write to guarantee
     * visibility across threads.
     *
     * @implNote
     * This method is expected to be called by the managing component immediately
     * after replacing the configuration instance.
     *
     * @since 1.0.0
     */
    public void updateConfig(@NotNull YamlConfig config) {
        this.yamlConfig = config;
    }

    /**
     * Updates the stored checksum.
     *
     * @since 1.0.0
     */
    public void updateChecksum() {
        this.lastChecksum = FileChecksum.sha256(yamlConfig.file());
    }

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
     * Performs checksum validation and triggers reload if needed.
     *
     * @since 1.0.1
     */
    private void reload() {

        File file = yamlConfig.file();

        if (!file.exists()) {
            plugin.getLogger().fine(() ->
                    "[ConfigWatcher] File missing, skipping reload: " + file.getName()
            );
            return;
        }

        String newChecksum = FileChecksum.sha256(file);
        if (newChecksum.equals(lastChecksum)) return;

        String oldChecksum = lastChecksum;
        String newContent = readContentSafe(file);

        if (newContent.equals(lastContent)) {
            lastChecksum = newChecksum;
            lastContent = newContent;
            return;
        }

        List<String> oldLines = splitLines(lastContent);
        List<String> newLines = splitLines(newContent);

        int max = Math.max(oldLines.size(), newLines.size());
        List<ConfigLineDifference> diffs = null;

        int changed = 0;
        int added = 0;
        int removed = 0;

        for (int i = 0; i < max; i++) {

            String oldLine = i < oldLines.size() ? oldLines.get(i) : "";
            String newLine = i < newLines.size() ? newLines.get(i) : "";

            if (!oldLine.equals(newLine)) {

                if (diffs == null) {
                    diffs = new ArrayList<>(max);
                }

                diffs.add(new ConfigLineDifference(i + 1, oldLine, newLine));

                if (i >= oldLines.size()) added++;
                else if (i >= newLines.size()) removed++;
                else changed++;
            }
        }

        lastChecksum = newChecksum;
        lastContent = newContent;

        final ConfigChangeSummary summary = new ConfigChangeSummary(changed, added, removed);
        final List<ConfigLineDifference> finalDiffs =
                diffs == null ? List.of() : List.copyOf(diffs);

        plugin.getServer().getScheduler().runTask(plugin, () -> {

            reloadCallback.run();

            plugin.getServer().getPluginManager().callEvent(
                    new ConfigReloadedEvent(
                            yamlConfig,
                            oldChecksum,
                            newChecksum,
                            summary,
                            finalDiffs
                    )
            );
        });
    }

    /**
     * Splits file content into lines without using regex.
     *
     * @since 1.0.3
     */
    private static @NotNull List<String> splitLines(@NotNull String content) {

        List<String> lines = new ArrayList<>();
        int start = 0;

        for (int i = 0; i < content.length(); i++) {
            if (content.charAt(i) == '\n') {
                lines.add(content.substring(start, i));
                start = i + 1;
            }
        }

        lines.add(content.substring(start));
        return lines;
    }

    /**
     * Reads file content safely.
     *
     * @since 1.0.1
     */
    private @NotNull String readContentSafe(File file) {
        try {
            return Files.readString(file.toPath());
        } catch (Exception e) {
            plugin.getLogger().warning(
                    "[ConfigWatcher] Failed to read config file: " + file.getAbsolutePath()
            );
            return "";
        }
    }
}