package fr.rawz06.rslfrancobot.engine.usecases.franco;

import fr.rawz06.rslfrancobot.engine.domain.entities.Preset;
import fr.rawz06.rslfrancobot.engine.domain.entities.SettingsFile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Use Case: Builds the final settings file for Franco mode.
 * Merges the base preset with user-selected options.
 */
@Component
public class BuildFinalSettingsUseCase {

    private static final String STARTING_INVENTORY_KEY = "starting_inventory";

    public SettingsFile execute(Preset preset, List<String> selectedOptionIds) {
        // Copy base settings
        Map<String, Object> finalSettings = new HashMap<>(preset.baseSettings());

        // If no options selected, return base settings
        if (selectedOptionIds == null || selectedOptionIds.isEmpty()) {
            return new SettingsFile(finalSettings);
        }

        // Apply selected options
        Map<String, Preset.PresetOption> optionsMap = preset.availableOptions().stream()
                .collect(java.util.stream.Collectors.toMap(
                        Preset.PresetOption::id,
                        option -> option
                ));

        List<String> baseStartingInventory = toStringList(preset.baseSettings().get(STARTING_INVENTORY_KEY));
        Set<String> addedInventoryItems = new LinkedHashSet<>();
        Set<String> removedInventoryItems = new LinkedHashSet<>();

        for (String selectedId : selectedOptionIds) {
            Preset.PresetOption option = optionsMap.get(selectedId);
            if (option == null || option.settingsToApply() == null) {
                continue;
            }

            for (Map.Entry<String, Object> entry : option.settingsToApply().entrySet()) {
                if (STARTING_INVENTORY_KEY.equals(entry.getKey())) {
                    // starting_inventory is special-cased: each option redeclares the whole
                    // list, so a plain overwrite makes the last-applied option silently
                    // undo another option's item toggle (e.g. "ocarina" vs "start_weird_egg").
                    // Instead, diff each option's list against the default inventory and
                    // accumulate additions/removals so every option's toggle sticks
                    // regardless of selection order.
                    List<String> optionInventory = toStringList(entry.getValue());
                    for (String item : baseStartingInventory) {
                        if (!optionInventory.contains(item)) {
                            removedInventoryItems.add(item);
                        }
                    }
                    for (String item : optionInventory) {
                        if (!baseStartingInventory.contains(item)) {
                            addedInventoryItems.add(item);
                        }
                    }
                } else {
                    finalSettings.put(entry.getKey(), entry.getValue());
                }
            }
        }

        if (!addedInventoryItems.isEmpty() || !removedInventoryItems.isEmpty()) {
            List<String> mergedInventory = new ArrayList<>(baseStartingInventory);
            mergedInventory.removeAll(removedInventoryItems);
            for (String item : addedInventoryItems) {
                if (!mergedInventory.contains(item)) {
                    mergedInventory.add(item);
                }
            }
            finalSettings.put(STARTING_INVENTORY_KEY, mergedInventory);
        }

        return new SettingsFile(finalSettings);
    }

    @SuppressWarnings("unchecked")
    private static List<String> toStringList(Object value) {
        if (value instanceof List<?> list) {
            return (List<String>) list;
        }
        return List.of();
    }
}
