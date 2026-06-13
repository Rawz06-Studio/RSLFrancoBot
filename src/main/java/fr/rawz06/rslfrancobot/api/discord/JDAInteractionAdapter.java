package fr.rawz06.rslfrancobot.api.discord;

import fr.rawz06.rslfrancobot.bot.models.DiscordButton;
import fr.rawz06.rslfrancobot.bot.models.DiscordInteraction;
import fr.rawz06.rslfrancobot.bot.models.DiscordMessage;
import fr.rawz06.rslfrancobot.bot.models.DiscordSelectMenu;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.utils.FileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JDA adapter for Discord interactions.
 * Translates JDA events into our DiscordInteraction abstraction.
 */
public class JDAInteractionAdapter implements DiscordInteraction {

    private static final Logger logger = LoggerFactory.getLogger(JDAInteractionAdapter.class);

    private final Object event; // ButtonInteractionEvent or StringSelectInteractionEvent
    private final String userId;
    private final String username;
    private final String channelId;
    private final String customId;
    private final List<String> selectedValues;

    // Temporary user data storage (by userId)
    private static final Map<String, Map<String, Object>> userDataStore = new ConcurrentHashMap<>();

    private JDAInteractionAdapter(Object event, String userId, String username, String channelId,
                                   String customId, List<String> selectedValues) {
        this.event = event;
        this.userId = userId;
        this.username = username;
        this.channelId = channelId;
        this.customId = customId;
        this.selectedValues = selectedValues;
    }

    public static JDAInteractionAdapter fromButtonEvent(ButtonInteractionEvent event) {
        return new JDAInteractionAdapter(
                event,
                event.getUser().getId(),
                event.getUser().getName(),
                event.getChannel().getId(),
                event.getComponentId(),
                List.of()
        );
    }

    public static JDAInteractionAdapter fromSelectMenuEvent(StringSelectInteractionEvent event) {
        return new JDAInteractionAdapter(
                event,
                event.getUser().getId(),
                event.getUser().getName(),
                event.getChannel().getId(),
                event.getComponentId(),
                event.getValues()
        );
    }

    @Override
    public String getUserId() {
        return userId;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getChannelId() {
        return channelId;
    }

    @Override
    public List<String> getSelectedValues() {
        return selectedValues;
    }

    @Override
    public String getCustomId() {
        return customId;
    }

    @Override
    public void reply(DiscordMessage message) {
        if (event instanceof ButtonInteractionEvent buttonEvent) {
            sendMessageToJDA(buttonEvent, message);
        } else if (event instanceof StringSelectInteractionEvent selectEvent) {
            sendMessageToJDA(selectEvent, message);
        }
    }

    @Override
    public void deferAndReply(DiscordMessage message) {
        if (event instanceof ButtonInteractionEvent buttonEvent) {
            buttonEvent.deferReply().queue(hook ->
                sendEditedMessage(hook, message)
            );
        } else if (event instanceof StringSelectInteractionEvent selectEvent) {
            selectEvent.deferReply().queue(hook ->
                sendEditedMessage(hook, message)
            );
        }
    }

    private InteractionHook deferredHook;

    @Override
    public void defer() {
        // Block until defer is completed (synchronous)
        if (event instanceof ButtonInteractionEvent buttonEvent) {
            this.deferredHook = buttonEvent.deferReply().complete();
        } else if (event instanceof StringSelectInteractionEvent selectEvent) {
            this.deferredHook = selectEvent.deferReply().complete();
        }
    }

    @Override
    public void editDeferredReply(DiscordMessage message) {
        if (deferredHook != null) {
            sendEditedMessage(deferredHook, message);
        } else {
            throw new IllegalStateException("No deferred reply to edit. Call defer() first.");
        }
    }

    @Override
    public void sendFile(String filename, byte[] content, String mimeType) {
        if (event instanceof ButtonInteractionEvent buttonEvent) {
            buttonEvent.deferReply().queue(hook ->
                hook.sendFiles(FileUpload.fromData(content, filename)).queue()
            );
        } else if (event instanceof StringSelectInteractionEvent selectEvent) {
            selectEvent.deferReply().queue(hook ->
                hook.sendFiles(FileUpload.fromData(content, filename)).queue()
            );
        }
    }

    @Override
    public void storeUserData(String key, Object value) {
        userDataStore.computeIfAbsent(userId, k -> new ConcurrentHashMap<>())
                .put(key, value);
    }

    @Override
    public <T> T getUserData(String key, Class<T> type) {
        Map<String, Object> userData = userDataStore.get(userId);
        if (userData == null) {
            return null;
        }
        return (T) userData.get(key);
    }
    
    @Override
    public void clearUserData(String key) {
        Map<String, Object> userData = userDataStore.get(userId);
        if (userData != null) {
            userData.remove(key);
        }
    }

    @Override
    public Map<String, Object> getAllUserData() {
        return userDataStore.getOrDefault(userId, Map.of());
    }

    @Override
    public void deleteOriginalMessage() {
        try {
            // Delete the deferred reply message if it exists
            if (deferredHook != null) {
                deferredHook.deleteOriginal().queue(
                    null,
                    throwable -> logger.debug("Could not delete deferred original message: {}", throwable.getMessage())
                );
            }

            // Also delete the button/menu message that triggered the interaction
            deleteTriggerMessage();
        } catch (Exception e) {
            logger.debug("Error while attempting to delete messages: {}", e.getMessage());
        }
    }

    @Override
    public void deleteTriggerMessage() {
        try {
            if (event instanceof ButtonInteractionEvent buttonEvent) {
                buttonEvent.getMessage().delete().queue(
                    null,
                    throwable -> logger.debug("Could not delete trigger message: {}", throwable.getMessage())
                );
            } else if (event instanceof StringSelectInteractionEvent selectEvent) {
                selectEvent.getMessage().delete().queue(
                    null,
                    throwable -> logger.debug("Could not delete trigger message: {}", throwable.getMessage())
                );
            }
        } catch (Exception e) {
            logger.debug("Error while attempting to delete trigger message: {}", e.getMessage());
        }
    }

    @Override
    public void sendChannelMessage(DiscordMessage message) {
        try {
            if (event instanceof ButtonInteractionEvent buttonEvent) {
                buttonEvent.getChannel().sendMessage(message.getContent()).queue();
            } else if (event instanceof StringSelectInteractionEvent selectEvent) {
                selectEvent.getChannel().sendMessage(message.getContent()).queue();
            }
        } catch (Exception e) {
            // Silently ignore if cannot send message
        }
    }

    @Override
    public void acknowledgeSelect() {
        try {
            if (event instanceof StringSelectInteractionEvent selectEvent) {
                selectEvent.deferEdit().queue(); // Silent acknowledgment for menus
            } else if (event instanceof ButtonInteractionEvent buttonEvent) {
                buttonEvent.deferEdit().queue(); // Silent acknowledgment for buttons
            }
        } catch (Exception e) {
            // Silently ignore if already acknowledged
        }
    }

    private void sendMessageToJDA(ButtonInteractionEvent event, DiscordMessage message) {
        var reply = event.reply(message.getContent())
                .setEphemeral(message.isEphemeral());

        addComponentsToReply(reply, message);
        reply.queue();
    }

    private void sendMessageToJDA(StringSelectInteractionEvent event, DiscordMessage message) {
        var reply = event.reply(message.getContent())
                .setEphemeral(message.isEphemeral());

        addComponentsToReply(reply, message);
        reply.queue();
    }

    private void sendEditedMessage(InteractionHook hook, DiscordMessage message) {
        var editAction = hook.editOriginal(message.getContent());

        // Convert components
        List<ActionRow> actionRows = new ArrayList<>();

        // Handle button rows (new style with multiple rows)
        if (!message.getButtonRows().isEmpty()) {
            for (List<DiscordButton> row : message.getButtonRows()) {
                List<Button> buttons = row.stream()
                        .map(this::convertButton)
                        .toList();
                actionRows.add(ActionRow.of(buttons));
            }
        }
        // Fallback to old single-row buttons
        else if (!message.getButtons().isEmpty()) {
            List<Button> buttons = message.getButtons().stream()
                    .map(this::convertButton)
                    .toList();
            actionRows.add(ActionRow.of(buttons));
        }

        if (!message.getSelectMenus().isEmpty()) {
            for (DiscordSelectMenu menu : message.getSelectMenus()) {
                actionRows.add(ActionRow.of(convertSelectMenu(menu)));
            }
        }

        if (!actionRows.isEmpty()) {
            editAction.setComponents(actionRows);
        } else {
            editAction.setComponents(); // Clear components
        }

        editAction.queue(
            null,
            throwable -> logger.error("Failed to send edited message: {}", throwable.getMessage())
        );
    }

    private void addComponentsToReply(Object reply, DiscordMessage message) {
        List<ActionRow> actionRows = new ArrayList<>();

        // Handle button rows (new style with multiple rows)
        if (!message.getButtonRows().isEmpty()) {
            for (List<DiscordButton> row : message.getButtonRows()) {
                List<Button> buttons = row.stream()
                        .map(this::convertButton)
                        .toList();
                actionRows.add(ActionRow.of(buttons));
            }
        }
        // Fallback to old single-row buttons
        else if (!message.getButtons().isEmpty()) {
            List<Button> buttons = message.getButtons().stream()
                    .map(this::convertButton)
                    .toList();
            actionRows.add(ActionRow.of(buttons));
        }

        if (!message.getSelectMenus().isEmpty()) {
            for (DiscordSelectMenu menu : message.getSelectMenus()) {
                actionRows.add(ActionRow.of(convertSelectMenu(menu)));
            }
        }

        if (!actionRows.isEmpty()) {
            if (reply instanceof net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction replyAction) {
                replyAction.addComponents(actionRows);
            }
        }
    }

    private Button convertButton(DiscordButton button) {
        ButtonStyle style = switch (button.style()) {
            case PRIMARY -> ButtonStyle.PRIMARY;
            case SECONDARY -> ButtonStyle.SECONDARY;
            case SUCCESS -> ButtonStyle.SUCCESS;
            case DANGER -> ButtonStyle.DANGER;
        };

        return Button.of(style, button.customId(), button.label());
    }

    private StringSelectMenu convertSelectMenu(DiscordSelectMenu menu) {
        StringSelectMenu.Builder builder = StringSelectMenu.create(menu.getCustomId());

        if (menu.getPlaceholder() != null) {
            builder.setPlaceholder(menu.getPlaceholder());
        }

        builder.setMinValues(menu.getMinValues());
        builder.setMaxValues(menu.getMaxValues());

        for (DiscordSelectMenu.SelectOption option : menu.getOptions()) {
            if (option.description() != null) {
                builder.addOption(option.label(), option.value(), option.description());
            } else {
                builder.addOption(option.label(), option.value());
            }
        }

        return builder.build();
    }
}
