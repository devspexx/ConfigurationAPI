package dev.spexx.configurationAPI.api.utils;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;

/**
 * Utility class for generating file checksums.
 *
 * <p>This class provides methods to compute cryptographic hashes of files,
 * which can be used for change detection, integrity verification, and caching mechanisms.</p>
 *
 * <p>The primary use case within this API is to detect configuration file changes
 * efficiently by comparing previously stored checksums with newly computed ones.</p>
 *
 * <h2>Implementation Details</h2>
 * <ul>
 *     <li>Uses {@code SHA-256} as the hashing algorithm</li>
 *     <li>Reads files in buffered chunks (8 KB) for memory efficiency</li>
 *     <li>Outputs checksum as a lowercase hexadecimal string</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is stateless and thread-safe.</p>
 *
 * @since 1.3.0
 */
public final class FileChecksum {

    /**
     * Private constructor to prevent instantiation.
     *
     * <p>This class is intended to be used as a static utility holder.</p>
     *
     * @since 1.3.0
     */
    private FileChecksum() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Computes the SHA-256 checksum of the given file.
     *
     * <p>The file is read in chunks to avoid loading the entire file into memory,
     * making this method suitable for both small and large files.</p>
     *
     * <p>The resulting checksum is returned as a lowercase hexadecimal string.</p>
     *
     * @param file the file to compute the checksum for (must not be {@code null})
     * @return the SHA-256 checksum as a hexadecimal string
     * @throws Exception if:
     *                   <ul>
     *                       <li>the file cannot be read</li>
     *                       <li>the hashing algorithm is not available</li>
     *                       <li>an I/O error occurs during reading</li>
     *                   </ul>
     */
    public static @NotNull String computeSha256(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        try (InputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }

        byte[] hashBytes = digest.digest();

        // Convert to hex
        StringBuilder hex = new StringBuilder();
        for (byte b : hashBytes) {
            hex.append(String.format("%02x", b));
        }

        return hex.toString();
    }
}