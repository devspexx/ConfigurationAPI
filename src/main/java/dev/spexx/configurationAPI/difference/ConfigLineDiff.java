package dev.spexx.configurationAPI.difference;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a single line-level difference between two versions of a file.
 *
 * <p>This class encapsulates the following components:</p>
 * <ul>
 *     <li>The line number where the change occurred</li>
 *     <li>The original line content</li>
 *     <li>The updated line content</li>
 *     <li>The character length delta between the two versions</li>
 * </ul>
 *
 * <p>Instances of this class are immutable and represent a snapshot of a
 * detected difference at a specific point in time.</p>
 *
 * @apiNote
 * Instances are safe for concurrent use and may be freely shared across threads.
 * This class is intended for diagnostic, logging, and event propagation purposes.
 *
 * @implSpec
 * The {@code charDelta} value is calculated as:
 * {@code newLine.length() - oldLine.length()}.
 * A positive value indicates an increase in length, while a negative value
 * indicates a reduction.
 *
 * @implNote
 * This class performs no deep or semantic diffing. It provides a lightweight,
 * line-based comparison model suitable for high-frequency change detection
 * scenarios without incurring significant computational overhead.
 *
 * @since 1.0.0
 */
public final class ConfigLineDiff {

    /**
     * The 1-based line number where the change occurred.
     */
    private final int lineNumber;

    /**
     * The original content of the line prior to modification.
     */
    private final String oldLine;

    /**
     * The updated content of the line after modification.
     */
    private final String newLine;

    /**
     * The difference in character length between {@code newLine} and {@code oldLine}.
     */
    private final int charDelta;

    /**
     * Constructs a new {@code ConfigLineDiff} instance.
     *
     * @param lineNumber the 1-based line number where the change occurred
     * @param oldLine    the original line content, must not be {@code null}
     * @param newLine    the updated line content, must not be {@code null}
     */
    public ConfigLineDiff(int lineNumber,
                          @NotNull String oldLine,
                          @NotNull String newLine) {
        this.lineNumber = lineNumber;
        this.oldLine = oldLine;
        this.newLine = newLine;
        this.charDelta = newLine.length() - oldLine.length();
    }

    /**
     * Determines whether the difference between the old and new line
     * consists solely of whitespace changes.
     *
     * <p>This method ignores all whitespace characters (spaces, tabs,
     * and other Unicode whitespace) and compares the normalized content
     * of both lines.</p>
     *
     * <p>Examples:</p>
     * <ul>
     *     <li>{@code "value: 1"} vs {@code "value: 1   "} → {@code true}</li>
     *     <li>{@code "value:    1"} vs {@code "value: 1"} → {@code true}</li>
     *     <li>{@code "value: 1"} vs {@code "value: 2"} → {@code false}</li>
     * </ul>
     *
     * @return {@code true} if only whitespace differs, {@code false} otherwise
     *
     * @apiNote
     * This method performs a lightweight normalization by removing all
     * whitespace characters before comparison.
     *
     * @implSpec
     * The comparison is based on {@code String.replaceAll("\\s+", "")}.
     *
     * @implNote
     * This method does not perform semantic or structural comparison.
     * It is intended for quick detection of formatting-only changes.
     *
     * @since 1.0.1
     */
    public boolean isOnlyWhitespaceChange() {
        return !oldLine.equals(newLine)
                && normalize(oldLine).equals(normalize(newLine));
    }

    /**
     * Normalizes a line by removing all whitespace characters.
     */
    private static @NotNull String normalize(@NotNull String input) {
        return input.replaceAll("\\s+", "");
    }

    /**
     * Returns the line number where the change occurred.
     *
     * @return the 1-based line number
     */
    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * Returns the original line content.
     *
     * @return the previous line value, never {@code null}
     */
    public @NotNull String getOldLine() {
        return oldLine;
    }

    /**
     * Returns the updated line content.
     *
     * @return the new line value, never {@code null}
     */
    public @NotNull String getNewLine() {
        return newLine;
    }

    /**
     * Returns the character length delta between the new and old line.
     *
     * <p>A positive value indicates the new line is longer, while a negative
     * value indicates it is shorter.</p>
     *
     * @return the character length difference
     */
    public int getCharDelta() {
        return charDelta;
    }
}