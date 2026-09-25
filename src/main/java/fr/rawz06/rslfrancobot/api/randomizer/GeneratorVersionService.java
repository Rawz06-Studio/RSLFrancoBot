package fr.rawz06.rslfrancobot.api.randomizer;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.rawz06.rslfrancobot.engine.domain.entities.GroupMode;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Service to fetch generator versions from the external API.
 * Calls /api/seed/info endpoint.
 */
@Service
public class GeneratorVersionService {

    private static final Logger logger = LoggerFactory.getLogger(GeneratorVersionService.class);

    @Value("${app.seed.api.domain}")
    private String apiDomain;

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public GeneratorVersionService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClients.createDefault();
    }

    /**
     * Fetches generator versions from the external API.
     * @return Map of GroupMode to version string
     */
    public Map<GroupMode, String> fetchVersions() {
        try {
            String url = apiDomain + "/api/seed/info";
            HttpGet httpGet = new HttpGet(url);
            httpGet.setHeader("Content-Type", "application/json");

            logger.info("Fetching generator versions from: {}", url);
            
            // Use response handler to read content before response is closed
            Map<GroupMode, String> versions = httpClient.execute(httpGet, response -> {
                if (response.getCode() != HttpStatus.SC_OK) {
                    logger.error("API returned status: {}", response.getCode());
                    return new HashMap<>();
                }

                String responseBody = new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
                Map<String, String> rawVersions = objectMapper.readValue(responseBody, Map.class);
                
                // Convert String keys to GroupMode enum
                Map<GroupMode, String> result = new HashMap<>();
                for (Map.Entry<String, String> entry : rawVersions.entrySet()) {
                    try {
                        GroupMode mode = GroupMode.valueOf(entry.getKey());
                        result.put(mode, entry.getValue());
                    } catch (IllegalArgumentException e) {
                        logger.warn("Unknown group mode in API response: {}", entry.getKey());
                    }
                }
                
                logger.debug("Generator versions fetched successfully: {}", result);
                return result;
            });
            
            return versions;

        } catch (IOException e) {
            logger.error("Error fetching generator versions from API", e);
            return new HashMap<>();
        }
    }
}
