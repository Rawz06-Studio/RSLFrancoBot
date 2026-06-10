package fr.rawz06.rslfrancobot.engine.usecases.rsl;

import fr.rawz06.rslfrancobot.engine.domain.entities.*;
import fr.rawz06.rslfrancobot.engine.domain.ports.PresetRepository;
import fr.rawz06.rslfrancobot.engine.domain.ports.RandomizerApi;
import fr.rawz06.rslfrancobot.engine.domain.ports.RSLScriptRunner;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Use Case: Generates a seed in RSL, PoT, or Beginner mode.
 * Uses the Python script to generate random settings, then generates the seed.
 */
@Component
public class GenerateRSLSeedUseCase {

    private final PresetRepository presetRepository;
    private final RSLScriptRunner rslScriptRunner;
    private final RandomizerApi randomizerApi;

    public GenerateRSLSeedUseCase(
            PresetRepository presetRepository,
            RSLScriptRunner rslScriptRunner,
            RandomizerApi randomizerApi
    ) {
        this.presetRepository = presetRepository;
        this.rslScriptRunner = rslScriptRunner;
        this.randomizerApi = randomizerApi;
    }

    public SeedResult execute(SeedRequest request) throws GenerationException {
        // Determine preset name based on mode
        String presetName = switch (request.mode()) {
            case RSL -> "rsl";
            case POT -> "pot";
            case ROT -> "rot";
            case BEGINNER -> "beginner";
            default -> throw new GenerationException("Unsupported mode for RSL/PoT/Beginner: " + request.mode());
        };

        // 1. Retrieve preset
        Preset preset = presetRepository.getPreset(presetName)
                .orElseThrow(() -> new GenerationException("Preset " + presetName + " not found"));

        // 2. Generate settings via Python script
        SettingsFile generatedSettings;
        try {
            generatedSettings = rslScriptRunner.generateSettings(preset);
        } catch (RSLScriptRunner.ScriptExecutionException e) {
            throw new GenerationException("Error executing RSL script", e);
        }

        // 2.5. Add hardcoded settings for RSL modes
        // Python script puts everything inside a "settings" key, we need to flatten it
        Map<String, Object> pythonOutput = generatedSettings.settings();

        // Extract nested settings and put everything at top level
        @SuppressWarnings("unchecked")
        Map<String, Object> flatSettings = new HashMap<>((Map<String, Object>) pythonOutput.get("settings"));

        // Add hardcoded settings at top level
        flatSettings.put("show_seed_info", true);
        flatSettings.put("create_spoiler", true);
        flatSettings.put("password_lock", false);

        SettingsFile finalSettings = new SettingsFile(flatSettings);

        // 3. Generate seed via API
        try {
            return randomizerApi.generateSeed(request.mode(), finalSettings);
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
