package dev.spexx.configurationAPI.exceptions;

import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * Exception thrown when a configuration file cannot be parsed.
 *
 * <p>This occurs when the file content is syntactically invalid or does not
 * conform to the expected format.</p>
 *
 * <p>Typical causes include malformed structure, invalid indentation,
 * or unexpected tokens.</p>
 *
 * @since 1.3.0
 */
public class ConfigParseException extends ConfigFileException {

    /**
     * Constructs a new {@link ConfigParseException} for a specific file.
     *
     * @param file    the file that failed to parse
     * @param message the detail message describing the parsing error
     * @since 1.3.0
     */
    public ConfigParseException(@NotNull File file, @NotNull String message) {
        super(file, "Failed to parse configuration: " + message);
    }

    /**
     * Constructs a new {@link ConfigParseException} for a specific file with a cause.
     *
     * @param file    the file that failed to parse
     * @param message the detail message describing the parsing error
     * @param cause   the underlying cause of the parsing failure
     * @since 1.3.0
     */
    public ConfigParseException(@NotNull File file,
                                @NotNull String message,
                                @NotNull Throwable cause) {
        super(file, "Failed to parse configuration: " + message, cause);
    }
}