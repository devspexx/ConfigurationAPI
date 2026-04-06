package dev.spexx.configurationAPI.file;

import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * Represents a utility for checking file system permissions on a given {@link File}.
 *
 * <p>This record provides simple, direct access to the underlying file's permission
 * state using the standard {@link File} API. It allows consumers to verify whether
 * the file can be read, written to, or executed.</p>
 *
 * <p>This is particularly useful when performing pre-validation before file operations
 * such as loading, saving, or executing configuration-related files.</p>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is immutable and thread-safe as it only holds a reference to a {@link File}
 * and performs no state mutation.</p>
 *
 * @param file the file whose permissions are to be checked, must not be {@code null}
 *
 * @since 1.3.0
 */
public record PermissionChecker(@NotNull File file) {

    /**
     * Checks whether the underlying file is readable.
     *
     * <p>This method delegates to {@link File#canRead()}.</p>
     *
     * @return {@code true} if the file can be read; {@code false} otherwise
     *
     * @since 1.3.0
     */
    public boolean canRead() {
        return file.canRead();
    }

    /**
     * Checks whether the underlying file is writable.
     *
     * <p>This method delegates to {@link File#canWrite()}.</p>
     *
     * @return {@code true} if the file can be written to; {@code false} otherwise
     *
     * @since 1.3.0
     */
    public boolean canWrite() {
        return file.canWrite();
    }

    /**
     * Checks whether the underlying file is executable.
     *
     * <p>This method delegates to {@link File#canExecute()}.</p>
     *
     * @return {@code true} if the file can be executed; {@code false} otherwise
     *
     * @since 1.3.0
     */
    public boolean canExecute() {
        return file.canExecute();
    }
}