package fr.rawz06.rslfrancobot.api.randomizer;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.rawz06.rslfrancobot.engine.domain.entities.SeedMode;
import fr.rawz06.rslfrancobot.engine.domain.entities.SeedResult;
import fr.rawz06.rslfrancobot.engine.domain.entities.SettingsFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Service containing all the business logic for Seed Generation API operations.
 * Handles communication with the external seed generation API.
 * This service is shared between HTTP and Mock implementations.
 */
@Service
public class RandomizerApiService {

    private static final Logger logger = LoggerFactory.getLogger(RandomizerApiService.class);

    @Value("${app.seed.api.domain}")
    private String apiDomain;

    private final ObjectMapper objectMapper;

    public RandomizerApiService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Builds the full API URL for a specific seed mode.
     * Format: {apiDomain}/api/seed/{mode}
     */
    public String buildApiUrl(SeedMode mode, SettingsFile settings) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(apiDomain)
                .path("/api/seed/")
                .path(mode.name());

        // Add all settings as query parameters
        for (var entry : settings.settings().entrySet()) {
            builder.queryParam(entry.getKey(), entry.getValue());
        }

        return builder.toUriString();
    }

    /**
     * Logs the settings being sent to the API in pretty-printed JSON format.
     * Uses DEBUG level to avoid flooding logs in production.
     */
    public void logSettings(SeedMode mode, SettingsFile settings) {
        try {
            String jsonSettings = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(settings.settings());
            logger.debug("Sending settings to seed generation API for mode {}:\n{}", mode, jsonSettings);
        } catch (Exception e) {
            logger.error("Error serializing settings for logging", e);
        }
    }

    /**
     * Constructs a SeedResult from the API response.
     */
    public SeedResult buildSeedResult(ApiResponse apiResponse, SettingsFile settings) {
        logger.info("Seed created successfully: seedUrl={}, version={}",
                apiResponse.seedUrl, apiResponse.version);

        return new SeedResult(
                apiResponse.seedUrl,
                apiResponse.version,
                apiResponse.spoilers,
                settings
        );
    }

    /**
     * DTO representing the API response structure
     */
    public static class ApiResponse {
        public String seedUrl;
        public String version;
        public Boolean spoilers;

        // For mock construction
        public ApiResponse() {}

        public ApiResponse(String seedUrl, String version, Boolean spoilers) {
            this.seedUrl = seedUrl;
            this.version = version;
            this.spoilers = spoilers;
        }
    }
}
