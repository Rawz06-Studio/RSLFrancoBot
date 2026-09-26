package fr.rawz06.rslfrancobot.bot.handlers;

import fr.rawz06.rslfrancobot.bot.models.DiscordInteraction;
import fr.rawz06.rslfrancobot.bot.presenters.SeedPresenter;
import fr.rawz06.rslfrancobot.bot.services.SeedService;
import fr.rawz06.rslfrancobot.engine.domain.entities.SeedMode;
import fr.rawz06.rslfrancobot.engine.domain.entities.SeedResult;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Generic handler for standard seed generation modes (S8, S9, RSL, etc.).
 * Handles the common pattern: defer → generate → sendChannelMessage → delete original.
 */
@Component
public class StandardSeedButtonHandler {

    private final SeedService seedService;
    private final SeedPresenter presenter;

    public StandardSeedButtonHandler(SeedService seedService, SeedPresenter presenter) {
        this.seedService = seedService;
        this.presenter = presenter;
    }

    /**
     * Handles standard seed generation for any mode.
     * @param interaction Discord interaction
     * @param mode Seed generation mode
     * @param displayName User-friendly name for the result message
     */
    public void handle(DiscordInteraction interaction, SeedMode mode, String displayName) {
        try {
            // Defer immediately as generation takes time
            interaction.defer();

            // Generate seed with no user-specific settings
            SeedResult result = seedService.generateSeed(
                    mode,
                    interaction.getUserId(),
                    Map.of()
            );

            // Send final result as channel message (persists after cleanup)
            interaction.sendChannelMessage(presenter.presentSeedResult(result, displayName, interaction.getUsername()));

            // Delete interaction messages to keep channel clean
            interaction.deleteOriginalMessage();
        } catch (SeedService.SeedGenerationException e) {
            interaction.editDeferredReply(presenter.presentError(e.getMessage()));
        }
    }
}
