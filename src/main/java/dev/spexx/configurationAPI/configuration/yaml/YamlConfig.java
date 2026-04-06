package dev.spexx.configurationAPI.configuration.yaml;

import dev.spexx.configurationAPI.file.PermissionChecker;
import dev.spexx.configurationAPI.exceptions.ConfigException;
import dev.spexx.configurationAPI.exceptions.ConfigFileException;
import dev.spexx.configurationAPI.exceptions.ConfigParseException;
import dev.spexx.configurationAPI.exceptions.ConfigPermissionException;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Represents a YAML configuration file abstraction.
 *
 * <p>The file may or may not exist at construction time. This class provides
 * methods to create, load, and delete the file safely.</p>
 *
 * <p>The loaded configuration is cached and can be retrieved without reloading.</p>
 *
 * @since 1.3.0
 */
public class YamlConfig {

    private final @NotNull File file;
    private @NotNull YamlConfiguration cached = new YamlConfiguration();

    /**
     * Creates a new YAML configuration wrapper.
     *
     * <p>The file is not required to exist. If it exists, it must not be a directory.</p>
     *
     * @param file the configuration file
     *
     * @throws ConfigException if the path points to a directory
     *
     * @since 1.3.0
     */
    public YamlConfig(@NotNull File file) throws ConfigException {
        if (file.isDirectory()) {
            throw new ConfigException("Path points to a directory, expected a file!");
        }

        this.file = file;
    }

    /**
     * Loads the YAML configuration from disk and updates the cache.
     *
     * <p>The file must exist and be readable.</p>
     *
     * @return the loaded {@link YamlConfiguration}
     *
     * @throws ConfigFileException if file is missing or IO fails
     * @throws ConfigParseException if YAML is invalid
     * @throws ConfigPermissionException if file is not readable
     *
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

        // 🔥 update cache
        this.cached = yamlConfiguration;

        return yamlConfiguration;
    }

    /**
     * Reloads the configuration from disk.
     *
     * @throws ConfigFileException       if file is missing or IO fails
     * @throws ConfigParseException      if YAML is invalid
     * @throws ConfigPermissionException if file is not readable
     * @since 1.3.0
     */
    public void reload()
            throws ConfigFileException, ConfigParseException, ConfigPermissionException {
        load();
    }

    /**
     * Saves the cached configuration to disk.
     *
     * @throws ConfigFileException if saving fails
     * @throws ConfigPermissionException if file is not writable
     *
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
     * <p>This does not trigger a file read.</p>
     *
     * @return the cached {@link YamlConfiguration}
     *
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
     *
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
     *
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
     *
     * @since 1.3.0
     */
    public @NotNull File getFile() {
        return file;
    }
}