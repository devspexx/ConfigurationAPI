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

    private final @NotNull Map<File, YamlConfig> configs = new ConcurrentHashMap<>();
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
     * <p>If the file is already registered, an exception is thrown.</p>
     *
     * <p>The file will be created if it does not exist, loaded, and registered
     * for watching.</p>
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

        if (configs.containsKey(file)) {
            throw new ConfigException("Config already registered: " + file.getAbsolutePath());
        }

        YamlConfig config = new YamlConfig(file);

        config.create();
        config.load();

        configs.put(file, config);

        watcher.watch(file);

        return config;
    }

    public @NotNull YamlConfig registerWithDefaults(@NotNull File file,
                                                    @NotNull String resourcePath,
                                                    @NotNull JavaPlugin plugin) throws ConfigException {

        if (configs.containsKey(file)) {
            throw new ConfigException("Config already registered: " + file.getAbsolutePath());
        }

        // copy from jar if missing
        if (!file.exists()) {
            copyResource(plugin, resourcePath, file);
        }

        YamlConfig config = new YamlConfig(file);
        config.load();

        configs.put(file, config);
        watcher.watch(file);

        return config;
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
     * @param file the configuration file
     *
     * @return the {@link YamlConfig}
     *
     * @throws ConfigException if the config is not registered
     *
     * @since 1.3.0
     */
    public @NotNull YamlConfig get(@NotNull File file) throws ConfigException {
        YamlConfig config = configs.get(file);

        if (config == null) {
            throw new ConfigException("Config not registered: " + file.getAbsolutePath());
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