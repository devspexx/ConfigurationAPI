package dev.spexx.configurationAPI.events;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Event fired when a configuration has been reloaded.
 *
 * <p>This event provides access to the updated configuration state
 * as well as checksum information for change comparison.</p>
 *
 * @since 1.3.0
 */
public class ConfigReloadEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final @NotNull String configName;
    private final @NotNull FileConfiguration newConfig;
    private final String oldChecksum;
    private final String newChecksum;

    /**
     * Creates a new configuration reload event.
     *
     * @param configName  the name of the configuration file (including extension)
     * @param newConfig   the updated {@link FileConfiguration} instance
     * @param oldChecksum the previous checksum, or {@code null} if not available
     * @param newChecksum the new checksum, or {@code null} if generation failed
     * @since 1.3.0
     */
    public ConfigReloadEvent(@NotNull String configName,
                             @NotNull FileConfiguration newConfig,
                             String oldChecksum,
                             String newChecksum) {
        this.configName = configName;
        this.newConfig = newConfig;
        this.oldChecksum = oldChecksum;
        this.newChecksum = newChecksum;
    }

    /**
     * Returns the list of handlers for this event.
     *
     * <p>This method is required by the Bukkit event system.</p>
     *
     * @return the static {@link HandlerList} for this event
     * @since 1.3.0
     */
    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }

    /**
     * Returns the name of the configuration file.
     *
     * @return the configuration file name (including extension)
     * @since 1.3.0
     */
    public @NotNull String getConfigName() {
        return configName;
    }

    /**
     * Returns the updated configuration.
     *
     * @return the reloaded {@link FileConfiguration}
     * @since 1.3.0
     */
    public @NotNull FileConfiguration getNewConfig() {
        return newConfig;
    }

    /**
     * Returns the checksum before the reload.
     *
     * @return the previous checksum, or {@code null} if not available
     * @since 1.3.0
     */
    public String getOldChecksum() {
        return oldChecksum;
    }

    /**
     * Returns the checksum after the reload.
     *
     * @return the new checksum, or {@code null} if generation failed
     * @since 1.3.0
     */
    public String getNewChecksum() {
        return newChecksum;
    }

    /**
     * Returns the handlers for this event instance.
     *
     * @return the {@link HandlerList}
     * @since 1.3.0
     */
    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }
}