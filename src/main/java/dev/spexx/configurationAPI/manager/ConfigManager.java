package dev.spexx.configurationAPI.manager;

import dev.spexx.configurationAPI.config.ConfigLoadResult;
import dev.spexx.configurationAPI.config.ConfigLoadStatus;
import dev.spexx.configurationAPI.config.YamlConfig;
import dev.spexx.configurationAPI.watcher.GlobalConfigWatcher;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;

/**
 * Central access point for configuration files managed by the system.
 *
 * <p>This class is responsible for:</p>
 * <ul>
 *     <li>Loading configuration files from disk</li>
 *     <li>Registering configurations with the global watcher</li>
 *     <li>Providing access to the latest immutable configuration snapshots</li>
 * </ul>
 *
 * <h2>Architecture</h2>
 * <p>The {@link GlobalConfigWatcher} serves as the single source of truth for all
 * configuration state. This class delegates all state management to the watcher
 * and does not maintain its own cache.</p>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is thread-safe. The underlying watcher uses concurrent data structures
 * and atomic replacement of {@link YamlConfig} instances.</p>
 *
 * @apiNote {@link YamlConfig} instances are immutable snapshots. Consumers should
 * retrieve them on demand instead of caching long-term references.
 *
 * @since 1.1.0
 */
public final class ConfigManager {

    private final @NotNull JavaPlugin plugin;
    private final @NotNull GlobalConfigWatcher watcher;

    /**
     * Creates a new {@code ConfigManager}.
     *
     * <p>Initializes and starts the {@link GlobalConfigWatcher}, which begins
     * monitoring registered configuration files for changes.</p>
     *
     * @param plugin owning plugin instance, must not be {@code null}
     *
     * @throws NullPointerException if {@code plugin} is {@code null}
     * @throws RuntimeException if watcher initialization fails
     *
     * @since 1.1.0
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
     * <p>This method does not attempt to load the file. The configuration must
     * already be registered.</p>
     *
     * @param file configuration file, must not be {@code null}
     * @return latest {@link YamlConfig} snapshot, never {@code null}
     *
     * @throws NullPointerException if {@code file} is {@code null}
     * @throws IllegalStateException if the configuration is not loaded
     *
     * @since 1.1.0
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
     * <p>If the configuration is not registered, it is loaded and registered
     * with the watcher.</p>
     *
     * @param file configuration file, must not be {@code null}
     * @return latest {@link YamlConfig} snapshot, never {@code null}
     *
     * @since 1.1.0
     */
    public @NotNull YamlConfig getOrLoad(@NotNull File file) {

        Path path = normalize(file.toPath());

        YamlConfig existing = watcher.get(path);
        if (existing != null) {
            return existing;
        }

        return loadInternal(file);
    }

    /**
     * Loads a configuration file intended for internal plugin usage.
     *
     * <p>This method guarantees that the file exists by copying it from the
     * plugin JAR if missing.</p>
     *
     * <h3>Behavior</h3>
     * <ul>
     *     <li>Validates that the resource exists in the plugin JAR</li>
     *     <li>Copies the resource if the file does not exist</li>
     *     <li>Loads and registers the configuration</li>
     * </ul>
     *
     * @param resourceName resource name (e.g. {@code "config.yml"})
     * @return loaded configuration snapshot
     *
     * @throws IllegalArgumentException if resource is missing in JAR
     *
     * @since 1.1.0
     */
    public @NotNull YamlConfig getInternal(@NotNull String resourceName) {

        Objects.requireNonNull(resourceName, "resourceName");

        // Ensure resource exists inside JAR
        if (plugin.getResource(resourceName) == null) {
            throw new IllegalArgumentException("Resource not found in plugin JAR: " + resourceName);
        }

        File file = new File(plugin.getDataFolder(), resourceName);

        // Copy resource if missing
        if (!file.exists()) {
            plugin.saveResource(resourceName, false);
        }

        return getOrLoad(file);
    }

    /**
     * Attempts to load a configuration file safely.
     *
     * <p>Never throws exceptions. Instead, returns a structured result.</p>
     *
     * @param file configuration file
     * @return result describing outcome
     *
     * @since 1.1.0
     */
    public @NotNull ConfigLoadResult tryLoad(@NotNull File file) {
        try {
            return new ConfigLoadResult(ConfigLoadStatus.LOADED, loadInternal(file), null);
        } catch (Exception e) {
            return new ConfigLoadResult(ConfigLoadStatus.IO_ERROR, null, e);
        }
    }

    /**
     * Attempts to create and load a configuration safely.
     *
     * @param file configuration file
     * @return result describing outcome
     *
     * @since 1.1.0
     */
    public @NotNull ConfigLoadResult tryCreate(@NotNull File file) {

        if (file.exists()) {
            return new ConfigLoadResult(ConfigLoadStatus.ALREADY_EXISTS, get(file), null);
        }

        try {
            ensureParentDirectories(file);
            return new ConfigLoadResult(ConfigLoadStatus.CREATED, loadInternal(file), null);
        } catch (Exception e) {
            return new ConfigLoadResult(ConfigLoadStatus.IO_ERROR, null, e);
        }
    }

    /**
     * Stops tracking a configuration file.
     *
     * @param file configuration file
     *
     * @since 1.1.0
     */
    public void unload(@NotNull File file) {
        watcher.unregister(normalize(file.toPath()));
    }

    /**
     * Returns all tracked configurations.
     *
     * @return collection of configurations
     *
     * @since 1.1.0
     */
    public @NotNull Collection<YamlConfig> getAll() {
        return watcher.getAll();
    }

    /**
     * Stops the underlying watcher.
     *
     * <p>Must be called during plugin shutdown to avoid thread leaks.</p>
     *
     * @since 1.1.0
     */
    public void shutdown() {
        watcher.stop();
    }

    /**
     * Internal load implementation.
     *
     * @param file configuration file
     * @return loaded configuration
     *
     * @since 1.1.0
     */
    private @NotNull YamlConfig loadInternal(@NotNull File file) {

        // Ensure directory structure exists
        ensureParentDirectories(file);

        // Load YAML into memory
        YamlConfig config = new YamlConfig(
                file,
                YamlConfiguration.loadConfiguration(file)
        );

        try {
            // Register with watcher for live updates
            watcher.register(config);
        } catch (IOException e) {
            throw new RuntimeException("Failed to register watcher for " + file, e);
        }

        return config;
    }

    /**
     * Ensures parent directories exist.
     *
     * @param file file whose parent directories should exist
     *
     * @since 1.1.0
     */
    private void ensureParentDirectories(@NotNull File file) {

        File parent = file.getParentFile();

        if (parent != null && !parent.exists()) {
            if (!parent.mkdirs() && !parent.exists()) {
                throw new IllegalStateException("Failed to create directories: " + parent);
            }
        }
    }

    /**
     * Normalizes a file path.
     *
     * @param path raw path
     * @return normalized absolute path
     *
     * @since 1.1.0
     */
    private static @NotNull Path normalize(@NotNull Path path) {
        return path.toAbsolutePath().normalize();
    }
}