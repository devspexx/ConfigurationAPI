package dev.spexx.configurationAPI.exceptions;

import org.jetbrains.annotations.NotNull;

/**
 * Base exception for all configuration-related errors.
 *
 * <p>This exception acts as the root of the configuration exception hierarchy.
 * All configuration-specific exceptions extend this class.</p>
 *
 * <p>It extends {@link RuntimeException}, making it unchecked. This allows
 * configuration operations to remain concise while still providing the option
 * to handle errors explicitly when needed.</p>
 *
 * @since 1.3.0
 */
public class ConfigException extends RuntimeException {

    /**
     * Constructs a new {@link ConfigException} with no detail message.
     *
     * @since 1.3.0
     */
    public ConfigException() {
        super();
    }

    /**
     * Constructs a new {@link ConfigException} with a detail message.
     *
     * @param message the detail message describing the error
     * @since 1.3.0
     */
    public ConfigException(@NotNull String message) {
        super(message);
    }

    /**
     * Constructs a new {@link ConfigException} with a detail message and cause.
     *
     * @param message the detail message describing the error
     * @param cause   the underlying cause of the exception
     * @since 1.3.0
     */
    public ConfigException(@NotNull String message, @NotNull Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new {@link ConfigException} with a cause.
     *
     * @param cause the underlying cause of the exception
     * @since 1.3.0
     */
    public ConfigException(@NotNull Throwable cause) {
        super(cause);
    }
}