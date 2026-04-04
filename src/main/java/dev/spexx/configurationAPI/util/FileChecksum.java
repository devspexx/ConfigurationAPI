package dev.spexx.configurationAPI.util;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;

/**
 * Utility class for computing cryptographic checksums of files.
 *
 * <p>This class provides methods for generating hash values used to detect
 * changes in file contents.</p>
 *
 * @apiNote
 * Checksum computation performs a full file read operation. It should not be
 * used excessively on large files or in performance-critical paths without
 * appropriate caching or throttling.
 *
 * @implSpec
 * The {@link #sha256(File)} method computes a SHA-256 hash of the file contents
 * using a buffered stream. The returned value is encoded as a lowercase
 * hexadecimal string.
 *
 * @implNote
 * A fixed buffer size of 8192 bytes is used for streaming file data. The method
 * relies on {@link MessageDigest} and standard Java I/O without additional
 * optimizations such as memory-mapped files.
 *
 * @since 1.0.0
 */
public final class FileChecksum {

    /**
     * Private constructor to prevent instantiation.
     *
     * @since 1.0.0
     */
    private FileChecksum() {}

    /**
     * Computes the SHA-256 checksum of the given file.
     *
     * <p>The entire file is read and processed through a
     * {@link MessageDigest} instance.</p>
     *
     * @param file the file to compute the checksum for, must not be {@code null}
     * @return the SHA-256 checksum as a lowercase hexadecimal string, never {@code null}
     * @throws RuntimeException if an error occurs while reading the file or computing the hash
     *
     * @since 1.0.0
     */
    public static @NotNull String sha256(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {

            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] buffer = new byte[8192];
            int read;

            while ((read = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }

            byte[] hash = digest.digest();

            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }

            return hex.toString();

        } catch (Exception e) {
            throw new RuntimeException("Failed to compute checksum for " + file, e);
        }
    }
}