package fr.rawz06.rslfrancobot.api.randomizer;

import fr.rawz06.rslfrancobot.api.randomizer.RandomizerApiService.ApiResponse;
import fr.rawz06.rslfrancobot.engine.domain.entities.SeedMode;
import fr.rawz06.rslfrancobot.engine.domain.entities.SeedResult;
import fr.rawz06.rslfrancobot.engine.domain.entities.SettingsFile;
import fr.rawz06.rslfrancobot.engine.domain.ports.RandomizerApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Real HTTP implementation of the Randomizer API.
 * Calls https://ootrandomizer.com/api/v2/seed/create
 * All business logic is delegated to RandomizerApiService.
 *
 * Active only in the 'prod' profile.
 */
@Component
@ConditionalOnProperty(name = "app.randomizer.api.mode", havingValue = "http", matchIfMissing = true)
public class HttpRandomizerApiAdapter implements RandomizerApi {

    private static final Logger logger = LoggerFactory.getLogger(HttpRandomizerApiAdapter.class);

    private final RestTemplate restTemplate;
    private final RandomizerApiService apiService;

    public HttpRandomizerApiAdapter(RandomizerApiService apiService) {
        this.apiService = apiService;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public SeedResult generateSeed(SeedMode mode, SettingsFile settings) throws RandomizerApiException {
        // 1. Get version for this mode (business logic in service)
        String version = apiService.getVersionForMode(mode);

        // 2. Log settings (business logic in service)
        apiService.logSettings(mode, version, settings);

        try {
            // 3. Build URL with query parameters (business logic in service)
            String url = apiService.buildApiUrl(version);

            // 4. Prepare HTTP request (HTTP-specific logic only)
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(settings.settings(), headers);

            // 5. Send POST request (HTTP-specific logic only)
            logger.info("Calling API: POST https://ootrandomizer.com/api/v2/seed/create");
            ResponseEntity<ApiResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    ApiResponse.class
            );

            // 6. Validate response (HTTP-specific logic only)
            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                throw new RandomizerApiException("API returned status: " + response.getStatusCode());
            }

            // 7. Build SeedResult (business logic in service)
            ApiResponse apiResponse = response.getBody();
            return apiService.buildSeedResult(apiResponse, settings);

        } catch (Exception e) {
            logger.error("Error calling randomizer API", e);
            throw new RandomizerApiException("Failed to generate seed: " + e.getMessage(), e);
        }
    }
}
