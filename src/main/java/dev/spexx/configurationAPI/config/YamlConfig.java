package dev.spexx.configurationAPI.config;

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
 * @apiNote
 * To obtain the latest configuration, retrieve a new instance from the managing
 * component instead of reusing previously obtained references.
 *
 * @implSpec
 * Thread-safety is achieved through immutability and atomic replacement of
 * instances at a higher level (for example, by the configuration manager).
 *
 * @implNote
 * The underlying {@link FileConfiguration} instance is assumed to be used in a
 * read-only manner. Modifying it directly may lead to inconsistent behavior.
 *
 * @since 1.0.0
 */
public final class YamlConfig {

    /**
     * The underlying configuration file on disk.
     */
    private final @NotNull File file;

    /**
     * Parsed configuration snapshot.
     */
    private final @NotNull FileConfiguration config;

    /**
     * Constructs a new {@code YamlConfig} instance.
     *
     * <p>The provided {@link FileConfiguration} is assumed to represent a fully
     * parsed and valid snapshot of the given file.</p>
     *
     * @param file the configuration file, must not be {@code null}
     * @param config the parsed configuration snapshot, must not be {@code null}
     */
    public YamlConfig(
            @NotNull File file,
            @NotNull FileConfiguration config
    ) {
        this.file = file;
        this.config = config;
    }

    /**
     * Returns the underlying configuration file.
     *
     * <p>This refers to the physical file on disk from which this snapshot
     * was created.</p>
     *
     * @return the configuration file, never {@code null}
     */
    public @NotNull File file() {
        return file;
    }

    /**
     * Returns the parsed configuration snapshot.
     *
     * <p>The returned {@link FileConfiguration} represents a stable, immutable
     * view of the configuration at the time this {@code YamlConfig} instance
     * was created.</p>
     *
     * <p>This object should be treated as read-only.</p>
     *
     * @return the configuration snapshot, never {@code null}
     */
    public @NotNull FileConfiguration config() {
        return config;
    }
}