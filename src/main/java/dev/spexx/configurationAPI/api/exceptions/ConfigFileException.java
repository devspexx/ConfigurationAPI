package dev.spexx.configurationAPI.api.exceptions;

import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * Exception related to configuration file operations.
 *
 * <p>This exception is used when an error occurs while interacting with a file,
 * such as missing files, invalid paths, or general I/O-related issues.</p>
 *
 * <p>It may optionally carry a reference to the file that caused the failure.</p>
 *
 * @since 1.3.0
 */
public class ConfigFileException extends ConfigException {

    /**
     * The file associated with the exception, if available.
     *
     * @since 1.3.0
     */
    private final File file;

    /**
     * Constructs a new {@link ConfigFileException} with a detail message.
     *
     * @param message the detail message describing the error
     * @since 1.3.0
     */
    public ConfigFileException(@NotNull String message) {
        super(message);
        this.file = null;
    }

    /**
     * Constructs a new {@link ConfigFileException} for a specific file.
     *
     * <p>The file path is appended to the message for easier debugging.</p>
     *
     * @param file    the file related to the error
     * @param message the detail message describing the error
     * @since 1.3.0
     */
    public ConfigFileException(@NotNull File file, @NotNull String message) {
        super(message + ": " + file.getAbsolutePath());
        this.file = file;
    }

    /**
     * Constructs a new {@link ConfigFileException} for a specific file with a cause.
     *
     * <p>The file path is appended to the message for easier debugging.</p>
     *
     * @param file    the file related to the error
     * @param message the detail message describing the error
     * @param cause   the underlying cause of the exception
     * @since 1.3.0
     */
    public ConfigFileException(@NotNull File file,
                               @NotNull String message,
                               @NotNull Throwable cause) {
        super(message + ": " + file.getAbsolutePath(), cause);
        this.file = file;
    }

    /**
     * Returns the file associated with this exception.
     *
     * @return the file, or {@code null} if not specified
     * @since 1.3.0
     */
    public File getFile() {
        return file;
    }
}