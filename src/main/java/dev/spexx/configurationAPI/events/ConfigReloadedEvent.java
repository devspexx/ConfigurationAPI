package dev.spexx.configurationAPI.events;

import dev.spexx.configurationAPI.config.YamlConfig;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Event fired when a configuration file has been reloaded.
 *
 * <p>This event is dispatched after a successful configuration reload
 * triggered by a file system change detected by the configuration watcher.</p>
 *
 * <p>The event provides both the previous and updated {@link YamlConfig}
 * instances, along with checksum information and the total time required
 * to perform the reload operation.</p>
 *
 * <h2>Threading</h2>
 * <p>This event is always fired synchronously on the main server thread.
 * Consumers may safely interact with the Bukkit API within event handlers.</p>
 *
 * <h2>Immutability</h2>
 * <p>All fields are immutable and represent a complete snapshot of the reload
 * operation at the time the event was created.</p>
 *
 * @apiNote
 * Consumers should treat {@link YamlConfig} instances as read-only snapshots.
 * To obtain the most recent configuration state, query the managing component
 * rather than storing references long-term.
 *
 * @since 1.0.5
 */
public final class ConfigReloadedEvent extends Event {

    /**
     * Static handler list required by the Bukkit event system.
     */
    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * Previous configuration snapshot before reload.
     */
    private final @NotNull YamlConfig oldConfig;

    /**
     * Updated configuration snapshot after reload.
     */
    private final @NotNull YamlConfig newConfig;

    /**
     * SHA-256 checksum of the configuration file before reload.
     */
    private final @NotNull String oldChecksum;

    /**
     * SHA-256 checksum of the configuration file after reload.
     */
    private final @NotNull String newChecksum;

    /**
     * Time required to reload the configuration, measured in milliseconds.
     */
    private final int reloadTimeMs;

    /**
     * Constructs a new {@code ConfigReloadedEvent}.
     *
     * <p>This constructor is typically invoked internally by the configuration
     * watcher after detecting and applying a file change.</p>
     *
     * @param oldConfig previous configuration snapshot, must not be {@code null}
     * @param newConfig updated configuration snapshot, must not be {@code null}
     * @param oldChecksum checksum of the file before reload, must not be {@code null}
     * @param newChecksum checksum of the file after reload, must not be {@code null}
     * @param reloadTimeMs time taken to reload the configuration in milliseconds
     */
    public ConfigReloadedEvent(
            @NotNull YamlConfig oldConfig,
            @NotNull YamlConfig newConfig,
            @NotNull String oldChecksum,
            @NotNull String newChecksum,
            int reloadTimeMs
    ) {
        this.oldConfig = oldConfig;
        this.newConfig = newConfig;
        this.oldChecksum = oldChecksum;
        this.newChecksum = newChecksum;
        this.reloadTimeMs = reloadTimeMs;
    }

    /**
     * Returns the configuration snapshot prior to reload.
     *
     * <p>This represents the last known valid state before the file change
     * was applied.</p>
     *
     * @return previous configuration snapshot, never {@code null}
     */
    public @NotNull YamlConfig getOldConfig() {
        return oldConfig;
    }

    /**
     * Returns the configuration snapshot after reload.
     *
     * <p>This represents the newly loaded state reflecting the current
     * contents of the configuration file.</p>
     *
     * @return updated configuration snapshot, never {@code null}
     */
    public @NotNull YamlConfig getNewConfig() {
        return newConfig;
    }

    /**
     * Returns the checksum of the configuration file before reload.
     *
     * @return previous SHA-256 checksum, never {@code null}
     */
    public @NotNull String getOldChecksum() {
        return oldChecksum;
    }

    /**
     * Returns the checksum of the configuration file after reload.
     *
     * @return new SHA-256 checksum, never {@code null}
     */
    public @NotNull String getNewChecksum() {
        return newChecksum;
    }

    /**
     * Returns the time required to reload the configuration.
     *
     * <p>The value is measured in milliseconds and represents the duration
     * between the start of the reload process and completion of the new
     * configuration snapshot.</p>
     *
     * @return reload duration in milliseconds
     */
    public int getReloadTimeMs() {
        return reloadTimeMs;
    }

    /**
     * Returns the handler list for this event instance.
     *
     * @return handler list, never {@code null}
     */
    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    /**
     * Returns the static handler list required by the Bukkit event system.
     *
     * @return static handler list, never {@code null}
     */
    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}