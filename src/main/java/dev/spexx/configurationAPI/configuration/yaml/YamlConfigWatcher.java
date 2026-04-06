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
 * <p>Files can be registered for watching. Each file can only be registered once.</p>
 *
 * <p>This watcher runs on a dedicated thread and listens for file system events.</p>
 *
 * @since 1.3.0
 */
public class YamlConfigWatcher {

    private final @NotNull WatchService watchService;

    private final @NotNull Map<WatchKey, Path> directories = new ConcurrentHashMap<>();
    private final Map<Path, YamlConfig> watchedFiles = new ConcurrentHashMap<>();

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
     * Registers a file for watching.
     *
     * <p>The file cannot be registered more than once.</p>
     *
     * @param config the configuration to watch
     *
     * @throws ConfigException if the file is already registered or invalid
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
            WatchKey key = directory.register(
                    watchService,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE
            );

            directories.put(key, directory);
            watchedFiles.put(path, config);

        } catch (IOException e) {
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

            Path dir = directories.get(key);
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

                // stop tracking deleted file
                if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
                    watchedFiles.remove(changed);
                    continue;
                }

                // reload on modify
                if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {

                    long now = System.currentTimeMillis();
                    long last = lastModified.getOrDefault(changed, 0L);

                    // debounce to prevent double / triple firing
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