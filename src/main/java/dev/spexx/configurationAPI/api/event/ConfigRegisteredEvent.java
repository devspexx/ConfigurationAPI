package dev.spexx.configurationAPI.api.event;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when a configuration is registered.
 *
 * <p>This event is triggered after the file is loaded and tracked by the watcher.</p>
 *
 * @since 1.3.0
 */
public class ConfigRegisteredEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final @NotNull String configName;
    private final @NotNull FileConfiguration config;
    private final String checksum;

    /**
     * Creates a new registration event.
     *
     * @param configName the configuration file name
     * @param config     the loaded configuration
     * @param checksum   the current checksum
     */
    public ConfigRegisteredEvent(@NotNull String configName,
                                 @NotNull FileConfiguration config,
                                 String checksum) {
        this.configName = configName;
        this.config = config;
        this.checksum = checksum;
    }

    /**
     * Required handler list for Bukkit events.
     *
     * @return the handler list
     */
    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }

    /**
     * Returns the configuration name.
     *
     * @return the configuration file name
     */
    public @NotNull String getConfigName() {
        return configName;
    }

    /**
     * Returns the loaded configuration.
     *
     * @return the configuration instance
     */
    public @NotNull FileConfiguration getConfig() {
        return config;
    }

    /**
     * Returns the checksum.
     *
     * @return checksum or null if unavailable
     */
    public String getChecksum() {
        return checksum;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }
}