package fr.rawz06.rslfrancobot.engine.domain.ports;

import fr.rawz06.rslfrancobot.engine.domain.entities.Preset;

import java.util.Optional;

/**
 * Port for preset repository.
 * Provides access to preset definitions (Franco options).
 */
public interface PresetRepository {
    /**
     * Retrieves a preset by name.
     * @param name Preset name (e.g. "franco")
     * @return Preset if found, empty otherwise
     */
    Optional<Preset> getPreset(String name);
}
