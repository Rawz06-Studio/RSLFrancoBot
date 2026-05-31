package fr.rawz06.rslfrancobot.api.randomizer;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.rawz06.rslfrancobot.api.randomizer.RandomizerApiService.ApiResponse;
import fr.rawz06.rslfrancobot.engine.domain.entities.SeedMode;
import fr.rawz06.rslfrancobot.engine.domain.entities.SeedResult;
import fr.rawz06.rslfrancobot.engine.domain.entities.SettingsFile;
import fr.rawz06.rslfrancobot.engine.domain.ports.RandomizerApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Mock implementation of the Randomizer API.
 * Simulates seed generation without calling the real API.
 * Uses the same business logic as HttpRandomizerApiAdapter via RandomizerApiService.
 *
 * Active only in 'dev' and 'local' profiles.
 */
@Component
@ConditionalOnProperty(name = "app.randomizer.api.mode", havingValue = "mock")
public class MockRandomizerApiAdapter implements RandomizerApi {

    private static final Logger logger = LoggerFactory.getLogger(MockRandomizerApiAdapter.class);

    private final RandomizerApiService apiService;
    private final ObjectMapper objectMapper;

    public MockRandomizerApiAdapter(RandomizerApiService apiService, ObjectMapper objectMapper) {
        this.apiService = apiService;
        this.objectMapper = objectMapper;
    }

    @Override
    public SeedResult generateSeed(SeedMode mode, SettingsFile settings) throws RandomizerApiException {
        // 1. Get version for this mode (same business logic as HTTP adapter)
        String version = apiService.getVersionForMode(mode);

        // 2. Display settings in formatted JSON for verification (INFO level for Mock)
        try {
            String jsonSettings = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(settings.settings());

            System.out.println("\n" + "=".repeat(80));
            System.out.println("📋 SETTINGS SENT TO RANDOMIZER API (MOCK)");
            System.out.println("=".repeat(80));
            System.out.println(jsonSettings);
            System.out.println("=".repeat(80) + "\n");

            logger.info("MOCK: Settings contains {} keys", settings.settings().size());
        } catch (Exception e) {
            logger.error("Error serializing settings", e);
        }

        // 3. Simulate realistic network delay (5 seconds like a real HTTP call)
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RandomizerApiException("Generation interrupted", e);
        }

        // 4. Create fake API response (simulating what the real API would return)
        String mockId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        ApiResponse mockResponse = new ApiResponse(mockId, version, true);

        logger.info("MOCK: Simulating API response with id={}", mockId);

        // 5. Build SeedResult (same business logic as HTTP adapter)
        return apiService.buildSeedResult(mockResponse, settings);
    }
}
