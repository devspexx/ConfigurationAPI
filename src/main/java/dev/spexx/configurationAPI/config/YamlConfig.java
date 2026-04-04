package dev.spexx.configurationAPI.config;

import dev.spexx.configurationAPI.properties.FileProperties;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * Immutable snapshot of a YAML configuration file.
 *
 * <p>This class encapsulates the following components:</p>
 * <ul>
 *     <li>The underlying {@link File} on disk</li>
 *     <li>The parsed {@link FileConfiguration} instance</li>
 *     <li>Associated {@link FileProperties} metadata</li>
 * </ul>
 *
 * <p>Instances represent a point-in-time view of a configuration and are intended
 * to be treated as read-only. New instances are created during reload operations
 * and atomically swapped by the managing component.</p>
 *
 * @apiNote
 * Consumers should avoid holding long-lived references if they require access to
 * the most up-to-date configuration. Instead, they should retrieve the current
 * instance from the managing component when needed.
 *
 * @implSpec
 * This class is immutable. All fields are {@code final}, and no mutator methods
 * are provided. Thread-safety is achieved through immutability and atomic
 * replacement at a higher level.
 *
 * @implNote
 * Atomic replacement of instances ensures that readers never observe partially
 * updated configuration state and do not require synchronization.
 *
 * @since 1.0.0
 */
public final class YamlConfig {

    /**
     * The underlying configuration file on disk.
     */
    private final File file;

    /**
     * Metadata describing the underlying file.
     */
    private final FileProperties properties;

    /**
     * Parsed configuration snapshot.
     */
    private final FileConfiguration config;

    /**
     * Constructs a new immutable {@code YamlConfig} instance.
     *
     * @param file   the configuration file, must not be {@code null}
     * @param config the parsed configuration snapshot, must not be {@code null}
     */
    public YamlConfig(
            @NotNull File file,
            @NotNull FileConfiguration config) {
        this.file = file;
        this.properties = new FileProperties(file);
        this.config = config;
    }

    /**
     * Returns the underlying configuration file.
     *
     * @return the configuration file, never {@code null}
     */
    public @NotNull File file() {
        return file;
    }

    /**
     * Returns metadata associated with this configuration file.
     *
     * @return file properties, never {@code null}
     */
    public @NotNull FileProperties properties() {
        return properties;
    }

    /**
     * Returns the parsed configuration snapshot.
     *
     * <p>The returned instance represents a stable view of the configuration
     * at the time this {@code YamlConfig} was created.</p>
     *
     * @return the configuration snapshot, never {@code null}
     */
    public @NotNull FileConfiguration config() {
        return config;
    }
}