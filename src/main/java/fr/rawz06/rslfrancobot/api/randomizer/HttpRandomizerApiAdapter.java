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

/**
 * Real HTTP implementation of the Seed Generation API.
 * Calls the external seed generation API at {apiDomain}/api/seed/{mode}
 */
@Component
@ConditionalOnProperty(name = "app.seed.api.mode", havingValue = "http", matchIfMissing = false)
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
        // 1. Log settings (business logic in service)
        apiService.logSettings(mode, settings);

        try {
            // 2. Build URL with query parameters (business logic in service)
            String url = apiService.buildApiUrl(mode, settings);

            // 3. Prepare HTTP request (HTTP-specific logic only)
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Void> request = new HttpEntity<>(headers);

            // 4. Send GET request (HTTP-specific logic only)
            logger.info("Calling Seed Generation API: GET {}", url);
            ResponseEntity<ApiResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    ApiResponse.class
            );

            // 5. Validate response (HTTP-specific logic only)
            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                throw new RandomizerApiException("API returned status: " + response.getStatusCode());
            }

            // 6. Build SeedResult (business logic in service)
            ApiResponse apiResponse = response.getBody();
            return apiService.buildSeedResult(apiResponse, settings);

        } catch (Exception e) {
            logger.error("Error calling seed generation API", e);
            throw new RandomizerApiException("Failed to generate seed: " + e.getMessage(), e);
        }
    }
}
