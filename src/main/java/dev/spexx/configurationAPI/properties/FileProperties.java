package dev.spexx.configurationAPI.properties;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Represents metadata and derived properties of a file.
 *
 * <p>This class provides a read-only abstraction over {@link File} and exposes
 * commonly used attributes such as name, size, timestamps, permissions, and
 * normalized path information.</p>
 *
 * <p>Instances are lightweight and may lazily resolve additional file system
 * attributes when required.</p>
 *
 * @apiNote
 * This class does not actively monitor file system changes. Returned values
 * reflect the state of the file at the time of method invocation and may become
 * stale if the file is modified externally.
 *
 * @implSpec
 * The underlying {@link Path} is normalized using
 * {@link Path#toAbsolutePath()} and {@link Path#normalize()} to ensure consistent
 * identity across different operating systems and path representations.
 *
 * @implNote
 * File attribute access via {@link BasicFileAttributes} is performed lazily and
 * cached after the first successful read. If attribute retrieval fails, methods
 * gracefully fall back to alternative mechanisms or return sentinel values.
 *
 * @since 1.0.0
 */
public final class FileProperties {

    /**
     * The underlying file.
     *
     * @since 1.0.0
     */
    private final @NotNull File file;

    /**
     * Normalized absolute path of the file.
     *
     * @since 1.0.0
     */
    private final @NotNull Path path;

    /**
     * Lazily initialized file attributes.
     *
     * @since 1.0.0
     */
    private BasicFileAttributes attributes;

    /**
     * Constructs a new {@code FileProperties} instance for the given file.
     *
     * @param file the file to inspect, must not be {@code null}
     *
     * @since 1.0.0
     */
    public FileProperties(@NotNull File file) {
        this.file = file;
        this.path = file.toPath().toAbsolutePath().normalize();
    }

    /**
     * Returns the file name including its extension.
     *
     * @return file name with extension, never {@code null}
     *
     * @since 1.0.0
     */
    public @NotNull String getName() {
        return file.getName();
    }

    /**
     * Returns the file name without its extension.
     *
     * @return base file name, never {@code null}
     *
     * @since 1.0.0
     */
    public @NotNull String getBaseName() {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        return (dot == -1) ? name : name.substring(0, dot);
    }

    /**
     * Returns the file extension without the leading dot.
     *
     * @return file extension, or an empty string if none exists
     *
     * @since 1.0.0
     */
    public @NotNull String getExtension() {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        return (dot == -1) ? "" : name.substring(dot + 1);
    }

    /**
     * Returns the absolute file path as a string.
     *
     * @return absolute path, never {@code null}
     *
     * @since 1.0.0
     */
    public @NotNull String getAbsolutePath() {
        return file.getAbsolutePath();
    }

    /**
     * Returns the parent directory of the file.
     *
     * @return parent directory, never {@code null}
     * @throws IllegalStateException if the file has no parent directory
     *
     * @since 1.0.0
     */
    public @NotNull File getParent() {
        File parent = file.getParentFile();
        if (parent == null) {
            throw new IllegalStateException("File has no parent: " + file);
        }
        return parent;
    }

    /**
     * Returns the file size in bytes.
     *
     * @return file size in bytes
     *
     * @since 1.0.0
     */
    public long getFileSize() {
        try {
            return Files.size(path);
        } catch (Exception e) {
            return file.length();
        }
    }

    /**
     * Returns whether the file is empty.
     *
     * @return {@code true} if file size is zero, {@code false} otherwise
     *
     * @since 1.0.0
     */
    public boolean isEmpty() {
        return getFileSize() == 0;
    }

    /**
     * Returns the last modified time of the file.
     *
     * @return last modified timestamp in milliseconds since epoch
     *
     * @since 1.0.0
     */
    public long getLastModified() {
        return file.lastModified();
    }

    /**
     * Returns whether the file exists.
     *
     * @return {@code true} if the file exists, {@code false} otherwise
     *
     * @since 1.0.0
     */
    public boolean exists() {
        return file.exists();
    }

    /**
     * Returns whether the file is hidden.
     *
     * @return {@code true} if the file is hidden, {@code false} otherwise
     *
     * @since 1.0.0
     */
    public boolean isHidden() {
        return file.isHidden();
    }

    /**
     * Returns whether the file is readable.
     *
     * @return {@code true} if readable, {@code false} otherwise
     *
     * @since 1.0.0
     */
    public boolean canRead() {
        return file.canRead();
    }

    /**
     * Returns whether the file is writable.
     *
     * @return {@code true} if writable, {@code false} otherwise
     *
     * @since 1.0.0
     */
    public boolean canWrite() {
        return file.canWrite();
    }

    /**
     * Returns whether the file is executable.
     *
     * @return {@code true} if executable, {@code false} otherwise
     *
     * @since 1.0.0
     */
    public boolean canExecute() {
        return file.canExecute();
    }

    /**
     * Returns the file creation time.
     *
     * @return creation time in milliseconds since epoch, or {@code -1} if unavailable
     *
     * @since 1.0.0
     */
    public long getCreationTime() {
        BasicFileAttributes attrs = attributes();
        return (attrs != null) ? attrs.creationTime().toMillis() : -1;
    }

    /**
     * Returns the last access time of the file.
     *
     * @return last access time in milliseconds since epoch, or {@code -1} if unavailable
     *
     * @since 1.0.0
     */
    public long getLastAccessTime() {
        BasicFileAttributes attrs = attributes();
        return (attrs != null) ? attrs.lastAccessTime().toMillis() : -1;
    }

    /**
     * Returns the normalized {@link Path} representation of the file.
     *
     * @return normalized path, never {@code null}
     *
     * @since 1.0.0
     */
    public @NotNull Path toPath() {
        return path;
    }

    /**
     * Returns cached file attributes, loading them if necessary.
     *
     * @return file attributes, or {@code null} if unavailable
     *
     * @since 1.0.0
     */
    private @Nullable BasicFileAttributes attributes() {
        if (attributes == null) {
            try {
                attributes = Files.readAttributes(path, BasicFileAttributes.class);
            } catch (Exception ignored) {
                return null;
            }
        }
        return attributes;
    }
}