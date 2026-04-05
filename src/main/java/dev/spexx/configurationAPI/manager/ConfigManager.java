package dev.spexx.configurationAPI.manager;

import dev.spexx.configurationAPI.config.YamlConfig;
import dev.spexx.configurationAPI.watcher.GlobalConfigWatcher;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central registry and lifecycle manager for {@link YamlConfig} instances.
 *
 * <p>This class provides controlled access to configuration files by handling:</p>
 * <ul>
 *     <li>Initial loading of configuration files</li>
 *     <li>Registration of configurations for file system monitoring</li>
 *     <li>Thread-safe access to configuration snapshots</li>
 *     <li>Basic lifecycle operations such as unload</li>
 * </ul>
 *
 * <p>File system monitoring and automatic reload behavior are delegated to
 * {@link GlobalConfigWatcher}. This class does not perform direct file watching.</p>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is safe for concurrent use. Internally, a
 * {@link ConcurrentHashMap} is used to store configuration snapshots.
 * Each {@link YamlConfig} instance is immutable, ensuring that readers
 * never observe partially updated state.</p>
 *
 * <h2>Lifecycle</h2>
 * <ol>
 *     <li>Configurations are loaded via {@link #getOrLoad(File)}</li>
 *     <li>Each configuration is registered with the watcher</li>
 *     <li>The watcher updates configuration state when file changes occur</li>
 * </ol>
 *
 * @apiNote
 * Consumers should avoid caching {@link YamlConfig} instances long-term.
 * Instead, they should retrieve the current snapshot when needed to ensure
 * they are using the most recent configuration state.
 *
 * @since 1.0.5
 */
public final class ConfigManager {

    /**
     * Owning plugin instance used for resource access and logging.
     */
    private final @NotNull JavaPlugin plugin;

    /**
     * Global watcher responsible for monitoring configuration file changes.
     */
    private final @NotNull GlobalConfigWatcher watcher;

    /**
     * Cache of configuration snapshots indexed by normalized file path.
     */
    private final ConcurrentHashMap<Path, YamlConfig> configs = new ConcurrentHashMap<>();

    /**
     * Constructs a new {@code ConfigManager} instance.
     *
     * <p>This initializes and starts the {@link GlobalConfigWatcher}.</p>
     *
     * @param plugin the owning plugin instance, must not be {@code null}
     *
     * @throws RuntimeException if the watcher cannot be initialized
     */
    public ConfigManager(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;

        try {
            this.watcher = new GlobalConfigWatcher(plugin);
            this.watcher.start();
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize GlobalConfigWatcher", e);
        }
    }

    /**
     * Returns the configuration associated with the specified file.
     *
     * <p>The file must have been previously loaded using
     * {@link #getOrLoad(File)} or {@link #getOrLoadResource(String)}.</p>
     *
     * @param file the configuration file, must not be {@code null}
     * @return the corresponding {@link YamlConfig} snapshot, never {@code null}
     *
     * @throws IllegalStateException if the configuration has not been loaded
     */
    public @NotNull YamlConfig get(@NotNull File file) {
        Path path = normalize(file.toPath());

        YamlConfig config = configs.get(path);
        if (config == null) {
            throw new IllegalStateException("Config not loaded: " + path);
        }

        return config;
    }

    /**
     * Returns an existing configuration or loads it if not already present.
     *
     * <p>If the configuration is not already cached, it is loaded from disk,
     * registered with the watcher, and stored internally.</p>
     *
     * @param file the configuration file, must not be {@code null}
     * @return the existing or newly loaded {@link YamlConfig} snapshot
     */
    public @NotNull YamlConfig getOrLoad(@NotNull File file) {
        Path path = normalize(file.toPath());
        return configs.computeIfAbsent(path, p -> load(file));
    }

    /**
     * Loads a configuration file from the plugin's bundled resources.
     *
     * <p>If the file does not exist in the plugin data folder, it is copied
     * from the plugin JAR using {@link JavaPlugin#saveResource(String, boolean)}.</p>
     *
     * <p>The resulting file is then loaded and managed like any other configuration.</p>
     *
     * @param name the resource name (for example, {@code "config.yml"}), must not be {@code null}
     * @return the loaded {@link YamlConfig} snapshot
     */
    public @NotNull YamlConfig getOrLoadResource(@NotNull String name) {
        plugin.saveResource(name, false);
        return getOrLoad(new File(plugin.getDataFolder(), name));
    }

    /**
     * Unloads the configuration associated with the given file.
     *
     * <p>This removes the configuration snapshot from the internal cache.
     * The watcher will stop tracking the file once it is removed or no longer exists.</p>
     *
     * @param file the configuration file to unload, must not be {@code null}
     */
    public void unload(@NotNull File file) {
        Path path = normalize(file.toPath());
        configs.remove(path);
    }

    /**
     * Returns all currently loaded configuration snapshots.
     *
     * <p>The returned collection reflects the current internal state and is
     * backed by the underlying map.</p>
     *
     * @return collection of loaded configurations, never {@code null}
     */
    public @NotNull Collection<YamlConfig> getAll() {
        return configs.values();
    }

    /**
     * Loads a configuration file and registers it with the watcher.
     *
     * <p>This method performs the following steps:</p>
     * <ol>
     *     <li>Ensures the file and its parent directories exist</li>
     *     <li>Parses the YAML file into a {@link YamlConfig}</li>
     *     <li>Stores the snapshot in the internal cache</li>
     *     <li>Registers the file with the {@link GlobalConfigWatcher}</li>
     * </ol>
     *
     * @param file the configuration file, must not be {@code null}
     * @return the loaded {@link YamlConfig} snapshot
     *
     * @throws RuntimeException if watcher registration fails
     */
    private @NotNull YamlConfig load(@NotNull File file) {

        Path path = normalize(file.toPath());

        ensureFileExists(file);

        YamlConfig config = new YamlConfig(
                file,
                YamlConfiguration.loadConfiguration(file)
        );

        configs.put(path, config);

        try {
            watcher.register(config);
        } catch (IOException e) {
            throw new RuntimeException("Failed to register watcher for " + file, e);
        }

        return config;
    }

    /**
     * Ensures that the specified file and its parent directories exist.
     *
     * <p>If the file does not exist, parent directories are created if necessary.
     * The file itself is not created.</p>
     *
     * @param file the file to verify, must not be {@code null}
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
     * Normalizes a path to ensure consistent identity.
     *
     * <p>This method converts the path to an absolute, normalized form to avoid
     * inconsistencies caused by relative paths or differing representations.</p>
     *
     * @param path the raw path, must not be {@code null}
     * @return normalized absolute path
     */
    private static @NotNull Path normalize(@NotNull Path path) {
        return path.toAbsolutePath().normalize();
    }
}