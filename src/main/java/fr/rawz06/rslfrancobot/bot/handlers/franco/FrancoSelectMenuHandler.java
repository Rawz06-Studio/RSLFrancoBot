package fr.rawz06.rslfrancobot.bot.handlers.franco;

import fr.rawz06.rslfrancobot.bot.models.DiscordInteraction;
import fr.rawz06.rslfrancobot.bot.services.SeedService;
import fr.rawz06.rslfrancobot.engine.domain.entities.Preset;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Handler for selections in Franco menus.
 * Stores user selections.
 */
@Component
@RequiredArgsConstructor
public class FrancoSelectMenuHandler {

    private static final Logger logger = LoggerFactory.getLogger(FrancoSelectMenuHandler.class);
    private final SeedService seedService;

    public void handle(DiscordInteraction interaction) {
        List<String> selectedValues = interaction.getSelectedValues();
        String username = interaction.getUsername();
        String menuId = interaction.getCustomId();

        // Retrieve previous selections
        @SuppressWarnings("unchecked")
        List<String> existingSelections = (List<String>) interaction.getUserData("franco_selections", List.class);
        if (existingSelections == null) {
            existingSelections = new ArrayList<>();
        }

        // Identify which options belong to this menu
        List<Preset.PresetOption> allOptions = seedService.getAvailableOptions("franco");
        
        // Find which menu index this is (franco_options_0, franco_options_1, etc.)
        int menuIndex = Integer.parseInt(menuId.substring(menuId.lastIndexOf("_") + 1));
        int start = menuIndex * 25;
        int end = Math.min(start + 25, allOptions.size());
        
        List<Preset.PresetOption> menuOptions = allOptions.subList(start, end);
        Set<String> menuOptionIds = menuOptions.stream().map(Preset.PresetOption::id).collect(Collectors.toSet());

        List<String> updatedSelections = new ArrayList<>(existingSelections);

        // 1. Remove any previously selected options that are in THIS menu but were NOT in the current selection
        for (String optionId : menuOptionIds) {
            if (updatedSelections.contains(optionId) && !selectedValues.contains(optionId)) {
                updatedSelections.remove(optionId);
                logger.info("Setting deselected: {} by {}", optionId, username);
            }
        }

        // 2. Add new selections from THIS menu
        for (String value : selectedValues) {
            if (!updatedSelections.contains(value)) {
                updatedSelections.add(value);
                logger.info("Setting selected: {} by {}", value, username);
            }
        }

        // Store updated selections
        interaction.storeUserData("franco_selections", updatedSelections);

        // Silently acknowledge the interaction (no visible message)
        interaction.acknowledgeSelect();
    }
}
