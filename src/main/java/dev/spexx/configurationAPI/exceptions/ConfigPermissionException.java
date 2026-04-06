package dev.spexx.configurationAPI.exceptions;

import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * Thrown when a configuration file does not have the required permissions
 * to perform a specific operation.
 *
 * <p>This exception is typically used during validation of configuration files,
 * such as ensuring the file is readable before loading or writable before saving.</p>
 *
 * <p>It extends {@link ConfigFileException} to provide file-specific context.</p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * if (!file.canRead()) {
 *     throw new ConfigPermissionException(file, "read");
 * }
 * }</pre>
 *
 * @since 1.3.0
 */
public class ConfigPermissionException extends ConfigFileException {

    /**
     * Constructs a new {@link ConfigPermissionException} for a given file
     * and missing permission.
     *
     * @param file the file that failed permission validation
     * @param permission the missing permission (e.g. "read", "write", "execute")
     *
     * @since 1.3.0
     */
    public ConfigPermissionException(@NotNull File file, @NotNull String permission) {
        super(file, "Missing " + permission + " permission");
    }
}