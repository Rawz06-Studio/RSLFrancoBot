package fr.rawz06.rslfrancobot.bot.handlers.franco;

import fr.rawz06.rslfrancobot.bot.models.DiscordButton;
import fr.rawz06.rslfrancobot.bot.models.DiscordInteraction;
import fr.rawz06.rslfrancobot.bot.models.DiscordMessage;
import fr.rawz06.rslfrancobot.bot.presenters.SeedPresenter;
import fr.rawz06.rslfrancobot.bot.services.SeedService;
import fr.rawz06.rslfrancobot.engine.domain.entities.Preset;
import fr.rawz06.rslfrancobot.engine.domain.entities.SeedMode;
import fr.rawz06.rslfrancobot.engine.domain.entities.SeedResult;
import fr.rawz06.rslfrancobot.engine.domain.entities.ValidationResult;
import fr.rawz06.rslfrancobot.engine.usecases.franco.ValidateSettingsUseCase;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Handler for Franco random option selection.
 * Randomly selects a specified number of compatible options and generates a seed.
 */
@Component
public class FrancoRandomHandler {

    private final SeedService seedService;
    private final SeedPresenter presenter;
    private final ValidateSettingsUseCase validateSettingsUseCase;

    public FrancoRandomHandler(SeedService seedService, SeedPresenter presenter, ValidateSettingsUseCase validateSettingsUseCase) {
        this.seedService = seedService;
        this.presenter = presenter;
        this.validateSettingsUseCase = validateSettingsUseCase;
    }

    /**
     * Shows buttons to select number of random options.
     */
    public void showNumberSelection(DiscordInteraction interaction) {
        DiscordMessage message = new DiscordMessage(
                "🎲 **How many random Franco options do you want?**\n" +
                "(The bot will select compatible options for you)"
        );

        // Add buttons for different counts
        message.addButton("3 options", "franco_random_3", DiscordButton.Style.PRIMARY);
        message.addButton("5 options", "franco_random_5", DiscordButton.Style.PRIMARY);
        message.addButton("7 options", "franco_random_7", DiscordButton.Style.PRIMARY);
        message.addButton("10 options", "franco_random_10", DiscordButton.Style.PRIMARY);
        message.addButton("15 options", "franco_random_15", DiscordButton.Style.SUCCESS);

        message.setEphemeral(true);
        interaction.reply(message);

        // Delete the Franco options selection message
        interaction.deleteOriginalMessage();
    }

    /**
     * Handles random selection with specified count.
     * Randomly selects compatible options and generates a seed.
     */
    public void handleWithCount(DiscordInteraction interaction, int requestedCount) {
        interaction.defer();

        try {
            if (requestedCount < 1 || requestedCount > 20) {
                interaction.editDeferredReply(presenter.presentError("Number must be between 1 and 20."));
                return;
            }

            // Get Franco preset and available options
            List<Preset.PresetOption> allOptions = seedService.getAvailableOptions("franco");

            // Randomly select compatible options
            List<String> selectedOptions = selectRandomCompatibleOptions(allOptions, requestedCount);

            // Convert to Map for SeedService
            Map<String, String> userSettings = new HashMap<>();
            for (String optionId : selectedOptions) {
                userSettings.put(optionId, "true");
            }

            // Generate seed
            SeedResult result = seedService.generateSeed(
                    SeedMode.FRANCO,
                    interaction.getUserId(),
                    userSettings
            );

            // Send final result as channel message (persists after cleanup)
            interaction.sendChannelMessage(presenter.presentFrancoSeedResult(result, selectedOptions, interaction.getUsername()));

            // Delete interaction messages to keep channel clean
            interaction.deleteOriginalMessage();
        } catch (SeedService.SeedGenerationException e) {
            interaction.editDeferredReply(presenter.presentError(e.getMessage()));
        }
    }

    /**
     * Randomly selects compatible options from the available options.
     * Ensures selected options are compatible with each other.
     */
    private List<String> selectRandomCompatibleOptions(List<Preset.PresetOption> allOptions, int count) {
        List<String> selected = new ArrayList<>();
        List<Preset.PresetOption> availableOptions = new ArrayList<>(allOptions);
        Random random = new Random();

        // Get Franco preset for validation
        Preset francoPreset;
        try {
            francoPreset = seedService.getAvailableOptions("franco").isEmpty()
                ? null
                : new Preset("franco", Map.of(), allOptions);
        } catch (Exception e) {
            // Fallback: just randomly select without validation
            Collections.shuffle(availableOptions);
            return availableOptions.stream()
                    .limit(Math.min(count, availableOptions.size()))
                    .map(Preset.PresetOption::id)
                    .toList();
        }

        int attempts = 0;
        int maxAttempts = count * 100; // Prevent infinite loops

        while (selected.size() < Math.min(count, availableOptions.size()) && attempts < maxAttempts) {
            attempts++;

            if (availableOptions.isEmpty()) {
                break;
            }

            // Pick a random option
            int randomIndex = random.nextInt(availableOptions.size());
            Preset.PresetOption candidate = availableOptions.get(randomIndex);

            // Try adding this option
            List<String> testSelection = new ArrayList<>(selected);
            testSelection.add(candidate.id());

            // Validate compatibility
            ValidationResult validation = validateSettingsUseCase.execute(francoPreset, testSelection);

            if (validation.isValid()) {
                // Option is compatible, add it
                selected.add(candidate.id());
                availableOptions.remove(randomIndex);
            } else {
                // Option is incompatible, remove it from candidates
                availableOptions.remove(randomIndex);
            }
        }

        return selected;
    }
}
