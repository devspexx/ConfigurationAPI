package dev.spexx.configurationAPI.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when a watched configuration file is deleted.
 *
 * <p>The event contains the last known checksum before deletion.</p>
 *
 * @since 1.3.0
 */
public class ConfigDeletedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final @NotNull String configName;
    private final String checksum;

    /**
     * Creates a new deletion event.
     *
     * @param configName the name of the deleted configuration
     * @param checksum   the last known checksum before deletion
     */
    public ConfigDeletedEvent(@NotNull String configName, String checksum) {
        this.configName = configName;
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
     * Returns the last known checksum.
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