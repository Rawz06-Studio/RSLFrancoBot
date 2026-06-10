package fr.rawz06.rslfrancobot.api.discord;

import fr.rawz06.rslfrancobot.bot.handlers.*;
import fr.rawz06.rslfrancobot.bot.handlers.allsanity.AllsanityErButtonHandler;
import fr.rawz06.rslfrancobot.bot.handlers.allsanity.AllsanityErDecoupledButtonHandler;
import fr.rawz06.rslfrancobot.bot.handlers.allsanity.AllsanityOnlyButtonHandler;
import fr.rawz06.rslfrancobot.bot.handlers.franco.FrancoButtonHandler;
import fr.rawz06.rslfrancobot.bot.handlers.franco.FrancoRandomHandler;
import fr.rawz06.rslfrancobot.bot.handlers.franco.FrancoSelectMenuHandler;
import fr.rawz06.rslfrancobot.bot.handlers.franco.FrancoValidateHandler;
import fr.rawz06.rslfrancobot.bot.handlers.rsl.*;
import fr.rawz06.rslfrancobot.bot.handlers.salad.*;
import fr.rawz06.rslfrancobot.bot.handlers.std.MixedPoolButtonHandler;
import fr.rawz06.rslfrancobot.bot.handlers.std.S8ButtonHandler;
import fr.rawz06.rslfrancobot.bot.handlers.std.S9ButtonHandler;
import fr.rawz06.rslfrancobot.bot.handlers.std.TotButtonHandler;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Listener JDA qui route les événements Discord vers les handlers appropriés.
 * Fait le pont entre JDA et notre architecture clean.
 */
@Component
@RequiredArgsConstructor
public class JDAEventListener extends ListenerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(JDAEventListener.class);

    private final SeedCommandHandler seedCommandHandler;
    private final SaladCommandHandler saladCommandHandler;
    private final AllCommandHandler allCommandHandler;
    private final InfoCommandHandler infoCommandHandler;
    private final FrancoButtonHandler francoButtonHandler;
    private final RSLButtonHandler rslButtonHandler;
    private final PoTButtonHandler potButtonHandler;
    private final BeginnerButtonHandler beginnerButtonHandler;
    private final RoTButtonHandler roTButtonHandler;
    private final S8ButtonHandler s8ButtonHandler;
    private final S9ButtonHandler s9ButtonHandler;
    private final AllsanityErDecoupledButtonHandler allsanityErDecoupledButtonHandler;
    private final AllsanityErButtonHandler allsanityErButtonHandler;
    private final AllsanityOnlyButtonHandler allsanityOnlyButtonHandler;
    private final FrancoValidateHandler francoValidateHandler;
    private final FrancoSelectMenuHandler francoSelectMenuHandler;
    private final FrancoRandomHandler francoRandomHandler;
    private final SaladNatureButtonHandler saladNatureButtonHandler;
    private final SaladEnemyButtonHandler saladEnemyButtonHandler;
    private final SaladRupeeButtonHandler saladRupeeButtonHandler;
    private final SaladDungeonButtonHandler saladDungeonButtonHandler;
    private final SaladSongsButtonHandler saladSongsButtonHandler;
    private final SaladMixButtonHandler saladMixButtonHandler;
    private final SaladAllButtonHandler saladAllButtonHandler;
    private final TotButtonHandler totButtonHandler;
    private final MixedPoolButtonHandler mixedPoolButtonHandler;

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String commandName = event.getName();
        logger.info("Command received: {} by {}", commandName, event.getUser().getName());

        try {
            switch (commandName) {
                case "seed" -> {
                    var interaction = JDASlashCommandAdapter.from(event);
                    seedCommandHandler.handle(interaction);
                }
                case "salad" -> {
                    var interaction = JDASlashCommandAdapter.from(event);
                    saladCommandHandler.handle(interaction);
                }
                case "all" -> {
                    var interaction = JDASlashCommandAdapter.from(event);
                    allCommandHandler.handle(interaction);
                }
                case "info" -> {
                    var interaction = JDASlashCommandAdapter.from(event);
                    infoCommandHandler.handle(interaction);
                }
            }
        } catch (Exception e) {
            logger.error("Error processing command: {}", commandName, e);
            event.reply("❌ An error occurred while processing the command.").setEphemeral(true).queue();
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String buttonId = event.getComponentId();
        logger.info("Button clicked: {} by {}", buttonId, event.getUser().getName());

        try {
            var interaction = JDAInteractionAdapter.fromButtonEvent(event);

            // Handle level selection buttons
            if (buttonId.startsWith("franco_level_")) {
                String level = buttonId.substring("franco_level_".length());
                francoRandomHandler.handleLevelSelection(interaction, level);
                return;
            }

            // Handle random selection buttons
            if (buttonId.startsWith("franco_random_")) {
                String remaining = buttonId.substring("franco_random_".length());
                // format: {level}_{count}
                int lastUnderscore = remaining.lastIndexOf('_');
                if (lastUnderscore != -1) {
                    String level = remaining.substring(0, lastUnderscore);
                    String countStr = remaining.substring(lastUnderscore + 1);
                    try {
                        int count = Integer.parseInt(countStr);
                        francoRandomHandler.handleRandomSelection(interaction, count, level);
                    } catch (NumberFormatException e) {
                        event.reply("❌ Invalid number format.").setEphemeral(true).queue();
                    }
                } else {
                    // Legacy support or fallback
                    try {
                        int count = Integer.parseInt(remaining);
                        francoRandomHandler.handleRandomSelection(interaction, count, "hard");
                    } catch (NumberFormatException e) {
                        event.reply("❌ Invalid button ID format.").setEphemeral(true).queue();
                    }
                }
                return;
            }

            switch (buttonId) {
                case "seed_franco" -> francoButtonHandler.handle(interaction);
                case "seed_rsl" -> rslButtonHandler.handle(interaction);
                case "seed_pot" -> potButtonHandler.handle(interaction);
                case "seed_beginner" -> beginnerButtonHandler.handle(interaction);
                case "seed_rot" -> roTButtonHandler.handle(interaction);
                case "seed_s8" -> s8ButtonHandler.handle(interaction);
                case "seed_s9" -> s9ButtonHandler.handle(interaction);
                case "seed_tot" -> totButtonHandler.handle(interaction);
                case "seed_mixed" -> mixedPoolButtonHandler.handle(interaction);
                case "seed_allsanity_er_decoupled" -> allsanityErDecoupledButtonHandler.handle(interaction);
                case "seed_allsanity_er" -> allsanityErButtonHandler.handle(interaction);
                case "seed_allsanity_only" -> allsanityOnlyButtonHandler.handle(interaction);
                case "seed_salad_enemy" -> saladEnemyButtonHandler.handle(interaction);
                case "seed_salad_nature" -> saladNatureButtonHandler.handle(interaction);
                case "seed_salad_rupee" -> saladRupeeButtonHandler.handle(interaction);
                case "seed_salad_songs" -> saladSongsButtonHandler.handle(interaction);
                case "seed_salad_dungeon" -> saladDungeonButtonHandler.handle(interaction);
                case "seed_salad_mix" -> saladMixButtonHandler.handle(interaction);
                case "seed_salad_all" -> saladAllButtonHandler.handle(interaction);
                case "franco_validate" -> francoValidateHandler.handle(interaction);
                case "franco_random" -> francoRandomHandler.handle(interaction);
                case "franco_cancel" -> event.reply("Generation cancelled.").setEphemeral(true).queue();
                default -> event.reply("Unknown button: " + buttonId).setEphemeral(true).queue();
            }
        } catch (Exception e) {
            logger.error("Error processing button: {}", buttonId, e);
            event.reply("❌ An error occurred.").setEphemeral(true).queue();
        }
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        String menuId = event.getComponentId();
        logger.info("Menu selected: {} by {}", menuId, event.getUser().getName());

        try {
            var interaction = JDAInteractionAdapter.fromSelectMenuEvent(event);

            if (menuId.startsWith("franco_options_")) {
                francoSelectMenuHandler.handle(interaction);
            } else {
                event.reply("Unknown menu: " + menuId).setEphemeral(true).queue();
            }
        } catch (Exception e) {
            logger.error("Error processing menu: {}", menuId, e);
            event.reply("❌ An error occurred.").setEphemeral(true).queue();
        }
    }
}
