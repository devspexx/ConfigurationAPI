package dev.spexx.configurationAPI.exceptions;

import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * Represents an exception related to configuration file operations.
 *
 * <p>This exception is thrown when an issue occurs while interacting with a
 * configuration file, such as missing files, invalid paths, or permission-related
 * problems (when not handled by more specific subclasses).</p>
 *
 * <p>It serves as a base class for more specialized file-related exceptions,
 * such as {@link ConfigPermissionException}.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * if (!file.exists()) {
 *     throw new ConfigFileException(file, "File does not exist");
 * }
 * }</pre>
 *
 * @since 1.3.0
 */
public class ConfigFileException extends ConfigException {

    private final File file;

    /**
     * Constructs a new {@link ConfigFileException} with a custom message.
     *
     * @param message the detail message describing the exception
     *
     * @since 1.3.0
     */
    public ConfigFileException(@NotNull String message) {
        super(message);
        this.file = null;
    }

    /**
     * Constructs a new {@link ConfigFileException} for a specific file with a custom message.
     *
     * @param file the file related to the exception
     * @param message the detail message describing the exception
     *
     * @since 1.3.0
     */
    public ConfigFileException(@NotNull File file, @NotNull String message) {
        super(message + ": " + file.getAbsolutePath());
        this.file = file;
    }

    /**
     * Constructs a new {@link ConfigFileException} for a specific file with a custom message and cause.
     *
     * @param file the file related to the exception
     * @param message the detail message describing the exception
     * @param cause the cause of the exception
     *
     * @since 1.3.0
     */
    public ConfigFileException(@NotNull File file,
                               @NotNull String message,
                               @NotNull Throwable cause) {
        super(message + ": " + file.getAbsolutePath(), cause);
        this.file = file;
    }

    /**
     * Returns the file associated with this exception, if available.
     *
     * @return the file, or {@code null} if not specified
     *
     * @since 1.3.0
     */
    public File getFile() {
        return file;
    }
}