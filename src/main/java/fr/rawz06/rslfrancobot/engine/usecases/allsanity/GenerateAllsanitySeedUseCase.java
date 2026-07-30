package fr.rawz06.rslfrancobot.engine.usecases.allsanity;

import fr.rawz06.rslfrancobot.engine.domain.entities.*;
import fr.rawz06.rslfrancobot.engine.domain.ports.PresetRepository;
import fr.rawz06.rslfrancobot.engine.domain.ports.RandomizerApi;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Use Case: Generates a seed in Allsanity modes.
 * Three variants:
 * - ALLSANITY_ER_DECOUPLED: Uses allsanity.json as-is
 * - ALLSANITY_ER: Sets decouple_entrances to false
 * - ALLSANITY_ONLY: Disables all ER settings
 */
@Component
public class GenerateAllsanitySeedUseCase {

    private final PresetRepository presetRepository;
    private final RandomizerApi randomizerApi;

    public GenerateAllsanitySeedUseCase(
            PresetRepository presetRepository,
            RandomizerApi randomizerApi
    ) {
        this.presetRepository = presetRepository;
        this.randomizerApi = randomizerApi;
    }

    public SeedResult execute(SeedRequest request) throws GenerationException {
        // 1. Retrieve Allsanity preset (contains fixed settings from allsanity.json)
        Preset allsanityPreset = presetRepository.getPreset("allsanity")
                .orElseThrow(() -> new GenerationException("Allsanity preset not found"));

        // 2. Create a mutable copy of base settings
        Map<String, Object> settings = new HashMap<>(allsanityPreset.baseSettings());

        // 3. Apply mode-specific modifications
        switch (request.mode()) {
            case ALLSANITY_ER_DECOUPLED:
                // Use settings as-is (decouple_entrances is already true)
                break;

            case ALLSANITY_ER:
                // Set decouple_entrances to false
                settings.put("decouple_entrances", false);
                break;

            case ALLSANITY_ER_NOOW:
                // Set decouple_entrances and shuffle_overworld_entrances to false
                settings.put("decouple_entrances", false);
                settings.put("shuffle_overworld_entrances", false);
                settings.put("shuffle_gerudo_valley_river_exit", false);
                settings.put("shuffle_gerudo_fortress_heart_piece", "remove");
                settings.put("shuffle_hideout_entrances", false);
                break;

            case ALLSANITY_ONLY:
                // Disable all ER settings
                settings.put("decouple_entrances", false);
                settings.put("shuffle_gerudo_valley_river_exit", false);
                settings.put("owl_drops", false);
                settings.put("warp_songs", false);
                settings.put("shuffle_overworld_entrances", false);
                settings.put("shuffle_ganon_tower", false);
                settings.put("shuffle_grotto_entrances", false);
                settings.put("shuffle_hideout_entrances", false);
                settings.put("shuffle_interior_entrances", false);
                settings.put("shuffle_dungeon_entrances", "off");
                settings.put("shuffle_bosses", "off");
                settings.put("shuffle_gerudo_fortress_heart_piece", "remove");
                settings.put("mix_entrance_pools", new ArrayList<>());
                break;

            default:
                throw new GenerationException("Unsupported Allsanity mode: " + request.mode());
        }

        // 4. Create SettingsFile from modified settings
        SettingsFile settingsFile = new SettingsFile(settings);

        // 5. Generate seed via API
        try {
            return randomizerApi.generateSeed(request.mode(), settingsFile);
        } catch (RandomizerApi.RandomizerApiException e) {
            throw new GenerationException("Error during seed generation", e);
        }
    }

    public static class GenerationException extends Exception {
        public GenerationException(String message) {
            super(message);
        }

        public GenerationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
