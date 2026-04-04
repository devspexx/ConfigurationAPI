package dev.spexx.configurationAPI.manager;

import dev.spexx.configurationAPI.config.YamlConfig;
import dev.spexx.configurationAPI.watcher.YamlConfigWatcher;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central registry and lifecycle manager for {@link YamlConfig} instances.
 *
 * <p>This class is responsible for loading, caching, reloading, and unloading
 * configuration files. Configurations are indexed by their normalized
 * {@link Path}, ensuring consistent identity across different environments.</p>
 *
 * <p>The manager provides thread-safe access to configuration snapshots and
 * coordinates integration with {@link YamlConfigWatcher} for automatic reloads.</p>
 *
 * @apiNote
 * This class is designed for concurrent access. Consumers may safely retrieve
 * configuration instances from multiple threads without additional synchronization.
 * Returned {@link YamlConfig} instances are immutable snapshots and may become
 * outdated after reload operations.
 *
 * @implSpec
 * Configuration instances are replaced atomically using
 * {@link ConcurrentHashMap#computeIfPresent(Object, java.util.function.BiFunction)}.
 * This guarantees that readers either observe the old or the new configuration,
 * but never a partially updated state.
 *
 * <p>Each loaded configuration is associated with a {@link YamlConfigWatcher}
 * that monitors file system changes and triggers reload operations via a callback.</p>
 *
 * @implNote
 * Watchers are created per configuration file and managed alongside the cached
 * configuration instances. When a configuration is unloaded, its associated
 * watcher is stopped and removed to prevent resource leaks.
 *
 * @since 1.0.0
 */
public final class ConfigManager {

    /**
     * Owning plugin instance used for scheduling and logging.
     */
    private final @NotNull JavaPlugin plugin;

    /**
     * Cache of loaded configurations indexed by normalized path.
     */
    private final ConcurrentHashMap<Path, YamlConfig> configs = new ConcurrentHashMap<>();

    /**
     * Active watchers associated with loaded configurations.
     */
    private final ConcurrentHashMap<Path, YamlConfigWatcher> watchers = new ConcurrentHashMap<>();

    /**
     * Constructs a new {@code ConfigManager}.
     *
     * @param plugin the owning plugin instance, must not be {@code null}
     */
    public ConfigManager(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Returns a loaded configuration for the given file.
     *
     * @param file the configuration file, must not be {@code null}
     * @return the corresponding configuration snapshot, never {@code null}
     * @throws IllegalStateException if the configuration is not loaded
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
     * @param file the configuration file, must not be {@code null}
     * @return the existing or newly loaded configuration snapshot, never {@code null}
     */
    public @NotNull YamlConfig getOrLoad(@NotNull File file) {
        Path path = normalize(file.toPath());
        return configs.computeIfAbsent(path, p -> load(file));
    }

    /**
     * Loads a configuration file from the plugin's bundled resources,
     * copying it to the plugin data folder if it does not already exist.
     *
     * <p>The resource is resolved from the plugin JAR and written to the
     * data folder using {@link JavaPlugin#saveResource(String, boolean)}.
     * If the file already exists, it is not overwritten.</p>
     *
     * <p>After ensuring the file exists on disk, it is loaded and managed
     * as a regular configuration via {@link #getOrLoad(File)}.</p>
     *
     * @param name the resource name (for example, {@code "config.yml"}), must not be {@code null}
     * @return the loaded configuration snapshot, never {@code null}
     *
     * @apiNote
     * This method is intended for default configuration files packaged with
     * the plugin. The resource must exist within the plugin JAR under the
     * same path.
     *
     * @implSpec
     * The resource is copied only if the target file does not already exist.
     * Existing files are preserved to avoid overwriting user modifications.
     *
     * @implNote
     * This method bridges bundled resources and the configuration management
     * system by ensuring that all managed configurations operate on real
     * files within the plugin data folder.
     */
    public @NotNull YamlConfig getOrLoadResource(@NotNull String name) {
        plugin.saveResource(name, false);
        return getOrLoad(new File(plugin.getDataFolder(), name));
    }

    /**
     * Reloads the specified configuration file.
     *
     * <p>A new {@link YamlConfig} instance is created and atomically replaces
     * the previous instance in the cache.</p>
     *
     * @param file the configuration file, must not be {@code null}
     */
    public void reload(@NotNull File file) {
        Path path = normalize(file.toPath());

        configs.computeIfPresent(path, (p, oldConfig) -> {

            YamlConfig newConfig = new YamlConfig(
                    file,
                    YamlConfiguration.loadConfiguration(file)
            );

            YamlConfigWatcher watcher = watchers.get(path);

            if (watcher != null) {
                watcher.updateConfig(newConfig);
                watcher.updateChecksum();
            }

            return newConfig;
        });
    }

    /**
     * Unloads the specified configuration and stops its associated watcher.
     *
     * @param file the configuration file, must not be {@code null}
     */
    public void unload(@NotNull File file) {
        Path path = normalize(file.toPath());

        configs.remove(path);

        YamlConfigWatcher watcher = watchers.remove(path);
        if (watcher != null) {
            watcher.stop();
        }
    }

    /**
     * Returns all currently loaded configuration snapshots.
     *
     * @return collection of configurations, never {@code null}
     */
    public @NotNull Collection<YamlConfig> getAll() {
        return configs.values();
    }

    /**
     * Loads a configuration file and initializes its watcher.
     *
     * <p>If the file does not exist, parent directories are created as needed.</p>
     *
     * @param file the configuration file, must not be {@code null}
     * @return the loaded configuration snapshot, never {@code null}
     */
    private @NotNull YamlConfig load(@NotNull File file) {

        Path path = normalize(file.toPath());

        if (!file.exists()) {
            File parent = file.getParentFile();

            if (parent != null && !parent.exists()) {
                if (!parent.mkdirs() && !parent.exists()) {
                    throw new IllegalStateException("Failed to create directories: " + parent);
                }
            }
        }

        YamlConfig config = new YamlConfig(file, YamlConfiguration.loadConfiguration(file));

        YamlConfigWatcher watcher = new YamlConfigWatcher(
                plugin,
                config,
                () -> reload(file)
        );

        try {
            watcher.start();
        } catch (Exception e) {
            throw new RuntimeException("Failed to start watcher", e);
        }

        watchers.put(path, watcher);

        return config;
    }

    /**
     * Normalizes a path to ensure consistent identity.
     *
     * @param path the raw path, must not be {@code null}
     * @return normalized absolute path, never {@code null}
     */
    private @NotNull Path normalize(@NotNull Path path) {
        return path.toAbsolutePath().normalize();
    }
}