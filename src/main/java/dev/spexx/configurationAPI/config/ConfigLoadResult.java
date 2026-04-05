package dev.spexx.configurationAPI.config;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Represents the result of a configuration load or creation operation.
 *
 * <p>This class encapsulates:</p>
 * <ul>
 *     <li>The {@link ConfigLoadStatus} indicating the outcome</li>
 *     <li>An optional {@link YamlConfig} if successful</li>
 *     <li>An optional {@link Exception} if an error occurred</li>
 * </ul>
 *
 * <p>This allows safe, non-throwing APIs for configuration handling.</p>
 *
 * @since 1.1.0
 */
public final class ConfigLoadResult {

    private final ConfigLoadStatus status;
    private final YamlConfig config;
    private final Exception error;

    /**
     * Creates a new result instance.
     *
     * @param status operation status, must not be {@code null}
     * @param config resulting configuration, may be {@code null}
     * @param error exception if an error occurred, may be {@code null}
     */
    public ConfigLoadResult(
            @NotNull ConfigLoadStatus status,
            YamlConfig config,
            Exception error
    ) {
        this.status = status;
        this.config = config;
        this.error = error;
    }

    /**
     * Returns the operation status.
     *
     * @return status value, never {@code null}
     */
    public @NotNull ConfigLoadStatus status() {
        return status;
    }

    /**
     * Returns the loaded configuration if present.
     *
     * @return optional configuration
     */
    public @NotNull Optional<YamlConfig> config() {
        return Optional.ofNullable(config);
    }

    /**
     * Returns the error if one occurred.
     *
     * @return optional exception
     */
    public @NotNull Optional<Exception> error() {
        return Optional.ofNullable(error);
    }
}