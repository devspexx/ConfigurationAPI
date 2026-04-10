package dev.spexx.configurationAPI.api.exceptions;

import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * Exception thrown when a configuration file lacks the required permission
 * for an operation.
 *
 * <p>This is typically used to validate file access before performing actions
 * such as reading or writing.</p>
 *
 * @since 1.3.0
 */
public class ConfigPermissionException extends ConfigFileException {

    /**
     * Constructs a new {@link ConfigPermissionException} for a specific file.
     *
     * @param file       the file that failed permission validation
     * @param permission the missing permission (e.g. "read", "write", "execute")
     * @since 1.3.0
     */
    public ConfigPermissionException(@NotNull File file, @NotNull String permission) {
        super(file, "Missing " + permission + " permission");
    }
}