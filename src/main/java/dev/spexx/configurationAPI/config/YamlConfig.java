package dev.spexx.configurationAPI.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;
import java.util.Optional;

/**
 * Immutable snapshot of a YAML configuration file.
 *
 * <p>This record encapsulates the following components:</p>
 * <ul>
 *     <li>The underlying {@link File} on disk</li>
 *     <li>The parsed {@link FileConfiguration} instance</li>
 * </ul>
 *
 * <p>Each instance represents a point-in-time view of a configuration file.
 * When the file is reloaded, a new {@code YamlConfig} instance is created
 * and atomically replaces the previous instance.</p>
 *
 * <h2>Immutability</h2>
 * <p>This class is immutable. All fields are {@code final} and no mutator
 * methods are provided. This ensures thread-safe access without requiring
 * synchronization.</p>
 *
 * <h2>Usage</h2>
 * <p>Instances should be treated as read-only snapshots. Consumers should not
 * cache instances long-term if they require access to the most up-to-date
 * configuration state.</p>
 *
 * <h2>Typed Access</h2>
 * <p>This class provides two styles of accessors:</p>
 * <ul>
 *     <li>Optional-based getters for safe, explicit handling of missing values</li>
 *     <li>Default-based getters for convenience when fallback values are acceptable</li>
 * </ul>
 *
 * @param file the backing file on disk, must not be {@code null}
 * @param config parsed configuration snapshot, must not be {@code null}
 *
 * @since 1.1.0
 */
public record YamlConfig(@NotNull File file, @NotNull FileConfiguration config) {

    /**
     * Returns the underlying configuration file.
     *
     * @return backing file, never {@code null}
     *
     * @since 1.1.0
     */
    @Override
    public @NotNull File file() {
        return file;
    }

    /**
     * Returns the parsed configuration snapshot.
     *
     * <p>The returned {@link FileConfiguration} should be treated as read-only.
     * Modifying it directly may lead to inconsistent behavior.</p>
     *
     * @return configuration snapshot, never {@code null}
     *
     * @since 1.1.0
     */
    @Override
    public @NotNull FileConfiguration config() {
        return config;
    }

    /**
     * Returns a string value at the given path.
     *
     * @param path configuration path, must not be {@code null}
     * @return optional containing the value, or empty if not present
     *
     * @since 1.1.0
     */
    public @NotNull Optional<String> getString(@NotNull String path) {
        return Optional.ofNullable(config.getString(path));
    }

    /**
     * Returns an integer value at the given path.
     *
     * <p>This method avoids Bukkit's implicit default value ({@code 0}) by
     * checking for path existence first.</p>
     *
     * @param path configuration path, must not be {@code null}
     * @return optional containing the value, or empty if not present
     *
     * @since 1.1.0
     */
    public @NotNull Optional<Integer> getInt(@NotNull String path) {
        return config.contains(path)
                ? Optional.of(config.getInt(path))
                : Optional.empty();
    }

    /**
     * Returns a boolean value at the given path.
     *
     * <p>This method avoids Bukkit's implicit default value ({@code false})
     * by checking for path existence first.</p>
     *
     * @param path configuration path, must not be {@code null}
     * @return optional containing the value, or empty if not present
     *
     * @since 1.1.0
     */
    public @NotNull Optional<Boolean> getBoolean(@NotNull String path) {
        return config.contains(path)
                ? Optional.of(config.getBoolean(path))
                : Optional.empty();
    }

    /**
     * Returns a double value at the given path.
     *
     * @param path configuration path, must not be {@code null}
     * @return optional containing the value, or empty if not present
     *
     * @since 1.1.0
     */
    public @NotNull Optional<Double> getDouble(@NotNull String path) {
        return config.contains(path)
                ? Optional.of(config.getDouble(path))
                : Optional.empty();
    }

    /**
     * Returns a float value at the given path.
     *
     * <p>This method internally reads a double value and casts it to float,
     * as Bukkit does not provide a native float accessor.</p>
     *
     * @param path configuration path, must not be {@code null}
     * @return optional containing the value, or empty if not present
     *
     * @since 1.1.0
     */
    public @NotNull Optional<Float> getFloat(@NotNull String path) {
        return config.contains(path)
                ? Optional.of((float) config.getDouble(path))
                : Optional.empty();
    }

    /**
     * Returns a list of strings at the given path.
     *
     * @param path configuration path, must not be {@code null}
     * @return optional containing the list, or empty if not present
     *
     * @since 1.1.0
     */
    public @NotNull Optional<List<String>> getStringList(@NotNull String path) {
        return config.contains(path)
                ? Optional.of(config.getStringList(path))
                : Optional.empty();
    }

    /**
     * Returns a raw object at the given path.
     *
     * @param path configuration path, must not be {@code null}
     * @return optional containing the value, or empty if not present
     *
     * @since 1.1.0
     */
    public @NotNull Optional<Object> get(@NotNull String path) {
        return Optional.ofNullable(config.get(path));
    }

    /**
     * Returns a string value or a default if missing.
     *
     * @param path configuration path, must not be {@code null}
     * @param def fallback value, must not be {@code null}
     * @return resolved value, never {@code null}
     *
     * @since 1.1.0
     */
    public @NotNull String getStringOrDefault(@NotNull String path, @NotNull String def) {
        String value = config.getString(path);
        return value != null ? value : def;
    }

    /**
     * Returns an integer value or a default.
     *
     * @param path configuration path, must not be {@code null}
     * @param def fallback value
     * @return resolved value
     *
     * @since 1.1.0
     */
    public int getIntOrDefault(@NotNull String path, int def) {
        return config.getInt(path, def);
    }

    /**
     * Returns a boolean value or a default.
     *
     * @param path configuration path, must not be {@code null}
     * @param def fallback value
     * @return resolved value
     *
     * @since 1.1.0
     */
    public boolean getBooleanOrDefault(@NotNull String path, boolean def) {
        return config.getBoolean(path, def);
    }

    /**
     * Returns a double value or a default.
     *
     * @param path configuration path, must not be {@code null}
     * @param def fallback value
     * @return resolved value
     *
     * @since 1.1.0
     */
    public double getDoubleOrDefault(@NotNull String path, double def) {
        return config.getDouble(path, def);
    }

    /**
     * Returns a float value or a default.
     *
     * @param path configuration path, must not be {@code null}
     * @param def fallback value
     * @return resolved value
     *
     * @since 1.1.0
     */
    public float getFloatOrDefault(@NotNull String path, float def) {
        return (float) config.getDouble(path, def);
    }

    /**
     * Checks whether a value exists at the given path.
     *
     * @param path configuration path, must not be {@code null}
     * @return {@code true} if present, otherwise {@code false}
     *
     * @since 1.1.0
     */
    public boolean has(@NotNull String path) {
        return config.contains(path);
    }
}