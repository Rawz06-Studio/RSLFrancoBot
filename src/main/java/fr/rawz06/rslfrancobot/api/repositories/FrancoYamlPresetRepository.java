package fr.rawz06.rslfrancobot.api.repositories;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import fr.rawz06.rslfrancobot.engine.domain.entities.Preset;
import fr.rawz06.rslfrancobot.engine.domain.ports.PresetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

/**
 * Repository implementation that loads Franco preset from YAML file.
 * Reads franco.yaml and converts it into Preset domain entity.
 * Used by Discord Bot to load available Franco options.
 */
@Component
public class FrancoYamlPresetRepository implements PresetRepository {

    private static final Logger logger = LoggerFactory.getLogger(FrancoYamlPresetRepository.class);
    private static final String FRANCO_YAML_PATH = "data/franco.yaml";

    private final ObjectMapper yamlMapper;
    private Preset francoPreset;

    public FrancoYamlPresetRepository() {
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
        this.francoPreset = loadFrancoPreset();
    }

    @Override
    public Optional<Preset> getPreset(String name) {
        if ("franco".equalsIgnoreCase(name)) {
            return Optional.of(francoPreset);
        }
        return Optional.empty();
    }

    private Preset loadFrancoPreset() {
        try {
            ClassPathResource yamlResource = new ClassPathResource(FRANCO_YAML_PATH);
            Map<String, Map<String, Object>> yamlContent = yamlMapper.readValue(
                    yamlResource.getInputStream(),
                    Map.class
            );

            List<Preset.PresetOption> options = parseOptions(yamlContent);
            Map<String, Object> baseSettings = new HashMap<>();

            logger.info("Loaded Franco preset with {} options", options.size());
            return new Preset("franco", baseSettings, options);

        } catch (IOException e) {
            logger.error("Failed to load Franco preset from {}", FRANCO_YAML_PATH, e);
            throw new RuntimeException("Failed to load Franco preset", e);
        }
    }

    private List<Preset.PresetOption> parseOptions(Map<String, Map<String, Object>> yamlContent) {
        List<Preset.PresetOption> options = new ArrayList<>();

        for (Map.Entry<String, Map<String, Object>> entry : yamlContent.entrySet()) {
            String optionId = entry.getKey();
            Map<String, Object> optionData = entry.getValue();

            String label = (String) optionData.get("label");
            String description = (String) optionData.get("description");
            String level = (String) optionData.getOrDefault("level", "normal");

            @SuppressWarnings("unchecked")
            Map<String, Object> settings = (Map<String, Object>) optionData.get("settings");

            @SuppressWarnings("unchecked")
            List<String> incompatibilities = (List<String>) optionData.get("incompatibilities");
            if (incompatibilities == null) {
                incompatibilities = List.of();
            }

            Preset.PresetOption option = new Preset.PresetOption(
                    optionId,
                    label,
                    description,
                    level,
                    settings != null ? new HashMap<>(settings) : Map.of(),
                    incompatibilities
            );

            options.add(option);
        }

        return options;
    }
}

