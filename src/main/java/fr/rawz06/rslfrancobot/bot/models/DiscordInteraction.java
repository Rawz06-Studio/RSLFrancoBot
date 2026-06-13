package fr.rawz06.rslfrancobot.bot.models;

import java.util.List;
import java.util.Map;

/**
 * Represents an abstract Discord interaction.
 * Decoupled from JDA, allows testing handlers without depending on Discord.
 */
public interface DiscordInteraction {
    /**
     * @return The ID of the user who triggered the interaction
     */
    String getUserId();

    /**
     * @return The Discord username
     */
    String getUsername();

    /**
     * @return The Discord channel ID
     */
    String getChannelId();

    /**
     * @return Values selected in a menu (if applicable)
     */
    List<String> getSelectedValues();

    /**
     * @return Custom data attached to the button/interaction (if applicable)
     */
    String getCustomId();

    /**
     * Responds to the interaction with a message.
     */
    void reply(DiscordMessage message);

    /**
     * Responds in "defer" mode then updates with a message.
     */
    void deferAndReply(DiscordMessage message);

    /**
     * Defers the response (displays "Bot is thinking...").
     * Used when processing will take time.
     */
    void defer();

    /**
     * Acknowledges the interaction silently without displaying any message.
     * Used for component interactions (buttons/menus) that don't need feedback.
     */
    void acknowledgeSelect();

    /**
     * Edits the deferred response after a defer().
     */
    void editDeferredReply(DiscordMessage message);

    /**
     * Sends a file in response.
     */
    void sendFile(String filename, byte[] content, String mimeType);

    /**
     * Temporarily stores data for this user.
     */
    void storeUserData(String key, Object value);

    /**
     * Retrieves stored data for this user.
     */
    <T> T getUserData(String key, Class<T> type);

    /**
     * Clears stored data for this user for a specific key.
     */
    void clearUserData(String key);

    /**
     * Gets all stored user data.
     */
    Map<String, Object> getAllUserData();

    /**
     * Deletes the original message that triggered this interaction.
     * Useful for cleaning up intermediate messages after final result.
     */
    void deleteOriginalMessage();

    /**
     * Deletes only the trigger message (button/menu) but keeps the deferred reply.
     */
    void deleteTriggerMessage();

    /**
     * Sends a new message in the channel (not as interaction reply).
     * Used to send final results that persist after cleaning up interaction messages.
     */
    void sendChannelMessage(DiscordMessage message);
}
