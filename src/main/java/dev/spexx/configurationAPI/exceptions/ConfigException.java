package dev.spexx.configurationAPI.exceptions;

import org.jetbrains.annotations.NotNull;

/**
 * Base exception for all configuration-related errors within the ConfigurationAPI.
 *
 * <p>This exception serves as the root of the configuration exception hierarchy.
 * All specific configuration exceptions (e.g. file errors, permission issues,
 * parsing failures) should extend this class.</p>
 *
 * <p>It extends {@link RuntimeException}, meaning it is unchecked and does not
 * require explicit handling, but can still be caught if needed.</p>
 *
 * <h2>Design Goals</h2>
 * <ul>
 *     <li>Provide a unified exception type for all configuration errors</li>
 *     <li>Allow flexible subclassing for more specific failure scenarios</li>
 *     <li>Keep API usage clean without forcing excessive try-catch blocks</li>
 * </ul>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * throw new ConfigException("Failed to load configuration");
 * }</pre>
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
     * Constructs a new {@link ConfigException} with the specified detail message.
     *
     * @param message the detail message
     *
     * @since 1.3.0
     */
    public ConfigException(@NotNull String message) {
        super(message);
    }

    /**
     * Constructs a new {@link ConfigException} with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause of the exception
     *
     * @since 1.3.0
     */
    public ConfigException(@NotNull String message, @NotNull Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new {@link ConfigException} with the specified cause.
     *
     * @param cause the cause of the exception
     *
     * @since 1.3.0
     */
    public ConfigException(@NotNull Throwable cause) {
        super(cause);
    }
}