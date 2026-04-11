package dev.spexx.configurationAPI.api.manager;

import dev.spexx.configurationAPI.api.config.yaml.YamlConfig;
import dev.spexx.configurationAPI.api.config.yaml.YamlConfigWatcher;
import dev.spexx.configurationAPI.api.exceptions.ConfigException;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages multiple {@link YamlConfig} instances.
 *
 * <p>Provides a centralized registry for configuration files, ensuring that each file
 * is registered only once and accessed consistently.</p>
 *
 * <p>Integrates with {@link YamlConfigWatcher} to optionally enable automatic reload
 * when files are modified.</p>
 *
 * @since 1.3.0
 */
public class ConfigManager {

    /**
     * Stores registered configurations keyed by their absolute file path.
     *
     * <p>Using a {@link String} key avoids inconsistencies caused by different
     * {@link File} instances referring to the same path.</p>
     *
     * @since 1.3.0
     */
    private final @NotNull Map<String, YamlConfig> configs = new ConcurrentHashMap<>();

    private final @NotNull YamlConfigWatcher watcher;

    /**
     * Creates a new configuration manager.
     *
     * <p>An internal {@link YamlConfigWatcher} is created for tracking file changes.</p>
     *
     * @param javaPlugin the plugin instance used for scheduling and event dispatching
     * @throws ConfigException if watcher initialization fails
     * @since 1.3.0
     */
    public ConfigManager(@NotNull JavaPlugin javaPlugin) throws ConfigException {
        this.watcher = new YamlConfigWatcher(javaPlugin);
    }

    /**
     * Registers and initializes a configuration file.
     *
     * <p>If the file does not exist, it is created before loading.</p>
     *
     * @param file the configuration file
     * @return the managed {@link YamlConfig}
     * @throws ConfigException if the file is already registered or initialization fails
     * @since 1.3.0
     */
    public @NotNull YamlConfig register(@NotNull File file) throws ConfigException {

        String key = getNormalizedPath(file);

        if (isRegistered(file)) {
            throw new ConfigException("Config already registered: " + key);
        }

        YamlConfig config = initialize(file);

        configs.put(key, config);

        watcher.watch(config);

        return config;
    }

    /**
     * Registers a configuration file backed by a resource inside the plugin JAR.
     *
     * <p>If the target file does not exist, it is first created by copying the specified
     * resource from the plugin JAR. The file is then initialized, loaded, and registered
     * within this manager.</p>
     *
     * <p>Once registered, the configuration is tracked and automatically monitored for
     * changes if the internal watcher is running.</p>
     *
     * @param file         the target configuration file on disk
     * @param resourcePath the path to the resource inside the plugin JAR
     * @param plugin       the plugin used to access the resource
     * @return the managed {@link YamlConfig} instance
     * @throws ConfigException if:
     *                         <ul>
     *                             <li>the configuration is already registered</li>
     *                             <li>the resource cannot be found in the JAR</li>
     *                             <li>the file cannot be copied or initialized</li>
     *                         </ul>
     * @since 1.3.0
     */
    public @NotNull YamlConfig registerFromJar(@NotNull File file,
                                               @NotNull String resourcePath,
                                               @NotNull JavaPlugin plugin) throws ConfigException {

        String key = getNormalizedPath(file);

        if (isRegistered(file)) {
            throw new ConfigException("Config already registered: " + key);
        }

        if (!file.exists()) {
            copyResource(plugin, resourcePath, file);
        }

        YamlConfig config = initialize(file);

        configs.put(key, config);

        watcher.watch(config);

        return config;
    }

    /**
     * Registers a configuration file and applies default values if missing.
     *
     * <p>If the file does not exist, it is created and loaded. The provided default
     * values are then applied only to keys that are not already present in the file.</p>
     *
     * <p>Existing values are never overwritten.</p>
     *
     * @param file the configuration file
     * @param defaults a map of default key-value pairs to apply if missing
     * @return the managed {@link YamlConfig} instance
     *
     * @throws ConfigException if:
     * <ul>
     *     <li>the configuration is already registered</li>
     *     <li>initialization or saving fails</li>
     * </ul>
     *
     * @since 1.3.3
     */
    public @NotNull YamlConfig registerWithDefaults(
            @NotNull File file,
            @NotNull Map<String, Object> defaults
    ) throws ConfigException {

        String key = getNormalizedPath(file);

        if (isRegistered(file)) {
            throw new ConfigException("Config already registered: " + key);
        }

        YamlConfig config = initialize(file);

        boolean changed = false;

        for (Map.Entry<String, Object> entry : defaults.entrySet()) {
            if (!config.get().contains(entry.getKey())) {
                config.get().set(entry.getKey(), entry.getValue());
                changed = true;
            }
        }

        if (changed) {
            config.save();
        }

        configs.put(key, config);
        watcher.watch(config);

        return config;
    }

    /**
     * Initializes a {@link YamlConfig} instance for the given file.
     *
     * <p>If the file does not exist, it is created. The configuration is then
     * loaded and cached.</p>
     *
     * @param file the configuration file to initialize
     * @return the initialized {@link YamlConfig}
     * @throws ConfigException if file creation fails or the path is invalid
     * @since 1.3.0
     */
    private @NotNull YamlConfig initialize(File file) {
        YamlConfig config = new YamlConfig(file);
        config.create();
        config.load();
        return config;
    }

    /**
     * Copies a resource from the plugin JAR to the specified file.
     *
     * <p>Parent directories are created if necessary.</p>
     *
     * @param plugin       the plugin providing the resource
     * @param resourcePath the path inside the plugin JAR
     * @param target       the target file location
     * @throws ConfigException if the resource is missing or copy fails
     * @since 1.3.0
     */
    private void copyResource(@NotNull JavaPlugin plugin,
                              @NotNull String resourcePath,
                              @NotNull File target) throws ConfigException {

        try (InputStream in = plugin.getResource(resourcePath)) {

            if (in == null) {
                throw new ConfigException("Resource not found in jar: " + resourcePath);
            }

            if (target.getParentFile() != null) {
                Files.createDirectories(target.getParentFile().toPath());
            }

            Files.copy(in, target.toPath());

        } catch (IOException e) {
            throw new ConfigException("Failed to copy resource: " + resourcePath, e);
        }
    }

    /**
     * Returns a registered configuration.
     *
     * <p>Lookup is performed using the file's absolute path to ensure consistency.</p>
     *
     * @param file the configuration file
     * @return the {@link YamlConfig}
     * @throws ConfigException if the config is not registered
     * @since 1.3.0
     */
    public @NotNull YamlConfig get(@NotNull File file) throws ConfigException {

        String key = getNormalizedPath(file);

        YamlConfig config = configs.get(key);

        if (config == null) {
            throw new ConfigException("Config not registered: " + key);
        }

        return config;
    }

    /**
     * Retrieves a configuration by a raw file path string.
     *
     * <p>The provided path may be relative (e.g. {@code plugins/MyPlugin/config.yml})
     * or absolute. Relative paths are resolved against the JVM working directory,
     * which may vary depending on the environment.</p>
     *
     * @param path the raw file path (relative or absolute)
     * @return the registered {@link YamlConfig}
     * @throws ConfigException if no config is registered for the resolved path
     * @since 1.3.0
     */
    public @NotNull YamlConfig getByPath(@NotNull String path) throws ConfigException {
        File file = new File(path);
        String key = getNormalizedPath(file);

        YamlConfig config = configs.get(key);

        if (config == null) {
            throw new ConfigException("Config not registered: " + key);
        }

        return config;
    }

    /**
     * Checks whether a configuration is registered.
     *
     * <p>Lookup is performed using the file's absolute path.</p>
     *
     * @param file the configuration file
     * @return {@code true} if registered, {@code false} otherwise
     * @since 1.3.2
     */
    public boolean isRegistered(@NotNull File file) {
        return configs.containsKey(getNormalizedPath(file));
    }

    /**
     * Starts the internal watcher.
     *
     * @throws ConfigException if already running or fails
     * @since 1.3.0
     */
    public void startFileWatcher() throws ConfigException {
        watcher.start();
    }

    /**
     * Stops the internal watcher.
     *
     * @since 1.3.0
     */
    public void stopFileWatcher() {
        watcher.stop();
    }

    /**
     * Resolves a normalized key for the given file.
     *
     * <p>This ensures consistent lookup regardless of relative paths,
     * redundant segments, or platform differences.</p>
     *
     * @param file the file
     * @return normalized absolute path string
     * @since 1.3.2
     */
    private @NotNull String getNormalizedPath(@NotNull File file) {
        return file.toPath().toAbsolutePath().normalize().toString();
    }
}
