package dev.spexx.configurationAPI.configuration.yaml;

import dev.spexx.configurationAPI.exceptions.ConfigException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Watches YAML configuration files for changes.
 *
 * <p>Each registered {@link YamlConfig} is monitored for file system changes.
 * Multiple files in the same directory share a single {@link WatchKey}.</p>
 *
 * <p>File modification events are debounced to prevent duplicate reloads.</p>
 *
 * @since 1.3.0
 */
public class YamlConfigWatcher {

    private final @NotNull WatchService watchService;

    /**
     * Maps watched directories to their {@link WatchKey}.
     *
     * <p>A directory is registered only once even if multiple files within it are watched.</p>
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
     * Tracks last modification timestamps for debounce logic.
     *
     * @since 1.3.0
     */
    private final Map<Path, Long> lastModified = new ConcurrentHashMap<>();

    private volatile boolean running = false;

    /**
     * Creates a new watcher instance.
     *
     * @throws ConfigException if the watcher cannot be initialized
     *
     * @since 1.3.0
     */
    public YamlConfigWatcher() throws ConfigException {
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
     *
     * @throws ConfigException if already registered or invalid
     *
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
                    return dir.register(
                            watchService,
                            StandardWatchEventKinds.ENTRY_MODIFY,
                            StandardWatchEventKinds.ENTRY_DELETE
                    );
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
     *
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
     * <p>Resolves directory keys back to their {@link Path} and processes events
     * for registered files only.</p>
     *
     * @since 1.3.0
     */
    private void run() {
        while (running) {
            WatchKey key;

            try {
                key = watchService.take();
            } catch (InterruptedException | ClosedWatchServiceException e) {
                return;
            }

            Path dir = null;

            for (Map.Entry<Path, WatchKey> entry : directories.entrySet()) {
                if (entry.getValue().equals(key)) {
                    dir = entry.getKey();
                    break;
                }
            }

            if (dir == null) {
                key.reset();
                continue;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                Path changed = dir.resolve((Path) event.context()).toAbsolutePath();

                YamlConfig config = watchedFiles.get(changed);
                if (config == null) {
                    continue;
                }

                if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
                    watchedFiles.remove(changed);
                    continue;
                }

                if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {

                    long now = System.currentTimeMillis();
                    long last = lastModified.getOrDefault(changed, 0L);

                    if (now - last < 200) {
                        continue;
                    }

                    lastModified.put(changed, now);

                    System.out.println("[Watcher] File modified: " + changed);

                    try {
                        config.reload();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            key.reset();
        }
    }
}