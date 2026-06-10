package fr.rawz06.rslfrancobot.api.discord;

import fr.rawz06.rslfrancobot.bot.models.DiscordButton;
import fr.rawz06.rslfrancobot.bot.models.DiscordInteraction;
import fr.rawz06.rslfrancobot.bot.models.DiscordMessage;
import fr.rawz06.rslfrancobot.bot.models.DiscordSelectMenu;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.utils.FileUpload;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adapter for JDA SlashCommandInteractionEvent.
 */
public class JDASlashCommandAdapter implements DiscordInteraction {

    private final SlashCommandInteractionEvent event;
    private final String userId;
    private final String username;
    private final String channelId;

    private static final Map<String, Map<String, Object>> userDataStore = new ConcurrentHashMap<>();

    private JDASlashCommandAdapter(SlashCommandInteractionEvent event) {
        this.event = event;
        this.userId = event.getUser().getId();
        this.username = event.getUser().getName();
        this.channelId = event.getChannel().getId();
    }

    public static JDASlashCommandAdapter from(SlashCommandInteractionEvent event) {
        return new JDASlashCommandAdapter(event);
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
        return List.of();
    }

    @Override
    public String getCustomId() {
        return event.getName();
    }

    @Override
    public void reply(DiscordMessage message) {
        var reply = event.reply(message.getContent())
                .setEphemeral(message.isEphemeral());

        addComponentsToReply(reply, message);
        reply.queue();
    }

    @Override
    public void deferAndReply(DiscordMessage message) {
        event.deferReply().queue(hook ->
                hook.editOriginal(message.getContent()).queue()
        );
    }

    private net.dv8tion.jda.api.interactions.InteractionHook deferredHook;

    @Override
    public void defer() {
        // Block until defer is completed (synchronous)
        this.deferredHook = event.deferReply().complete();
    }

    @Override
    public void editDeferredReply(DiscordMessage message) {
        if (deferredHook != null) {
            var editAction = deferredHook.editOriginal(message.getContent());

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

            editAction.queue();
        } else {
            throw new IllegalStateException("No deferred reply to edit. Call defer() first.");
        }
    }

    @Override
    public void sendFile(String filename, byte[] content, String mimeType) {
        event.deferReply().queue(hook ->
                hook.sendFiles(FileUpload.fromData(content, filename)).queue()
        );
    }

    @Override
    public void storeUserData(String key, Object value) {
        userDataStore.computeIfAbsent(userId, k -> new ConcurrentHashMap<>())
                .put(key, value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getUserData(String key, Class<T> type) {
        Map<String, Object> userData = userDataStore.get(userId);
        if (userData == null) {
            return null;
        }
        return (T) userData.get(key);
    }

    @Override
    public Map<String, Object> getAllUserData() {
        return userDataStore.getOrDefault(userId, Map.of());
    }

    @Override
    public void deleteOriginalMessage() {
        try {
            if (deferredHook != null) {
                deferredHook.deleteOriginal().queue();
            }
        } catch (Exception e) {
            // Silently ignore if message is already deleted or cannot be deleted
        }
    }

    @Override
    public void sendChannelMessage(DiscordMessage message) {
        try {
            event.getChannel().sendMessage(message.getContent()).queue();
        } catch (Exception e) {
            // Silently ignore if cannot send message
        }
    }

    @Override
    public void acknowledgeSelect() {
        // Slash commands don't need silent acknowledgment
        // This method is for component interactions only
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

        Button btn = Button.of(style, button.customId(), button.label());
        return btn.withDisabled(button.disabled());
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
