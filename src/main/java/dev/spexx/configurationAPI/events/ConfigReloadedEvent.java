package dev.spexx.configurationAPI.events;

import dev.spexx.configurationAPI.config.YamlConfig;
import dev.spexx.configurationAPI.difference.ConfigChangeSummary;
import dev.spexx.configurationAPI.difference.ConfigLineDifference;
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
 * <p>Provides both a high-level summary of changes via
 * {@link ConfigChangeSummary} and detailed per-line differences via
 * {@link ConfigLineDifference}.</p>
 *
 * @apiNote
 * This event is always fired synchronously on the main server thread.
 *
 * @implSpec
 * Instances are immutable. All state is captured at creation time and remains
 * constant for the lifetime of the event.
 *
 * @implNote
 * Diff and summary data are precomputed before event dispatch and are safe for
 * concurrent read access without additional synchronization.
 *
 * @since 1.0.0
 */
public final class ConfigReloadedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * The updated configuration snapshot.
     *
     * @since 1.0.0
     */
    private final YamlConfig config;

    /**
     * SHA-256 checksum of the previous file state.
     *
     * @since 1.0.0
     */
    private final String oldChecksum;

    /**
     * SHA-256 checksum of the new file state.
     *
     * @since 1.0.0
     */
    private final String newChecksum;

    /**
     * Aggregated summary of changes between file versions.
     *
     * @since 1.0.2
     */
    private final ConfigChangeSummary summary;

    /**
     * Immutable list of line-level differences.
     *
     * @since 1.0.1
     */
    private final List<ConfigLineDifference> diffs;

    /**
     * Constructs a new {@code ConfigReloadedEvent}.
     *
     * @param config       the updated configuration snapshot, must not be {@code null}
     * @param oldChecksum  checksum of the previous file state, must not be {@code null}
     * @param newChecksum  checksum of the new file state, must not be {@code null}
     * @param summary      aggregated change summary, must not be {@code null}
     * @param diffs        list of line-level differences, must not be {@code null}
     *
     * @since 1.0.0
     */
    public ConfigReloadedEvent(
            @NotNull YamlConfig config,
            @NotNull String oldChecksum,
            @NotNull String newChecksum,
            @NotNull ConfigChangeSummary summary,
            @NotNull List<ConfigLineDifference> diffs
    ) {
        this.config = config;
        this.oldChecksum = oldChecksum;
        this.newChecksum = newChecksum;
        this.summary = summary;
        this.diffs = List.copyOf(diffs);
    }

    /**
     * Returns the updated configuration snapshot.
     *
     * @return configuration snapshot, never {@code null}
     *
     * @since 1.0.0
     */
    public @NotNull YamlConfig getConfig() {
        return config;
    }

    /**
     * Returns the checksum of the previous file state.
     *
     * @return previous checksum, never {@code null}
     *
     * @since 1.0.0
     */
    public @NotNull String getOldChecksum() {
        return oldChecksum;
    }

    /**
     * Returns the checksum of the new file state.
     *
     * @return new checksum, never {@code null}
     *
     * @since 1.0.0
     */
    public @NotNull String getNewChecksum() {
        return newChecksum;
    }

    /**
     * Returns the aggregated summary of detected changes.
     *
     * <p>This provides a high-level overview of modifications, additions,
     * and removals.</p>
     *
     * @return change summary, never {@code null}
     *
     * @since 1.0.2
     */
    public @NotNull ConfigChangeSummary getSummary() {
        return summary;
    }

    /**
     * Returns the list of detected line-level differences.
     *
     * <p>The returned list is immutable and reflects the exact state at the
     * time the event was created.</p>
     *
     * @return immutable list of diffs, never {@code null}
     *
     * @apiNote
     * The returned list is safe for concurrent iteration without additional synchronization.
     *
     * @implSpec
     * Backed by {@link List#copyOf(java.util.Collection)} to guarantee immutability.
     *
     * @since 1.0.1
     */
    public @NotNull List<ConfigLineDifference> getDiffs() {
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
     * @since 1.0.0
     */
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}