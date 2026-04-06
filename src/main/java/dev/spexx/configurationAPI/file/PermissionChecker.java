package dev.spexx.configurationAPI.file;

import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * Utility for checking file system permissions on a {@link File}.
 *
 * <p>Provides direct access to the file's readable, writable, and executable
 * states using the standard {@link File} API.</p>
 *
 * <p>This is typically used for validation before performing file operations
 * such as loading or saving configurations.</p>
 *
 * @param file the file to check, must not be {@code null}
 * @since 1.3.0
 */
public record PermissionChecker(@NotNull File file) {

    /**
     * Returns whether the file is readable.
     *
     * @return {@code true} if the file can be read, otherwise {@code false}
     * @since 1.3.0
     */
    public boolean canRead() {
        return file.canRead();
    }

    /**
     * Returns whether the file is writable.
     *
     * @return {@code true} if the file can be written to, otherwise {@code false}
     * @since 1.3.0
     */
    public boolean canWrite() {
        return file.canWrite();
    }

    /**
     * Returns whether the file is executable.
     *
     * @return {@code true} if the file can be executed, otherwise {@code false}
     * @since 1.3.0
     */
    public boolean canExecute() {
        return file.canExecute();
    }
}