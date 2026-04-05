package dev.spexx.configurationAPI.util;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;

/**
 * Utility class for computing cryptographic checksums of files.
 *
 * <p>This class provides methods for generating hash values that can be used
 * to detect changes in file contents. The primary use case is configuration
 * change detection where file content equality must be verified reliably.</p>
 *
 * <h2>Performance Considerations</h2>
 * <p>Checksum computation requires reading the entire file. This operation
 * may be expensive for large files and should not be performed excessively
 * in performance-critical code paths without appropriate caching or throttling.</p>
 *
 * @apiNote
 * This class is stateless and thread-safe. All methods are static and do not
 * maintain internal state.
 *
 * @implSpec
 * The {@link #sha256(File)} method computes a SHA-256 hash using a buffered
 * stream and returns the result as a lowercase hexadecimal string.
 *
 * @implNote
 * A fixed buffer size of 8192 bytes is used for reading file data. The method
 * relies on {@link MessageDigest} and standard Java I/O without using
 * memory-mapped files or other advanced optimizations.
 *
 * @since 1.0.0
 */
public final class FileChecksum {

    /**
     * Private constructor to prevent instantiation.
     */
    private FileChecksum() {
    }

    /**
     * Computes the SHA-256 checksum of the specified file.
     *
     * <p>The file is read in full using a buffered input stream and processed
     * through a {@link MessageDigest} instance configured for SHA-256.</p>
     *
     * <p>The resulting hash is returned as a lowercase hexadecimal string.</p>
     *
     * @param file the file to compute the checksum for, must not be {@code null}
     * @return the SHA-256 checksum as a lowercase hexadecimal string, never {@code null}
     *
     * @throws RuntimeException if an error occurs while reading the file or
     * computing the checksum
     */
    public static @NotNull String sha256(@NotNull File file) {
        try (FileInputStream fis = new FileInputStream(file)) {

            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] buffer = new byte[8192];
            int read;

            while ((read = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }

            byte[] hash = digest.digest();

            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }

            return hex.toString();

        } catch (Exception e) {
            throw new RuntimeException("Failed to compute checksum for " + file, e);
        }
    }
}