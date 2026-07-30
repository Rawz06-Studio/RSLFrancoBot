package fr.rawz06.rslfrancobot.api.randomizer;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.rawz06.rslfrancobot.config.AppVersionConfig;
import fr.rawz06.rslfrancobot.engine.domain.entities.SeedMode;
import fr.rawz06.rslfrancobot.engine.domain.entities.SeedResult;
import fr.rawz06.rslfrancobot.engine.domain.entities.SettingsFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Service containing all the business logic for Randomizer API operations.
 * This service is shared between HTTP and Mock implementations to ensure
 * they both follow the same logic (version selection, URL building, etc.)
 */
@Service
public class RandomizerApiService {

    private static final Logger logger = LoggerFactory.getLogger(RandomizerApiService.class);

    @Value("${app.randomizer.api.url}")
    private String apiUrl;

    @Value("${app.randomizer.api.key}")
    private String apiKey;

    private final AppVersionConfig versionConfig;
    private final ObjectMapper objectMapper;

    public RandomizerApiService(ObjectMapper objectMapper, AppVersionConfig versionConfig) {
        this.objectMapper = objectMapper;
        this.versionConfig = versionConfig;
    }

    /**
     * Determines the version to use based on the seed mode.
     */
    public String getVersionForMode(SeedMode mode) {
        return switch (mode) {
            case FRANCO -> versionConfig.getFranco();
            case RSL, POT, BEGINNER, ROT -> versionConfig.getRsl();
            case S8 -> versionConfig.getS8();
            case S9 -> versionConfig.getS9();
            case ALLSANITY_ER_DECOUPLED, ALLSANITY_ER, ALLSANITY_ONLY, ALLSANITY_ER_NOOW -> versionConfig.getAllsanity();
            case SALAD_NATURE, SALAD_RUPEES, SALAD_DUNGEONS, SALAD_SONGS, SALAD_MIX, SALAD_ALL -> versionConfig.getSalad();
            case TOT -> versionConfig.getTot();
            case MIXED -> versionConfig.getMixed();
            case SALAD_ENEMY -> versionConfig.getEnemySalad();
        };
    }

    /**
     * Builds the full API URL with query parameters (key and version).
     */
    public String buildApiUrl(String version) {
        return UriComponentsBuilder.fromUriString(apiUrl)
                .queryParam("key", apiKey)
                .queryParam("version", version)
                .toUriString();
    }

    /**
     * Logs the settings being sent to the API in pretty-printed JSON format.
     * Uses DEBUG level to avoid flooding logs in production.
     */
    public void logSettings(SeedMode mode, String version, SettingsFile settings) {
        try {
            String jsonSettings = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(settings.settings());
            logger.debug("Sending settings to randomizer API for mode {}, version {}:\n{}", mode, version, jsonSettings);
        } catch (Exception e) {
            logger.error("Error serializing settings for logging", e);
        }
    }

    /**
     * Builds the seed URL from the API response ID.
     */
    public String buildSeedUrl(String seedId) {
        return "https://ootrandomizer.com/seed/get?id=" + seedId;
    }

    /**
     * Constructs a SeedResult from the API response.
     */
    public SeedResult buildSeedResult(ApiResponse apiResponse, SettingsFile settings) {
        String seedUrl = buildSeedUrl(apiResponse.id);

        logger.info("Seed created successfully: id={}, version={}, spoilers={}",
                apiResponse.id, apiResponse.version, apiResponse.spoilers);

        return new SeedResult(
                seedUrl,
                apiResponse.version,
                apiResponse.spoilers,
                settings
        );
    }

    /**
     * DTO representing the API response structure
     */
    public static class ApiResponse {
        public String id;
        public String version;
        public Boolean spoilers;

        // For mock construction
        public ApiResponse() {}

        public ApiResponse(String id, String version, Boolean spoilers) {
            this.id = id;
            this.version = version;
            this.spoilers = spoilers;
        }
    }
}
