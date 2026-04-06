package dev.spexx.configurationAPI.exceptions;

import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * Thrown when a configuration file cannot be parsed due to invalid format
 * or syntax errors.
 *
 * <p>This typically occurs when the file content does not conform to the expected
 * structure (e.g. invalid YAML syntax).</p>
 *
 * <p>Common causes include:
 * <ul>
 *     <li>Malformed YAML structure</li>
 *     <li>Invalid indentation</li>
 *     <li>Unexpected tokens or characters</li>
 * </ul>
 * </p>
 *
 * @since 1.3.0
 */
public class ConfigParseException extends ConfigFileException {

    /**
     * Constructs a new {@link ConfigParseException} for a given file.
     *
     * @param file the file that failed to parse
     * @param message the detail message describing the parsing issue
     *
     * @since 1.3.0
     */
    public ConfigParseException(@NotNull File file, @NotNull String message) {
        super(file, "Failed to parse configuration: " + message);
    }

    /**
     * Constructs a new {@link ConfigParseException} with a cause.
     *
     * @param file the file that failed to parse
     * @param message the detail message
     * @param cause the underlying cause
     *
     * @since 1.3.0
     */
    public ConfigParseException(@NotNull File file,
                                @NotNull String message,
                                @NotNull Throwable cause) {
        super(file, "Failed to parse configuration: " + message, cause);
    }
}