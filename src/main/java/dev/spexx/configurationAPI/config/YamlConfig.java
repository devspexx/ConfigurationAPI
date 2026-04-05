package dev.spexx.configurationAPI.manager;

import dev.spexx.configurationAPI.config.ConfigLoadResult;
import dev.spexx.configurationAPI.config.ConfigLoadStatus;
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
 * <h2>Lifecycle</h2>
 * <p>This class provides multiple levels of control over configuration handling:</p>
 * <ul>
 *     <li>{@link #get(File)} &mdash; retrieve an already loaded configuration</li>
 *     <li>{@link #getOrLoad(File)} &mdash; retrieve or load if missing</li>
 *     <li>{@link #load(File)} &mdash; explicitly load and register</li>
 *     <li>{@link #createIfMissing(File)} &mdash; create file if needed, then load</li>
 *     <li>{@link #tryLoad(File)} &mdash; safe load with structured result</li>
 *     <li>{@link #tryCreate(File)} &mdash; safe create with structured result</li>
 * </ul>
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
     * <p>This initializes and starts the {@link GlobalConfigWatcher}, which begins
     * monitoring registered configuration files for changes.</p>
     *
     * @param plugin owning plugin instance, must not be {@code null}
     *
     * @throws NullPointerException if {@code plugin} is {@code null}
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
     * <p>The configuration must have been previously loaded using one of the
     * loading methods. This method does not attempt to load the file.</p>
     *
     * @param file configuration file, must not be {@code null}
     * @return latest {@link YamlConfig} snapshot, never {@code null}
     *
     * @throws NullPointerException if {@code file} is {@code null}
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
     * <p>If the configuration is not registered, it will be loaded from disk
     * and registered with the watcher.</p>
     *
     * @param file configuration file, must not be {@code null}
     * @return latest {@link YamlConfig} snapshot, never {@code null}
     *
     * @throws NullPointerException if {@code file} is {@code null}
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
     * <p>This method guarantees that the configuration file exists by copying it
     * from the plugin JAR if it is not already present in the plugin's data folder.</p>
     *
     * <p>If the file already exists, it is simply loaded and returned.</p>
     *
     * <p><b>Behavior:</b></p>
     * <ul>
     *     <li>If the file does not exist, it is copied from the plugin resources</li>
     *     <li>If the file exists, it is loaded normally</li>
     *     <li>The configuration is always registered with the watcher</li>
     * </ul>
     *
     * <p><b>Failure Conditions:</b></p>
     * <p>This method will throw an exception if the resource does not exist inside
     * the plugin JAR. It is intended strictly for internal configuration files that
     * are guaranteed to be packaged with the plugin.</p>
     *
     * <p>Example usage:</p>
     * <pre>
     * YamlConfig config = manager.getInternal("config.yml");
     * </pre>
     *
     * @param resourceName name of the resource inside the plugin JAR (e.g. {@code "config.yml"}), must not be {@code null}
     * @return loaded {@link YamlConfig} snapshot, never {@code null}
     *
     * @throws NullPointerException if {@code resourceName} is {@code null}
     * @throws IllegalArgumentException if the resource does not exist in the plugin JAR
     * @throws RuntimeException if loading or registration fails
     */
    public @NotNull YamlConfig getInternal(@NotNull String resourceName) {

        Objects.requireNonNull(resourceName, "resourceName");

        // Validate resource exists in JAR
        if (plugin.getResource(resourceName) == null) {
            throw new IllegalArgumentException(
                    "Resource not found in plugin JAR: " + resourceName
            );
        }

        File file = new File(plugin.getDataFolder(), resourceName);

        // Copy if missing
        if (!file.exists()) {
            plugin.saveResource(resourceName, false);
        }

        return getOrLoad(file);
    }

    /**
     * Explicitly loads a configuration file and registers it with the watcher.
     *
     * <p>This method always attempts to load the file regardless of its current
     * registration state.</p>
     *
     * @param file configuration file, must not be {@code null}
     * @return loaded {@link YamlConfig} snapshot, never {@code null}
     *
     * @throws NullPointerException if {@code file} is {@code null}
     * @throws RuntimeException if loading or registration fails
     */
    public @NotNull YamlConfig load(@NotNull File file) {
        return loadInternal(file);
    }

    /**
     * Ensures the configuration file exists, creating parent directories if needed,
     * and then loads and registers it.
     *
     * @param file configuration file, must not be {@code null}
     * @return loaded {@link YamlConfig} snapshot, never {@code null}
     *
     * @throws NullPointerException if {@code file} is {@code null}
     * @throws RuntimeException if loading or registration fails
     */
    public @NotNull YamlConfig createIfMissing(@NotNull File file) {
        if (!file.exists()) {
            ensureParentDirectories(file);
        }
        return loadInternal(file);
    }

    /**
     * Attempts to load a configuration file safely.
     *
     * <p>This method does not throw exceptions. Instead, it returns a structured
     * {@link ConfigLoadResult} describing the outcome.</p>
     *
     * @param file configuration file, must not be {@code null}
     * @return result object containing status, configuration, or error
     */
    public @NotNull ConfigLoadResult tryLoad(@NotNull File file) {
        try {
            YamlConfig config = loadInternal(file);
            return new ConfigLoadResult(ConfigLoadStatus.LOADED, config, null);
        } catch (Exception e) {
            return new ConfigLoadResult(ConfigLoadStatus.IO_ERROR, null, e);
        }
    }

    /**
     * Attempts to create and load a configuration file safely.
     *
     * <p>If the file already exists, the existing configuration is returned.</p>
     *
     * @param file configuration file, must not be {@code null}
     * @return result object describing the outcome
     */
    public @NotNull ConfigLoadResult tryCreate(@NotNull File file) {

        if (file.exists()) {
            try {
                return new ConfigLoadResult(
                        ConfigLoadStatus.ALREADY_EXISTS,
                        get(file),
                        null
                );
            } catch (Exception e) {
                return new ConfigLoadResult(ConfigLoadStatus.IO_ERROR, null, e);
            }
        }

        try {
            ensureParentDirectories(file);
            YamlConfig config = loadInternal(file);
            return new ConfigLoadResult(ConfigLoadStatus.CREATED, config, null);
        } catch (Exception e) {
            return new ConfigLoadResult(ConfigLoadStatus.IO_ERROR, null, e);
        }
    }

    /**
     * Stops tracking the specified configuration file.
     *
     * @param file configuration file, must not be {@code null}
     */
    public void unload(@NotNull File file) {
        watcher.unregister(normalize(file.toPath()));
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
     * Internal method responsible for loading and registering configurations.
     *
     * @param file configuration file, must not be {@code null}
     * @return loaded {@link YamlConfig} snapshot
     */
    private @NotNull YamlConfig loadInternal(@NotNull File file) {

        ensureParentDirectories(file);

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
     * @param file file whose parent directories should be verified
     *
     * @throws IllegalStateException if directory creation fails
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
     * Normalizes a path to ensure consistent identity.
     *
     * @param path raw path, must not be {@code null}
     * @return normalized absolute path
     */
    private static @NotNull Path normalize(@NotNull Path path) {
        return path.toAbsolutePath().normalize();
    }
}