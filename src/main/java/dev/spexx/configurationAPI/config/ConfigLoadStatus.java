package dev.spexx.configurationAPI.config;

/**
 * Represents the result status of a configuration load or creation operation.
 *
 * <p>This enum is used in conjunction with {@link ConfigLoadResult} to provide
 * structured feedback about configuration handling outcomes.</p>
 *
 * @since 1.1.0
 */
public enum ConfigLoadStatus {

    /**
     * Configuration was successfully loaded from disk.
     */
    LOADED,

    /**
     * Configuration file was newly created and loaded.
     */
    CREATED,

    /**
     * Configuration already existed and was not created again.
     */
    ALREADY_EXISTS,

    /**
     * An I/O error occurred during read/write operations.
     */
    IO_ERROR
}