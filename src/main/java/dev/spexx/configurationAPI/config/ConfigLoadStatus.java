package dev.spexx.configurationAPI.config;

/**
 * Represents the result status of a configuration load or creation operation.
 *
 * <p>This enum is used in conjunction with {@link ConfigLoadResult} to provide
 * structured, type-safe feedback about configuration handling outcomes.</p>
 *
 * <p>Each constant describes a distinct state that may occur when interacting
 * with configuration files, including successful operations and failure scenarios.</p>
 *
 * <h2>Usage</h2>
 * <p>This enum is primarily used as part of {@link ConfigLoadResult} to allow
 * non-throwing APIs to communicate results without relying on exceptions.</p>
 *
 * <p>Typical usage pattern:</p>
 * <pre>
 * ConfigLoadResult result = manager.tryLoad(file);
 *
 * switch (result.status()) {
 *     case LOADED -> { ... }
 *     case CREATED -> { ... }
 *     case ALREADY_EXISTS -> { ... }
 *     case IO_ERROR -> { ... }
 * }
 * </pre>
 *
 * @since 1.1.0
 */
public enum ConfigLoadStatus {

    /**
     * Indicates that the configuration file was successfully loaded from disk.
     *
     * <p>This status is returned when an existing file is read and parsed
     * without errors.</p>
     *
     * @since 1.1.0
     */
    LOADED,

    /**
     * Indicates that the configuration file was newly created and loaded.
     *
     * <p>This typically occurs when the file did not previously exist and was
     * created (for example, via resource extraction or manual creation)
     * before being loaded.</p>
     *
     * @since 1.1.0
     */
    CREATED,

    /**
     * Indicates that the configuration file already existed and was not
     * created again.
     *
     * <p>This status is commonly returned by creation methods when the target
     * file is already present on disk.</p>
     *
     * @since 1.1.0
     */
    ALREADY_EXISTS,

    /**
     * Indicates that an I/O error occurred during a configuration operation.
     *
     * <p>This may include failures such as:</p>
     * <ul>
     *     <li>File read errors</li>
     *     <li>File write errors</li>
     *     <li>Permission issues</li>
     *     <li>Unexpected file system conditions</li>
     * </ul>
     *
     * <p>When this status is returned, the associated exception can be obtained
     * from {@link ConfigLoadResult#error()}.</p>
     *
     * @since 1.1.0
     */
    IO_ERROR
}