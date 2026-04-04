package dev.spexx.configurationAPI.events;

import dev.spexx.configurationAPI.config.YamlConfig;
import dev.spexx.configurationAPI.difference.ConfigLineDiff;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Event fired when a configuration file has been reloaded.
 *
 * <p>This event is dispatched after a successful atomic replacement of the
 * underlying {@link YamlConfig} instance.</p>
 *
 * <p>In addition to checksum validation, this event provides detailed
 * line-level and character-level difference information between the
 * previous and current configuration states.</p>
 *
 * @apiNote
 * This event is always fired synchronously on the main server thread.
 *
 * @implSpec
 * Instances of this event are immutable and represent a completed reload cycle.
 *
 * @implNote
 * Diff data is computed prior to dispatch and is safe for concurrent read access.
 *
 * @since 1.0
 */
public final class ConfigReloadedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final YamlConfig config;
    private final String oldChecksum;
    private final String newChecksum;

    private final int changedLines;
    private final int addedLines;
    private final int removedLines;

    private final List<ConfigLineDiff> diffs;

    /**
     * Constructs a new {@code ConfigReloadedEvent}.
     *
     * @param config        the updated configuration instance, must not be {@code null}
     * @param oldChecksum   checksum of the previous file state
     * @param newChecksum   checksum of the new file state
     * @param changedLines  number of modified lines
     * @param addedLines    number of added lines
     * @param removedLines  number of removed lines
     * @param diffs         list of line-level differences
     *
     * @since 1.0
     */
    public ConfigReloadedEvent(
            @NotNull YamlConfig config,
            @NotNull String oldChecksum,
            @NotNull String newChecksum,
            int changedLines,
            int addedLines,
            int removedLines,
            @NotNull List<ConfigLineDiff> diffs
    ) {
        this.config = config;
        this.oldChecksum = oldChecksum;
        this.newChecksum = newChecksum;
        this.changedLines = changedLines;
        this.addedLines = addedLines;
        this.removedLines = removedLines;
        this.diffs = diffs;
    }

    /**
     * Returns the updated configuration instance.
     *
     * @return configuration snapshot, never {@code null}
     *
     * @since 1.0
     */
    public @NotNull YamlConfig getConfig() {
        return config;
    }

    /**
     * Returns the checksum of the previous file state.
     *
     * @return previous checksum
     *
     * @since 1.0
     */
    public @NotNull String getOldChecksum() {
        return oldChecksum;
    }

    /**
     * Returns the checksum of the new file state.
     *
     * @return new checksum
     *
     * @since 1.0
     */
    public @NotNull String getNewChecksum() {
        return newChecksum;
    }

    /**
     * Returns the number of modified lines.
     *
     * @return number of changed lines
     *
     * @since 1.0
     */
    public int getChangedLines() {
        return changedLines;
    }

    /**
     * Returns the number of added lines.
     *
     * @return number of added lines
     *
     * @since 1.0
     */
    public int getAddedLines() {
        return addedLines;
    }

    /**
     * Returns the number of removed lines.
     *
     * @return number of removed lines
     *
     * @since 1.0
     */
    public int getRemovedLines() {
        return removedLines;
    }

    /**
     * Returns the list of detected line-level differences.
     *
     * @return immutable diff list
     *
     * @since 1.0
     */
    public @NotNull List<ConfigLineDiff> getDiffs() {
        return diffs;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    /**
     * Returns the static handler list required by Bukkit.
     *
     * @return handler list
     *
     * @since 1.0
     */
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}