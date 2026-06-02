package fr.rawz06.rslfrancobot.engine.domain.entities;

import java.util.List;
import java.util.Map;

/**
 * Represents a configuration preset.
 * Contains base settings and user-configurable options.
 */
public record Preset(
        String name,
        Map<String, Object> baseSettings,
        List<PresetOption> availableOptions
) {
    public Preset {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Preset name cannot be empty");
        }
        if (baseSettings == null) {
            throw new IllegalArgumentException("Base settings cannot be null");
        }
    }

    /**
     * Configurable option within a preset.
     */
    public record PresetOption(
            String id,
            String label,
            String description,
            String level,
            Map<String, Object> settingsToApply,
            List<String> incompatibleWith
    ) {
        public PresetOption {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("Option ID cannot be empty");
            }
            if (label == null || label.isBlank()) {
                throw new IllegalArgumentException("Label cannot be empty");
            }
        }
    }
}
