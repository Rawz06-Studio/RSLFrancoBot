package fr.rawz06.rslfrancobot.engine.domain.entities;

import java.util.List;
import java.util.Map;

/**
 * Represents a preset with base settings and available options.
 * Used to load Franco preset from YAML configuration.
 */
public record Preset(
        String name,
        Map<String, Object> baseSettings,
        List<PresetOption> availableOptions
) {
    public record PresetOption(
            String id,
            String label,
            String description,
            String level,
            Map<String, Object> settingsToApply,
            List<String> incompatibleWith
    ) {}
}
