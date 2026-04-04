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
 * <p>This watcher monitors the parent directory of a target configuration file
 * using {@link WatchService} and filters events specific to the file name.</p>
 *
 * <p>It performs checksum validation and line-level diff analysis to detect changes.
 * Reload operations are delegated to an external callback and dispatched on the
 * main server thread.</p>
 *
 * <h2>Features</h2>
 * <ul>
 *     <li>Checksum-based change detection (SHA-256)</li>
 *     <li>Debounced file system event handling</li>
 *     <li>Retry-based safe file reading (prevents partial reads)</li>
 *     <li>Lazy diff computation with O(n) complexity</li>
 *     <li>Diff size limiting to prevent memory pressure</li>
 *     <li>Thread-safe configuration reference updates</li>
 * </ul>
 *
 * @apiNote
 * This watcher runs on a dedicated daemon thread. Reload callbacks and event
 * dispatching are always executed on the main server thread.
 *
 * @implSpec
 * File changes are detected via {@link WatchService} and validated using
 * SHA-256 checksums. Diff computation uses a manual line-splitting algorithm
 * to avoid regex overhead.
 *
 * @implNote
 * Some editors perform atomic file replacement (delete + recreate). This class
 * includes safeguards against transient file states and partial reads.
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
    private volatile long lastEventTime = 0;

    private static final long DEBOUNCE_MS = 300;
    private static final long MIN_EVENT_INTERVAL_MS = 100;
    private static final int MAX_DIFFS = 10_000;

    /**
     * Constructs a new {@code YamlConfigWatcher}.
     *
     * @param plugin         the owning plugin instance, must not be {@code null}
     * @param yamlConfig     the configuration to monitor, must not be {@code null}
     * @param reloadCallback callback responsible for performing reload logic
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
     * @throws IOException if the watcher cannot be initialized
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

        thread = new Thread(this::run, "ConfigWatcher-" + filePath.getFileName());
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Stops monitoring and releases resources.
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
     * @param config new configuration instance
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

    /**
     * Main watcher loop.
     */
    private void run() {

        final Path targetFileName = yamlConfig.file().toPath().getFileName();

        while (running.get()) {
            try {
                WatchKey key = watchService.take();

                for (WatchEvent<?> event : key.pollEvents()) {

                    if (event.kind() == StandardWatchEventKinds.OVERFLOW) continue;

                    Path changed = (Path) event.context();

                    if (!changed.equals(targetFileName)) continue;

                    long now = System.currentTimeMillis();

                    if (now - lastEventTime < MIN_EVENT_INTERVAL_MS) continue;
                    lastEventTime = now;

                    if (now - lastReload < DEBOUNCE_MS) continue;
                    lastReload = now;

                    reload();
                }

                if (!key.reset()) {
                    plugin.getLogger().warning("[ConfigWatcher] Watch key invalidated: "
                            + targetFileName);
                    break;
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;

            } catch (ClosedWatchServiceException e) {
                break;

            } catch (Exception e) {
                plugin.getLogger().severe("[ConfigWatcher] Watch loop crashed: " + e.getMessage());
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

                if (diffs == null) diffs = new ArrayList<>(max);

                if (diffs.size() >= MAX_DIFFS) {
                    plugin.getLogger().warning("[ConfigWatcher] Diff limit reached, truncating.");
                    break;
                }

                diffs.add(new ConfigLineDifference(i + 1, oldLine, newLine));

                if (i >= oldLines.size()) added++;
                else if (i >= newLines.size()) removed++;
                else changed++;
            }
        }

        lastChecksum = newChecksum;
        lastContent = newContent;

        if (!plugin.isEnabled()) return;

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
     * Splits content into lines without regex.
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
     * Reads file content safely with retry mechanism.
     *
     * @since 1.0.4
     */
    private @NotNull String readContentSafe(@NotNull File file) {

        Path path = file.toPath();

        for (int i = 0; i < 3; i++) {
            try {
                return Files.readString(path);
            } catch (IOException e) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        plugin.getLogger().warning(
                "[ConfigWatcher] Failed to read config file after retries: "
                        + file.getAbsolutePath()
        );

        return "";
    }
}