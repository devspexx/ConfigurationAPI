package dev.spexx.configurationAPI.config;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Represents the result of a configuration load or creation operation.
 *
 * <p>This class encapsulates the outcome of a configuration-related operation,
 * providing structured, non-throwing feedback to the caller.</p>
 *
 * <p>It contains:</p>
 * <ul>
 *     <li>A {@link ConfigLoadStatus} indicating the result of the operation</li>
 *     <li>An optional {@link YamlConfig} when the operation succeeds</li>
 *     <li>An optional {@link Exception} when an error occurs</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <p>This class is primarily used by safe APIs that avoid throwing exceptions,
 * allowing callers to explicitly handle success and failure cases.</p>
 *
 * <h2>Consistency Guarantees</h2>
 * <ul>
 *     <li>If {@link #status()} is {@link ConfigLoadStatus#LOADED} or {@link ConfigLoadStatus#CREATED},
 *     {@link #config()} will be present</li>
 *     <li>If {@link #status()} is {@link ConfigLoadStatus#IO_ERROR},
 *     {@link #error()} will be present</li>
 * </ul>
 *
 * @since 1.1.0
 */
public final class ConfigLoadResult {

    /**
     * Status representing the outcome of the operation.
     */
    private final ConfigLoadStatus status;

    /**
     * Loaded configuration snapshot, if available.
     */
    private final YamlConfig config;

    /**
     * Exception describing a failure, if one occurred.
     */
    private final Exception error;

    /**
     * Creates a new {@code ConfigLoadResult}.
     *
     * <p>This constructor enforces internal consistency between the provided
     * {@code status}, {@code config}, and {@code error} values.</p>
     *
     * <p>The following invariants apply:</p>
     * <ul>
     *     <li>If {@code status} is {@link ConfigLoadStatus#LOADED} or {@link ConfigLoadStatus#CREATED},
     *     {@code config} must not be {@code null}</li>
     *     <li>If {@code status} is {@link ConfigLoadStatus#IO_ERROR},
     *     {@code error} must not be {@code null}</li>
     * </ul>
     *
     * @param status operation status, must not be {@code null}
     * @param config resulting configuration, may be {@code null} depending on {@code status}
     * @param error exception if an error occurred, may be {@code null} depending on {@code status}
     *
     * @throws NullPointerException if {@code status} is {@code null}
     * @throws IllegalArgumentException if invariants between {@code status}, {@code config},
     * and {@code error} are violated
     *
     * @since 1.1.0
     */
    public ConfigLoadResult(
            @NotNull ConfigLoadStatus status,
            YamlConfig config,
            Exception error
    ) {
        this.status = status;
        this.config = config;
        this.error = error;

        // Enforce invariant: successful states must include a configuration
        if ((status == ConfigLoadStatus.LOADED || status == ConfigLoadStatus.CREATED) && config == null) {
            throw new IllegalArgumentException("Config must not be null for success status: " + status);
        }

        // Enforce invariant: error state must include an exception
        if (status == ConfigLoadStatus.IO_ERROR && error == null) {
            throw new IllegalArgumentException("Error must not be null for IO_ERROR status");
        }
    }

    /**
     * Returns the status of the configuration operation.
     *
     * <p>This value indicates whether the operation succeeded, created a new file,
     * reused an existing configuration, or failed due to an I/O error.</p>
     *
     * @return operation status, never {@code null}
     *
     * @since 1.1.0
     */
    public @NotNull ConfigLoadStatus status() {
        return status;
    }

    /**
     * Returns the loaded configuration snapshot, if available.
     *
     * <p>This value is present when the operation completed successfully
     * (for example {@link ConfigLoadStatus#LOADED} or {@link ConfigLoadStatus#CREATED}).</p>
     *
     * @return optional configuration snapshot
     *
     * @since 1.1.0
     */
    public @NotNull Optional<YamlConfig> config() {
        return Optional.ofNullable(config);
    }

    /**
     * Returns the exception associated with a failed operation, if present.
     *
     * <p>This value is present when {@link #status()} is
     * {@link ConfigLoadStatus#IO_ERROR}.</p>
     *
     * @return optional exception describing the failure
     *
     * @since 1.1.0
     */
    public @NotNull Optional<Exception> error() {
        return Optional.ofNullable(error);
    }
}