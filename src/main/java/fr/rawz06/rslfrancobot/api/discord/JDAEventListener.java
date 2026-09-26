package fr.rawz06.rslfrancobot.api.discord;

import fr.rawz06.rslfrancobot.bot.handlers.*;
import fr.rawz06.rslfrancobot.bot.handlers.franco.FrancoButtonHandler;
import fr.rawz06.rslfrancobot.bot.handlers.franco.FrancoRandomHandler;
import fr.rawz06.rslfrancobot.bot.handlers.franco.FrancoSelectMenuHandler;
import fr.rawz06.rslfrancobot.bot.handlers.franco.FrancoValidateHandler;
import fr.rawz06.rslfrancobot.engine.domain.entities.SeedMode;
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
    private final StandardSeedButtonHandler standardSeedButtonHandler;
    private final FrancoValidateHandler francoValidateHandler;
    private final FrancoSelectMenuHandler francoSelectMenuHandler;
    private final FrancoRandomHandler francoRandomHandler;

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
                case "seed_rsl" -> standardSeedButtonHandler.handle(interaction, SeedMode.RSL, "RSL");
                case "seed_pot" -> standardSeedButtonHandler.handle(interaction, SeedMode.POT, "PoT");
                case "seed_beginner" -> standardSeedButtonHandler.handle(interaction, SeedMode.BEGINNER, "Beginner");
                case "seed_rot" -> standardSeedButtonHandler.handle(interaction, SeedMode.ROT, "RoT");
                case "seed_s8" -> standardSeedButtonHandler.handle(interaction, SeedMode.S8, "S8");
                case "seed_s9" -> standardSeedButtonHandler.handle(interaction, SeedMode.S9, "S9");
                case "seed_tot" -> standardSeedButtonHandler.handle(interaction, SeedMode.TOT, "ToT");
                case "seed_mixed" -> standardSeedButtonHandler.handle(interaction, SeedMode.MIXED, "Mixed Pool S5");
                case "seed_allsanity_er_decoupled" -> standardSeedButtonHandler.handle(interaction, SeedMode.ALLSANITY_ER_DECOUPLED, "Allsanity + ER decoupled");
                case "seed_allsanity_er_noow" -> standardSeedButtonHandler.handle(interaction, SeedMode.ALLSANITY_ER_NOOW, "Allsanity + ER without OW");
                case "seed_allsanity_er" -> standardSeedButtonHandler.handle(interaction, SeedMode.ALLSANITY_ER, "Allsanity + ER");
                case "seed_allsanity_only" -> standardSeedButtonHandler.handle(interaction, SeedMode.ALLSANITY_ONLY, "Allsanity only");
                case "seed_salad_enemy" -> standardSeedButtonHandler.handle(interaction, SeedMode.SALAD_ENEMY, "Monstre en folie");
                case "seed_salad_nature" -> standardSeedButtonHandler.handle(interaction, SeedMode.SALAD_NATURE, "Nature en folie");
                case "seed_salad_rupee" -> standardSeedButtonHandler.handle(interaction, SeedMode.SALAD_RUPEES, "Rubis en folie");
                case "seed_salad_songs" -> standardSeedButtonHandler.handle(interaction, SeedMode.SALAD_SONGS, "Chansons en folie");
                case "seed_salad_dungeon" -> standardSeedButtonHandler.handle(interaction, SeedMode.SALAD_DUNGEONS, "Donjon en folie");
                case "seed_salad_mix" -> standardSeedButtonHandler.handle(interaction, SeedMode.SALAD_MIX, "Mélange en folie");
                case "seed_salad_all" -> standardSeedButtonHandler.handle(interaction, SeedMode.SALAD_ALL, "Salade complète");
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
