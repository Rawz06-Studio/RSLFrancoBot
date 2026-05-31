package fr.rawz06.rslfrancobot.web;

import fr.rawz06.rslfrancobot.bot.services.SeedService;
import fr.rawz06.rslfrancobot.engine.domain.entities.Preset;
import fr.rawz06.rslfrancobot.engine.domain.entities.SeedMode;
import fr.rawz06.rslfrancobot.engine.domain.entities.SeedResult;
import fr.rawz06.rslfrancobot.engine.domain.entities.ValidationResult;
import fr.rawz06.rslfrancobot.engine.domain.ports.PresetRepository;
import fr.rawz06.rslfrancobot.engine.usecases.franco.ValidateSettingsUseCase;
import fr.rawz06.rslfrancobot.web.dto.FrancoSeedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/generate")
@RequiredArgsConstructor
@SuppressWarnings("all")
public class SeedRestController {

    private final SeedService seedService;
    private final ValidateSettingsUseCase validateSettingsUseCase;
    private final PresetRepository presetRepository;

    @GetMapping("/franco")
    public FrancoSeedResponse generateFranco(@RequestParam(defaultValue = "5") int count, @RequestParam(required = false, defaultValue = "api-user") String userId) {
        Preset preset = presetRepository.getPreset("franco")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Franco preset not found"));

        List<Preset.PresetOption> availableOptions = new ArrayList<>(preset.availableOptions());
        Collections.shuffle(availableOptions);

        List<Preset.PresetOption> selectedOptions = new ArrayList<>();
        for (Preset.PresetOption candidate : availableOptions) {
            if (selectedOptions.size() >= count) {
                break;
            }

            // Try adding this option
            List<String> testSelectionIds = new ArrayList<>(selectedOptions.stream().map(Preset.PresetOption::id).toList());
            testSelectionIds.add(candidate.id());

            // Validate compatibility using existing Use Case
            ValidationResult validation = validateSettingsUseCase.execute(preset, testSelectionIds);

            if (validation.isValid()) {
                selectedOptions.add(candidate);
            }
        }

        Map<String, String> userSettings = selectedOptions.stream()
                .collect(Collectors.toMap(Preset.PresetOption::id, o -> "true"));

        try {
            SeedResult seedResult = seedService.generateSeed(SeedMode.FRANCO, userId, userSettings);
            return new FrancoSeedResponse(seedResult, selectedOptions);
        } catch (SeedService.SeedGenerationException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error during franco seed generation: " + e.getMessage(), e);
        }
    }

    @GetMapping("/{modeId}")
    public SeedResult generateSeed(@PathVariable String modeId, @RequestParam(required = false, defaultValue = "api-user") String userId) {
        SeedMode mode = SeedModeAPI.fromApiId(modeId)
                .map(SeedModeAPI::getSeedMode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mode not found or not supported via API: " + modeId));

        try {
            return seedService.generateSeed(mode, userId, Map.of());
        } catch (SeedService.SeedGenerationException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error during seed generation: " + e.getMessage(), e);
        }
    }
}
