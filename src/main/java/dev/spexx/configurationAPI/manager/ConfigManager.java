package dev.spexx.configurationAPI.manager;

import dev.spexx.configurationAPI.config.YamlConfig;
import dev.spexx.configurationAPI.watcher.GlobalConfigWatcher;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;

/**
 * Central access point for configuration files managed by the system.
 *
 * <p>This class is responsible for:
 * <ul>
 *     <li>Initial loading of configuration files</li>
 *     <li>Registering configurations with the global watcher</li>
 *     <li>Providing access to the latest configuration snapshots</li>
 * </ul>
 *
 * <h2>Architecture</h2>
 * <p>The {@link GlobalConfigWatcher} is the single source of truth for all
 * configuration state. This class does not maintain its own cache.</p>
 *
 * <p>All calls to {@link #get(File)} delegate directly to the watcher,
 * ensuring that consumers always receive the most up-to-date configuration.</p>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is thread-safe. The underlying watcher uses concurrent
 * data structures and atomic replacement of {@link YamlConfig} instances.</p>
 *
 * @apiNote
 * {@link YamlConfig} instances are immutable snapshots. Consumers should
 * retrieve them on demand rather than caching references long-term.
 *
 * @since 1.0.5
 */
public final class ConfigManager {

    private final @NotNull JavaPlugin plugin;
    private final @NotNull GlobalConfigWatcher watcher;

    /**
     * Creates a new {@code ConfigManager}.
     *
     * <p>This initializes and starts the {@link GlobalConfigWatcher}.</p>
     *
     * @param plugin owning plugin instance, must not be {@code null}
     *
     * @throws RuntimeException if watcher initialization fails
     */
    public ConfigManager(@NotNull JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");

        try {
            this.watcher = new GlobalConfigWatcher(plugin);
            this.watcher.start();
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize GlobalConfigWatcher", e);
        }
    }

    /**
     * Returns the latest configuration snapshot for the given file.
     *
     * <p>The configuration must have been previously loaded using
     * {@link #getOrLoad(File)} or {@link #getOrLoadResource(String)}.</p>
     *
     * @param file configuration file, must not be {@code null}
     * @return latest {@link YamlConfig} snapshot, never {@code null}
     *
     * @throws IllegalStateException if the configuration is not loaded
     */
    public @NotNull YamlConfig get(@NotNull File file) {
        Path path = normalize(file.toPath());

        YamlConfig config = watcher.get(path);
        if (config == null) {
            throw new IllegalStateException("Config not loaded: " + path);
        }

        return config;
    }

    /**
     * Returns an existing configuration or loads it if not already tracked.
     *
     * <p>If the configuration is not already registered, it is:
     * <ol>
     *     <li>Loaded from disk</li>
     *     <li>Registered with the watcher</li>
     * </ol>
     *
     * @param file configuration file, must not be {@code null}
     * @return latest {@link YamlConfig} snapshot
     */
    public @NotNull YamlConfig getOrLoad(@NotNull File file) {
        Path path = normalize(file.toPath());

        YamlConfig existing = watcher.get(path);
        if (existing != null) {
            return existing;
        }

        return load(file);
    }

    /**
     * Returns the latest configuration snapshot for a file located
     * relative to the plugin's data folder.
     *
     * <p>This is a convenience method that resolves the provided path
     * against {@link JavaPlugin#getDataFolder()}.</p>
     *
     * <p>Example usage:</p>
     * <pre>
     * YamlConfig config = manager.getByPath("configs/example.yml");
     * </pre>
     *
     * @param path relative file path using forward slashes, must not be {@code null}
     * @return latest {@link YamlConfig} snapshot
     *
     * @throws IllegalStateException if the configuration is not loaded
     */
    public @NotNull YamlConfig getByPath(@NotNull String path) {
        File file = resolvePath(path);
        return get(file);
    }

    /**
     * Returns an existing configuration or loads it if not already tracked,
     * using a path relative to the plugin's data folder.
     *
     * <p>Example usage:</p>
     * <pre>
     * YamlConfig config = manager.getOrLoadByPath("configs/example.yml");
     * </pre>
     *
     * @param path relative file path using forward slashes, must not be {@code null}
     * @return latest {@link YamlConfig} snapshot
     */
    public @NotNull YamlConfig getOrLoadByPath(@NotNull String path) {
        File file = resolvePath(path);
        return getOrLoad(file);
    }

    /**
     * Loads a configuration file from plugin resources if necessary.
     *
     * <p>If the file does not exist in the plugin data folder, it is copied
     * from the plugin JAR. The file is then loaded and registered.</p>
     *
     * @param name resource name (for example {@code "config.yml"}), must not be {@code null}
     * @return latest {@link YamlConfig} snapshot
     */
    public @NotNull YamlConfig getOrLoadResource(@NotNull String name) {
        plugin.saveResource(name, false);
        return getOrLoad(new File(plugin.getDataFolder(), name));
    }

    /**
     * Stops tracking the specified configuration file.
     *
     * <p>This removes the configuration from the watcher. If the file still
     * exists, it may be reloaded again if explicitly registered.</p>
     *
     * @param file configuration file, must not be {@code null}
     */
    public void unload(@NotNull File file) {
        Path path = normalize(file.toPath());
        watcher.unregister(path);
    }

    /**
     * Returns all currently tracked configuration snapshots.
     *
     * @return collection of configurations, never {@code null}
     */
    public @NotNull Collection<YamlConfig> getAll() {
        return watcher.getAll();
    }

    /**
     * Loads a configuration file and registers it with the watcher.
     *
     * @param file configuration file, must not be {@code null}
     * @return loaded {@link YamlConfig} snapshot
     *
     * @throws RuntimeException if watcher registration fails
     */
    private @NotNull YamlConfig load(@NotNull File file) {

        ensureFileExists(file);

        YamlConfig config = new YamlConfig(
                file,
                YamlConfiguration.loadConfiguration(file)
        );

        try {
            watcher.register(config);
        } catch (IOException e) {
            throw new RuntimeException("Failed to register watcher for " + file, e);
        }

        return config;
    }

    /**
     * Ensures that the parent directories of the file exist.
     *
     * @param file file to verify, must not be {@code null}
     *
     * @throws IllegalStateException if directory creation fails
     */
    private void ensureFileExists(@NotNull File file) {

        if (file.exists()) {
            return;
        }

        File parent = file.getParentFile();

        if (parent != null && !parent.exists()) {
            if (!parent.mkdirs() && !parent.exists()) {
                throw new IllegalStateException("Failed to create directories: " + parent);
            }
        }
    }

    /**
     * Resolves a relative path against the plugin's data folder.
     *
     * <p>Backslashes are normalized to forward slashes to ensure
     * cross-platform compatibility.</p>
     *
     * @param path relative path, must not be {@code null}
     * @return resolved file
     */
    private @NotNull File resolvePath(@NotNull String path) {
        Objects.requireNonNull(path, "path");

        // Normalize separators (Windows → Unix style)
        String normalized = path.replace("\\", "/");

        return new File(plugin.getDataFolder(), normalized);
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