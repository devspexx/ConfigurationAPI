package dev.spexx.configurationAPI.difference;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a summary of changes detected between two versions of a configuration file.
 *
 * <p>This class aggregates high-level change metrics, including the number of
 * modified, added, and removed lines.</p>
 *
 * @param changedLines the number of lines that were modified
 * @param addedLines   the number of lines that were added
 * @param removedLines the number of lines that were removed
 *
 * @apiNote
 * Instances are immutable and safe for concurrent use.
 *
 * @implSpec
 * All values are computed during diff analysis and remain constant for the lifetime
 * of the instance.
 *
 * @implNote
 * This class serves as a higher-level abstraction over raw diff data and is intended
 * to simplify consumer logic and improve API clarity.
 *
 * @since 1.0.2
 */
public record ConfigChangeSummary(int changedLines, int addedLines, int removedLines) {

    /**
     * Returns the number of modified lines.
     *
     * @return number of changed lines
     *
     * @since 1.0.2
     */
    public int changedLines() {
        return changedLines;
    }

    /**
     * Returns the number of added lines.
     *
     * @return number of added lines
     *
     * @since 1.0.2
     */
    public int addedLines() {
        return addedLines;
    }

    /**
     * Returns the number of removed lines.
     *
     * @return number of removed lines
     *
     * @since 1.0.2
     */
    public int removedLines() {
        return removedLines;
    }

    /**
     * Returns the total number of affected lines.
     *
     * <p>This value is the sum of changed, added, and removed lines.</p>
     *
     * @return total number of changes
     *
     * @since 1.0.2
     */
    public int getTotalChanges() {
        return changedLines + addedLines + removedLines;
    }

    /**
     * Indicates whether any changes were detected.
     *
     * @return {@code true} if changes exist, {@code false} otherwise
     *
     * @since 1.0.2
     */
    public boolean hasChanges() {
        return getTotalChanges() > 0;
    }

    /**
     * Returns a string representation of this summary.
     *
     * @return string representation of change summary
     *
     * @since 1.0.2
     */
    @Override
    public @NotNull String toString() {
        return "ConfigChangeSummary{" +
                "changed=" + changedLines +
                ", added=" + addedLines +
                ", removed=" + removedLines +
                '}';
    }
}