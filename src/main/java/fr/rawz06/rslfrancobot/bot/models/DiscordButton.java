package fr.rawz06.rslfrancobot.bot.models;

/**
 * Represents a Discord button.
 */
public record DiscordButton(
        String label,
        String customId,
        Style style,
        boolean disabled
) {
    public enum Style {
        PRIMARY,    // Blue
        SECONDARY,  // Gray
        SUCCESS,    // Green
        DANGER      // Red
    }

    public DiscordButton(String label, String customId) {
        this(label, customId, Style.PRIMARY, false);
    }
    public DiscordButton(String label, String customId, Style style) {
        this(label, customId, style, false);
    }
    public DiscordButton(String label, String customId, boolean disabled) {
        this(label, customId, Style.PRIMARY, disabled);
    }
}
