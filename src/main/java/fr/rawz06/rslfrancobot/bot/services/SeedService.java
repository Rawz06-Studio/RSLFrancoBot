package fr.rawz06.rslfrancobot.bot.services;

import fr.rawz06.rslfrancobot.engine.domain.entities.Preset;
import fr.rawz06.rslfrancobot.engine.domain.entities.SeedMode;
import fr.rawz06.rslfrancobot.engine.domain.entities.SeedRequest;
import fr.rawz06.rslfrancobot.engine.domain.entities.SeedResult;
import fr.rawz06.rslfrancobot.engine.domain.ports.PresetRepository;
import fr.rawz06.rslfrancobot.engine.usecases.allsanity.GenerateAllsanitySeedUseCase;
import fr.rawz06.rslfrancobot.engine.usecases.franco.GenerateFrancoSeedUseCase;
import fr.rawz06.rslfrancobot.engine.usecases.rsl.GenerateRSLSeedUseCase;
import fr.rawz06.rslfrancobot.engine.usecases.mixed.GenerateMixedSeedUseCase;
import fr.rawz06.rslfrancobot.engine.usecases.s8.GenerateS8SeedUseCase;
import fr.rawz06.rslfrancobot.engine.usecases.s9.GenerateS9SeedUseCase;
import fr.rawz06.rslfrancobot.engine.usecases.salad.GenerateSaladSeedUseCase;
import fr.rawz06.rslfrancobot.engine.usecases.tot.GenerateTotSeedUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Application service that coordinates domain use cases.
 * Entry point from Bot Layer to Engine Layer.
 */
@Service
@RequiredArgsConstructor
public class SeedService {

    private final GenerateFrancoSeedUseCase generateFrancoSeedUseCase;
    private final GenerateRSLSeedUseCase generateRSLSeedUseCase;
    private final GenerateS8SeedUseCase generateS8SeedUseCase;
    private final GenerateS9SeedUseCase generateS9SeedUseCase;
    private final GenerateAllsanitySeedUseCase generateAllsanitySeedUseCase;
    private final GenerateSaladSeedUseCase generateSaladSeedUseCase;
    private final PresetRepository presetRepository;
    private final GenerateTotSeedUseCase generateTotSeedUseCase;
    private final GenerateMixedSeedUseCase generateMixedSeedUseCase;

    /**
     * Generates a seed according to the requested mode.
     */
    public SeedResult generateSeed(SeedMode mode, String userId, Map<String, String> userSettings) throws SeedGenerationException {
        SeedRequest request = new SeedRequest(mode, userId, userSettings);

        try {
            return switch (mode) {
                case FRANCO -> generateFrancoSeedUseCase.execute(request);
                case RSL, POT, BEGINNER, ROT -> generateRSLSeedUseCase.execute(request);
                case S8 -> generateS8SeedUseCase.execute(request);
                case S9 -> generateS9SeedUseCase.execute(request);
                case ALLSANITY_ER_DECOUPLED, ALLSANITY_ER, ALLSANITY_ONLY, ALLSANITY_ER_NOOW -> generateAllsanitySeedUseCase.execute(request);
                case SALAD_NATURE, SALAD_ENEMY, SALAD_RUPEES, SALAD_DUNGEONS, SALAD_SONGS, SALAD_MIX, SALAD_ALL -> generateSaladSeedUseCase.execute(request);
                case TOT -> generateTotSeedUseCase.execute(request);
                case MIXED -> generateMixedSeedUseCase.execute(request);
            };
        } catch (GenerateFrancoSeedUseCase.GenerationException | GenerateRSLSeedUseCase.GenerationException |
                 GenerateS8SeedUseCase.GenerationException | GenerateS9SeedUseCase.GenerationException |
                 GenerateAllsanitySeedUseCase.GenerationException | GenerateSaladSeedUseCase.GenerationException |
                 GenerateTotSeedUseCase.GenerationException | GenerateMixedSeedUseCase.GenerationException e) {
            throw new SeedGenerationException(e.getMessage(), e);
        }
    }

    /**
     * Retrieves available options for a given preset.
     */
    public List<Preset.PresetOption> getAvailableOptions(String presetName) {
        return presetRepository.getPreset(presetName)
                .map(Preset::availableOptions)
                .orElseThrow(() -> new IllegalArgumentException("Preset not found: " + presetName));
    }

    public static class SeedGenerationException extends Exception {
        public SeedGenerationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
