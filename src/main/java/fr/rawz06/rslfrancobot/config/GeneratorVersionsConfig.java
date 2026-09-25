package fr.rawz06.rslfrancobot.config;

import fr.rawz06.rslfrancobot.engine.domain.entities.SeedMode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration containing generator versions for each seed mode.
 * Populated from properties in format: app.version.{seedmode}=value
 */
@Service
public class GeneratorVersionsConfig {

    private final Map<SeedMode, String> versions = new HashMap<>();

    public GeneratorVersionsConfig() {
        initializeVersions();
    }

    private void initializeVersions() {
        // Classic Versions
        versions.put(SeedMode.S8, "8.3.0");
        versions.put(SeedMode.S9, "9.0.0");
        versions.put(SeedMode.TOT, "9.0.0");
        versions.put(SeedMode.MIXED, "devFenhl_9.0.42-2");
        versions.put(SeedMode.FRANCO, "devrreal_9.0.2-17");

        // RSL Versions
        versions.put(SeedMode.RSL, "devRSL_9.0.2-17");
        versions.put(SeedMode.POT, "devRSL_9.0.2-17");
        versions.put(SeedMode.BEGINNER, "devRSL_9.0.2-17");
        versions.put(SeedMode.ROT, "devRSL_9.0.2-17");

        // Salad Versions
        versions.put(SeedMode.SALAD_ENEMY, "devEnemyShuffle_9.0.2-24");
        versions.put(SeedMode.SALAD_RUPEES, "devrreal_9.0.2-17");
        versions.put(SeedMode.SALAD_DUNGEONS, "devrreal_9.0.2-17");
        versions.put(SeedMode.SALAD_SONGS, "devrreal_9.0.2-17");
        versions.put(SeedMode.SALAD_MIX, "devrreal_9.0.2-17");
        versions.put(SeedMode.SALAD_NATURE, "devrreal_9.0.2-17");
        versions.put(SeedMode.SALAD_ALL, "devrreal_9.0.2-17");

        // All Sanity
        versions.put(SeedMode.ALLSANITY_ER_DECOUPLED, "devEnemyShuffle_9.0.2-24");
        versions.put(SeedMode.ALLSANITY_ER, "devEnemyShuffle_9.0.2-24");
        versions.put(SeedMode.ALLSANITY_ER_NOOW, "devEnemyShuffle_9.0.2-24");
        versions.put(SeedMode.ALLSANITY_ONLY, "devEnemyShuffle_9.0.2-24");
    }

    /**
     * Get the generator version for a specific seed mode.
     */
    public String getVersion(SeedMode mode) {
        return versions.getOrDefault(mode, "unknown");
    }

    /**
     * Get all versions as a map (for /api/info endpoint).
     */
    public Map<SeedMode, String> getAllVersions() {
        return new HashMap<>(versions);
    }
}
