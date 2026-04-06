package dev.spexx.configurationAPI.configuration.yaml;

import dev.spexx.configurationAPI.exceptions.ConfigException;
import dev.spexx.configurationAPI.exceptions.ConfigFileException;
import dev.spexx.configurationAPI.exceptions.ConfigParseException;
import dev.spexx.configurationAPI.exceptions.ConfigPermissionException;
import dev.spexx.configurationAPI.file.PermissionChecker;
import dev.spexx.configurationAPI.utils.FileChecksum;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Represents a YAML configuration file abstraction.
 *
 * <p>The file may or may not exist at construction time. Operations such as
 * {@link #create()}, {@link #load()}, and {@link #save()} control the file lifecycle.</p>
 *
 * <p>The loaded configuration is cached and can be accessed without re-reading the file.</p>
 *
 * @since 1.3.0
 */
public class YamlConfig {

    private final @NotNull File file;

    private volatile @NotNull YamlConfiguration cached = new YamlConfiguration();

    /**
     * Cached SHA-256 checksum of the configuration file.
     *
     * <p>This value represents the last known checksum of the file after a successful
     * {@link #load()} operation. It is used for efficient change detection to avoid
     * unnecessary reloads.</p>
     *
     * <p>The value may be {@code null} if:
     * <ul>
     *     <li>the configuration has not been loaded yet</li>
     *     <li>checksum generation failed during loading</li>
     * </ul>
     *
     * <p>This field is updated internally and should be treated as read-only.</p>
     */
    private volatile String cachedChecksum = null;

    /**
     * Creates a new YAML configuration wrapper.
     *
     * <p>If the file exists, it must not be a directory.</p>
     *
     * @param file the configuration file
     * @throws ConfigException if the path points to a directory
     * @since 1.3.0
     */
    public YamlConfig(@NotNull File file) throws ConfigException {
        if (file.isDirectory()) {
            throw new ConfigException("Path points to a directory, expected a file!");
        }

        this.file = file;
    }

    /**
     * Loads the configuration from disk and updates the cache.
     *
     * <p>The file must exist and be readable.</p>
     *
     * @return the loaded {@link YamlConfiguration}
     * @throws ConfigFileException       if the file does not exist or I/O fails
     * @throws ConfigParseException      if the file contains invalid YAML
     * @throws ConfigPermissionException if the file is not readable
     * @since 1.3.0
     */
    public @NotNull YamlConfiguration load()
            throws ConfigFileException, ConfigParseException, ConfigPermissionException {

        if (!file.exists()) {
            throw new ConfigFileException(file, "File does not exist. Did you forget to call create()?");
        }

        PermissionChecker checker = new PermissionChecker(file);
        if (!checker.canRead()) {
            throw new ConfigPermissionException(file, "read");
        }

        YamlConfiguration yamlConfiguration = new YamlConfiguration();

        try {
            yamlConfiguration.load(file);
        } catch (IOException e) {
            throw new ConfigFileException(file, "I/O error while loading", e);
        } catch (InvalidConfigurationException e) {
            throw new ConfigParseException(file, "Invalid YAML format", e);
        }

        // update the config with new
        this.cached = yamlConfiguration;

        // try to generate checksum
        try {
            this.cachedChecksum = FileChecksum.computeSha256(file);
        } catch (Exception e) {
            e.printStackTrace(); // log the exception
            this.cachedChecksum = null;
        }

        return yamlConfiguration;
    }

    /**
     * Returns the cached SHA-256 checksum of the configuration file.
     *
     * <p>The checksum reflects the state of the file at the time of the last successful
     * {@link #load()} or {@link #reload()} operation.</p>
     *
     * <p>This method does not perform any file I/O and simply returns the previously
     * computed checksum.</p>
     *
     * @return the cached checksum, or {@code null} if not yet available or if checksum generation failed
     * @since 1.3.0
     */
    public String getCachedChecksum() {
        return cachedChecksum;
    }

    /**
     * Reloads the configuration from disk.
     *
     * <p>This method is equivalent to {@link #load()} and updates the cached state.</p>
     *
     * @throws ConfigFileException       if the file does not exist or I/O fails
     * @throws ConfigParseException      if the file contains invalid YAML
     * @throws ConfigPermissionException if the file is not readable
     * @since 1.3.0
     */
    public void reload()
            throws ConfigFileException, ConfigParseException, ConfigPermissionException {
        load();
    }

    /**
     * Saves the cached configuration to disk.
     *
     * <p>The file must be writable.</p>
     *
     * @throws ConfigFileException       if saving fails
     * @throws ConfigPermissionException if the file is not writable
     * @since 1.3.0
     */
    public void save() throws ConfigFileException, ConfigPermissionException {

        PermissionChecker checker = new PermissionChecker(file);
        if (!checker.canWrite()) {
            throw new ConfigPermissionException(file, "write");
        }

        try {
            cached.save(file);
        } catch (IOException e) {
            throw new ConfigFileException(file, "Failed to save config", e);
        }
    }

    /**
     * Returns the cached configuration.
     *
     * <p>
     * Modifications affect the cached configuration directly.
     * Don't forget to save the cached config after you've made changes.
     * <p/>
     * <p>This method does not perform any file I/O.</p>
     *
     * @return the cached {@link YamlConfiguration}
     * @since 1.3.0
     */
    public @NotNull YamlConfiguration get() {
        return cached;
    }

    /**
     * Creates the file if it does not already exist.
     *
     * <p>Parent directories are created if necessary.</p>
     *
     * @throws ConfigFileException if creation fails
     * @since 1.3.0
     */
    public void create() throws ConfigFileException {
        if (file.exists()) return;

        try {
            if (file.getParentFile() != null) {
                Files.createDirectories(file.getParentFile().toPath());
            }
            Files.createFile(file.toPath());
        } catch (IOException e) {
            throw new ConfigFileException(file, "Failed to create file", e);
        }
    }

    /**
     * Deletes the file if it exists.
     *
     * @throws ConfigFileException if deletion fails
     * @since 1.3.0
     */
    public void delete() throws ConfigFileException {
        try {
            Files.deleteIfExists(file.toPath());
        } catch (IOException e) {
            throw new ConfigFileException(file, "Failed to delete file", e);
        }
    }

    /**
     * Returns the underlying file.
     *
     * @return the configuration file
     * @since 1.3.0
     */
    public @NotNull File getFile() {
        return file;
    }
}