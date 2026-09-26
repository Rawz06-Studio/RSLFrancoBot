package fr.rawz06.rslfrancobot.engine.usecases.external;

import fr.rawz06.rslfrancobot.engine.domain.entities.SeedRequest;
import fr.rawz06.rslfrancobot.engine.domain.entities.SeedResult;
import fr.rawz06.rslfrancobot.engine.domain.entities.SettingsFile;
import fr.rawz06.rslfrancobot.engine.domain.ports.RandomizerApi;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Use Case: Generates a seed via external API.
 * Used for all modes except Franco (S8, S9, RSL, Salad, etc.)
 * Settings are passed directly to the external API without any local processing.
 */
@Component
public class GenerateExternalSeedUseCase {

    private final RandomizerApi randomizerApi;

    public GenerateExternalSeedUseCase(
            RandomizerApi randomizerApi
    ) {
        this.randomizerApi = randomizerApi;
    }

    /**
     * Generates a seed via the external API.
     * The external API handles all seed generation logic.
     * @param request Contains mode and settings
     * @return Result containing seed URL and metadata
     */
    public SeedResult execute(SeedRequest request) throws GenerationException {
        try {
            Map<String, Object> settings = new HashMap<>(request.userSettings());
            SettingsFile settingsFile = new SettingsFile(settings);
            return randomizerApi.generateSeed(request.mode(), settingsFile);
        } catch (RandomizerApi.RandomizerApiException e) {
            throw new GenerationException("Error during seed generation via external API", e);
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
