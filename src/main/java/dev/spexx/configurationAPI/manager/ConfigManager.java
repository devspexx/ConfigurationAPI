package dev.spexx.configurationAPI.manager;

import dev.spexx.configurationAPI.configuration.yaml.YamlConfig;
import dev.spexx.configurationAPI.configuration.yaml.YamlConfigWatcher;
import dev.spexx.configurationAPI.exceptions.ConfigException;
import org.bukkit.plugin.java.JavaPlugin;
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
 * <p>This class provides a centralized registry for configuration files,
 * ensuring that each file is only managed once.</p>
 *
 * <p>It also optionally integrates with {@link YamlConfigWatcher} to enable
 * automatic reloading when files are modified.</p>
 *
 * @since 1.3.0
 */
public class ConfigManager {

    /**
     * Stores registered configurations keyed by their absolute file path.
     *
     * <p>Using {@link String} instead of {@link File} avoids inconsistencies caused by
     * different {@link File} instances pointing to the same path.</p>
     *
     * @since 1.3.0
     */
    private final @NotNull Map<String, YamlConfig> configs = new ConcurrentHashMap<>();

    private final @NotNull YamlConfigWatcher watcher;

    /**
     * Creates a new configuration manager.
     *
     * <p>A {@link YamlConfigWatcher} is internally created and used for tracking changes.</p>
     *
     * @throws ConfigException if watcher initialization fails
     *
     * @since 1.3.0
     */
    public ConfigManager() throws ConfigException {
        this.watcher = new YamlConfigWatcher();
    }

    /**
     * Registers and initializes a configuration file.
     *
     * <p>The file is created if it does not exist, then loaded and tracked by the watcher.</p>
     *
     * @param file the configuration file
     *
     * @return the managed {@link YamlConfig}
     *
     * @throws ConfigException if the file is already registered or initialization fails
     *
     * @since 1.3.0
     */
    public @NotNull YamlConfig register(@NotNull File file) throws ConfigException {

        String key = file.getAbsolutePath();

        if (configs.containsKey(key)) {
            throw new ConfigException("Config already registered: " + key);
        }

        YamlConfig config = new YamlConfig(file);

        config.create();
        config.load();

        configs.put(key, config);

        watcher.watch(config);

        return config;
    }

    /**
     * Registers a configuration file using a resource from the plugin JAR as default.
     *
     * <p>If the file does not exist, it is copied from the specified resource path.</p>
     *
     * @param file         the target configuration file
     * @param resourcePath the path inside the plugin JAR
     * @param plugin       the plugin used to access the resource
     * @throws ConfigException if already registered or copy/load fails
     * @since 1.3.0
     */
    public void registerFromJar(@NotNull File file,
                                @NotNull String resourcePath,
                                @NotNull JavaPlugin plugin) throws ConfigException {

        String key = file.getAbsolutePath();

        if (configs.containsKey(key)) {
            throw new ConfigException("Config already registered: " + key);
        }

        if (!file.exists()) {
            copyResource(plugin, resourcePath, file);
        }

        YamlConfig config = new YamlConfig(file);
        config.create();
        config.load();

        configs.put(key, config);

        watcher.watch(config);

    }

    private void copyResource(JavaPlugin plugin, String resourcePath, File target)
            throws ConfigException {

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
     *
     * @return the {@link YamlConfig}
     *
     * @throws ConfigException if the config is not registered
     *
     * @since 1.3.0
     */
    public @NotNull YamlConfig get(@NotNull File file) throws ConfigException {
        String key = file.getAbsolutePath();

        YamlConfig config = configs.get(key);

        if (config == null) {
            throw new ConfigException("Config not registered: " + key);
        }

        return config;
    }

    /**
     * Starts the internal watcher.
     *
     * @throws ConfigException if already running or fails
     *
     * @since 1.3.0
     */
    public void start() throws ConfigException {
        watcher.start();
    }

    /**
     * Stops the internal watcher.
     *
     * @since 1.3.0
     */
    public void stop() {
        watcher.stop();
    }
}