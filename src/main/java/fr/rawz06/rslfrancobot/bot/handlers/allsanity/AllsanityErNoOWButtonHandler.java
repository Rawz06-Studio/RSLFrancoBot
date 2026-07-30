package fr.rawz06.rslfrancobot.bot.handlers.allsanity;

import fr.rawz06.rslfrancobot.bot.models.DiscordInteraction;
import fr.rawz06.rslfrancobot.bot.presenters.SeedPresenter;
import fr.rawz06.rslfrancobot.bot.services.SeedService;
import fr.rawz06.rslfrancobot.engine.domain.entities.SeedMode;
import fr.rawz06.rslfrancobot.engine.domain.entities.SeedResult;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Handler for the Allsanity + ER button.
 * Directly generates an Allsanity + ER seed with decouple_entrances set to false.
 */
@Component
public class AllsanityErNoOWButtonHandler {

    private final SeedService seedService;
    private final SeedPresenter presenter;

    public AllsanityErNoOWButtonHandler(SeedService seedService, SeedPresenter presenter) {
        this.seedService = seedService;
        this.presenter = presenter;
    }

    public void handle(DiscordInteraction interaction) {
        try {
            // Defer immediately as generation takes time
            interaction.defer();

            // Generate seed with modified settings (decouple_entrances: false)
            SeedResult result = seedService.generateSeed(
                    SeedMode.ALLSANITY_ER_NOOW,
                    interaction.getUserId(),
                    Map.of()
            );

            // Send final result as channel message (persists after cleanup)
            interaction.sendChannelMessage(presenter.presentSeedResult(result, "Allsanity + ER without OW", interaction.getUsername()));

            // Delete interaction messages to keep channel clean
            interaction.deleteOriginalMessage();
        } catch (SeedService.SeedGenerationException e) {
            interaction.editDeferredReply(presenter.presentError(e.getMessage()));
        }
    }
}
