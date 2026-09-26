package fr.rawz06.rslfrancobot.api.randomizer;

import fr.rawz06.rslfrancobot.api.randomizer.RandomizerApiService.ApiResponse;
import fr.rawz06.rslfrancobot.engine.domain.entities.SeedMode;
import fr.rawz06.rslfrancobot.engine.domain.entities.SeedResult;
import fr.rawz06.rslfrancobot.engine.domain.entities.SettingsFile;
import fr.rawz06.rslfrancobot.engine.domain.ports.RandomizerApi;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Real HTTP implementation of the Seed Generation API.
 * Calls the external seed generation API at {apiDomain}/api/seed/{mode}
 */
@Component
@ConditionalOnProperty(name = "app.seed.api.mode", havingValue = "http", matchIfMissing = false)
public class HttpRandomizerApiAdapter implements RandomizerApi {

    private static final Logger logger = LoggerFactory.getLogger(HttpRandomizerApiAdapter.class);

    private final RandomizerApiService apiService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public HttpRandomizerApiAdapter(RandomizerApiService apiService, ObjectMapper objectMapper) {
        this.apiService = apiService;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClients.createDefault();
    }

    @Override
    public SeedResult generateSeed(SeedMode mode, SettingsFile settings) throws RandomizerApiException {
        // 1. Log settings (business logic in service)
        apiService.logSettings(mode, settings);

        try {
            // 2. Build URL with query parameters (business logic in service)
            String url = apiService.buildApiUrl(mode, settings);

            // 3. Create GET request (HTTP-specific logic only)
            HttpGet httpGet = new HttpGet(url);
            httpGet.setHeader("Content-Type", "application/json");

            // 4. Send GET request with response handler (HTTP-specific logic only)
            logger.info("Calling Seed Generation API: GET {}", url);
            String responseBody = httpClient.execute(httpGet, (HttpClientResponseHandler<String>) response -> {
                if (response.getCode() != HttpStatus.SC_OK) {
                    throw new RuntimeException(new RandomizerApiException("API returned status: " + response.getCode()));
                }
                try {
                    return new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            // 5. Parse response body
            ApiResponse apiResponse = objectMapper.readValue(responseBody, ApiResponse.class);

            if (apiResponse == null) {
                throw new RandomizerApiException("API returned empty response");
            }

            // 7. Build SeedResult (business logic in service)
            return apiService.buildSeedResult(apiResponse, settings);

        } catch (IOException e) {
            logger.error("Error calling seed generation API", e);
            throw new RandomizerApiException("Failed to generate seed: " + e.getMessage(), e);
        } catch (RuntimeException e) {
            if (e.getCause() instanceof RandomizerApiException) {
                throw (RandomizerApiException) e.getCause();
            }
            logger.error("Error calling seed generation API", e);
            throw new RandomizerApiException("Failed to generate seed: " + e.getMessage(), e);
        }
    }
}
