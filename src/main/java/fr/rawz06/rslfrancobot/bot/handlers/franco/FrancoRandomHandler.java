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
     * Entry point for random option selection.
     * Shows buttons to select difficulty level (Easy or Hard).
     */
    public void handle(DiscordInteraction interaction) {
        DiscordMessage message = new DiscordMessage(
                "⚔️ **Select difficulty level for random Franco options**\n" +
                "• **Easy**: Only options tagged as 'easy' will be picked.\n" +
                "• **Hard**: Both 'easy' and 'hard' options can be picked."
        );

        message.addButton("Easy", "franco_level_easy", DiscordButton.Style.PRIMARY);
        message.addButton("Hard", "franco_level_hard", DiscordButton.Style.DANGER);

        message.setEphemeral(true);
        interaction.reply(message);
    }

    /**
     * Handles the level selection and shows buttons for number of options.
     */
    public void handleLevelSelection(DiscordInteraction interaction, String level) {
        DiscordMessage message = new DiscordMessage(
                "🎲 **How many random Franco options do you want (" + level + ")?**\n" +
                "(The bot will select compatible options for you)"
        );

        String prefix = "franco_random_" + level + "_";
        message.addButton("3 options", prefix + "3", DiscordButton.Style.PRIMARY);
        message.addButton("5 options", prefix + "5", DiscordButton.Style.PRIMARY);
        message.addButton("7 options", prefix + "7", DiscordButton.Style.PRIMARY);
        message.addButton("10 options", prefix + "10", DiscordButton.Style.PRIMARY);
        message.addButton("15 options", prefix + "15", DiscordButton.Style.SUCCESS);

        message.setEphemeral(true);
        interaction.reply(message);

        // Delete the original message (Level Selection or Options Selection) to keep channel clean
        interaction.deleteOriginalMessage();
    }

    /**
     * Handles the final random selection with specified count and level.
     */
    public void handleRandomSelection(DiscordInteraction interaction, int requestedCount, String level) {
        interaction.defer();

        try {
            if (requestedCount < 1 || requestedCount > 20) {
                interaction.editDeferredReply(presenter.presentError("Number must be between 1 and 20."));
                return;
            }

            // Get Franco preset and available options
            List<Preset.PresetOption> allOptions = seedService.getAvailableOptions("franco");

            // Filter options based on level
            List<Preset.PresetOption> filteredOptions;
            if ("easy".equalsIgnoreCase(level)) {
                filteredOptions = allOptions.stream()
                        .filter(o -> "easy".equalsIgnoreCase(o.level()))
                        .toList();
            } else {
                // "hard" level includes both easy and hard (as per request)
                filteredOptions = allOptions;
            }

            // Randomly select compatible options
            List<String> selectedOptions = selectRandomCompatibleOptions(filteredOptions, requestedCount);

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

            // Clear selections in case of error
            interaction.clearUserData("franco_selections");

            // Delete interaction messages to keep channel clean
            interaction.deleteOriginalMessage();
        } catch (SeedService.SeedGenerationException e) {
            // Clear selections in case of error
            interaction.clearUserData("franco_selections");

            interaction.editDeferredReply(presenter.presentError(e.getMessage()));

            // Delete trigger message only
            try {
                interaction.deleteTriggerMessage();
            } catch (Exception ex) {
                // Ignore
            }
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
