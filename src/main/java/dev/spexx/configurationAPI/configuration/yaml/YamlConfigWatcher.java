package dev.spexx.configurationAPI.configuration.yaml;

import dev.spexx.configurationAPI.exceptions.ConfigException;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.Set;
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
    private final @NotNull Set<Path> watchedFiles = ConcurrentHashMap.newKeySet();

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
     * @param file the file to watch
     *
     * @throws ConfigException if the file is already registered or invalid
     *
     * @since 1.3.0
     */
    public void watch(@NotNull File file) throws ConfigException {
        Path path = file.toPath().toAbsolutePath();

        if (watchedFiles.contains(path)) {
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
            watchedFiles.add(path);

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

            // watch for events
            for (WatchEvent<?> event : key.pollEvents()) {
                Path changed = dir.resolve((Path) event.context()).toAbsolutePath();

                if (!watchedFiles.contains(changed)) {
                    continue;
                }

                // stop tracking deleted file
                if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
                    watchedFiles.remove(changed);
                    continue;
                }

                // reload the configuration, once it's been externally modified
                if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                    System.out.println("[Watcher] File modified: " + changed);
                }
            }

            key.reset();
        }
    }
}