package fr.rawz06.rslfrancobot.bot.handlers.franco;

import fr.rawz06.rslfrancobot.bot.models.DiscordInteraction;
import fr.rawz06.rslfrancobot.bot.presenters.SeedPresenter;
import fr.rawz06.rslfrancobot.bot.services.SeedService;
import fr.rawz06.rslfrancobot.engine.domain.entities.Preset;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Handler for the Franco button.
 * Displays the Franco options selection menu.
 */
@Component
public class FrancoButtonHandler {

    private final SeedService seedService;
    private final SeedPresenter presenter;

    public FrancoButtonHandler(SeedService seedService, SeedPresenter presenter) {
        this.seedService = seedService;
        this.presenter = presenter;
    }

    public void handle(DiscordInteraction interaction) {
        try {
            // Clear previous selections to start fresh
            interaction.clearUserData("franco_selections");

            List<Preset.PresetOption> options = seedService.getAvailableOptions("franco");
            interaction.reply(presenter.presentFrancoOptions(options));

            // Delete the mode selection message to keep channel clean
            interaction.deleteOriginalMessage();
        } catch (Exception e) {
            interaction.reply(presenter.presentError("Unable to load Franco options: " + e.getMessage()));
        }
    }
}
