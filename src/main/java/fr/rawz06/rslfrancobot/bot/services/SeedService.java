package fr.rawz06.rslfrancobot.bot.services;

import fr.rawz06.rslfrancobot.engine.domain.entities.Preset;
import fr.rawz06.rslfrancobot.engine.domain.entities.SeedMode;
import fr.rawz06.rslfrancobot.engine.domain.entities.SeedRequest;
import fr.rawz06.rslfrancobot.engine.domain.entities.SeedResult;
import fr.rawz06.rslfrancobot.engine.domain.ports.PresetRepository;
import fr.rawz06.rslfrancobot.engine.usecases.external.GenerateExternalSeedUseCase;
import fr.rawz06.rslfrancobot.engine.usecases.franco.GenerateFrancoSeedUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Application service that coordinates domain use cases.
 * Entry point from Bot Layer to Engine Layer.
 * 
 * Routes seed generation to appropriate use case:
 * - Franco mode: Local processing with validation
 * - All other modes: Direct call to external API
 */
@Service
@RequiredArgsConstructor
public class SeedService {

    private final GenerateFrancoSeedUseCase generateFrancoSeedUseCase;
    private final GenerateExternalSeedUseCase generateExternalSeedUseCase;
    private final PresetRepository presetRepository;

    /**
     * Generates a seed according to the requested mode.
     * Franco mode uses local validation and settings construction.
     * All other modes pass directly to external API.
     */
    public SeedResult generateSeed(SeedMode mode, String userId, Map<String, String> userSettings) 
            throws SeedGenerationException {
        SeedRequest request = new SeedRequest(mode, userId, userSettings);

        try {
            return switch (mode) {
                case FRANCO -> generateFrancoSeedUseCase.execute(request);
                default -> generateExternalSeedUseCase.execute(request);
            };
        } catch (GenerateFrancoSeedUseCase.GenerationException | GenerateExternalSeedUseCase.GenerationException e) {
            throw new SeedGenerationException(e.getMessage(), e);
        }
    }

    /**
     * Retrieves available options for Franco preset.
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
