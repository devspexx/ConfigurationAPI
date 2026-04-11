package dev.spexx.configurationAPI.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/**
 * Fired when a watched configuration file is deleted.
 *
 * <p>This event is triggered when the underlying file is removed
 * from the file system.</p>
 *
 * @since 1.3.2
 */
public class ConfigDeletedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final @NotNull String configName;
    private final @NotNull Path path;

    /**
     * Creates a new deletion event.
     *
     * @param configName the configuration file name
     * @param path       the absolute path of the deleted file
     */
    public ConfigDeletedEvent(@NotNull String configName,
                              @NotNull Path path) {
        this.configName = configName;
        this.path = path;
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
     * Returns the configuration file name.
     *
     * @return the file name
     */
    public @NotNull String getConfigName() {
        return configName;
    }

    /**
     * Returns the absolute path of the deleted file.
     *
     * @return the file path
     */
    public @NotNull Path getPath() {
        return path;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }
}