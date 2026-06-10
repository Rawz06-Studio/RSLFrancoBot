package fr.rawz06.rslfrancobot.api.python;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.rawz06.rslfrancobot.engine.domain.entities.Preset;
import fr.rawz06.rslfrancobot.engine.domain.entities.SettingsFile;
import fr.rawz06.rslfrancobot.engine.domain.ports.RSLScriptRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Real implementation of the RSL Python script.
 * Executes RandomSettingsGenerator.py script to generate random settings.
 */
@Component
public class PythonRSLScriptAdapter implements RSLScriptRunner {

    private static final Logger logger = LoggerFactory.getLogger(PythonRSLScriptAdapter.class);
    private static final Pattern FILENAME_PATTERN = Pattern.compile("Plando File: (.+\\.json)");

    @Value("${app.python.command}")
    private String pythonCommand;

    @Value("${app.python.script.dir}")
    private String scriptDir;

    @Value("${app.python.script.name}")
    private String scriptName;

    @Value("${app.python.weights.rsl}")
    private String rslWeight;

    @Value("${app.python.weights.pot}")
    private String potWeight;

    @Value("${app.python.weights.rot}")
    private String rotWeight;

    @Value("${app.python.weights.beginner}")
    private String beginnerWeight;

    private final ObjectMapper objectMapper;

    public PythonRSLScriptAdapter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public SettingsFile generateSettings(Preset preset) throws ScriptExecutionException {
        String weightFile = switch (preset.name()) {
            case "rsl" -> rslWeight;
            case "pot" -> potWeight;
            case "rot" -> rotWeight;
            case "beginner" -> beginnerWeight;
            default -> throw new ScriptExecutionException("Unknown preset: " + preset.name());
        };

        logger.info("Generating {} settings with Python script...", preset.name());
        logger.info("Weight file: {}", weightFile);

        try {
            // 1. Prepare command
            ProcessBuilder pb = new ProcessBuilder(
                    pythonCommand,
                    scriptName,
                    "--override", weightFile,
                    "--no_seed"
            );

            // Set working directory
            File workingDir = new File(scriptDir);
            if (!workingDir.exists() || !workingDir.isDirectory()) {
                throw new ScriptExecutionException("Script directory not found: " + scriptDir);
            }
            pb.directory(workingDir);
            pb.redirectErrorStream(true);

            logger.info("Command: {} in {}", String.join(" ", pb.command()), workingDir.getAbsolutePath());

            // 2. Execute script
            Process process = pb.start();

            // 3. Capture output
            String output;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                output = reader.lines().collect(Collectors.joining("\n"));
            }

            int exitCode = process.waitFor();
            logger.info("Script completed with exit code: {}", exitCode);
            logger.debug("Script output:\n{}", output);

            if (exitCode != 0) {
                throw new ScriptExecutionException("Script failed with exit code " + exitCode + ": " + output);
            }

            // 4. Extract generated file name
            String filename = extractFilename(output);
            logger.info("Generated file: {}", filename);

            // 5. Read JSON file
            File generatedFile = new File(workingDir, "data/" + filename);
            if (!generatedFile.exists()) {
                throw new ScriptExecutionException("Generated file not found: " + generatedFile.getAbsolutePath());
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> settings = objectMapper.readValue(generatedFile, Map.class);
            logger.info("Settings read successfully ({} keys)", settings.size());

            // 6. Delete temporary file
            boolean deleted = generatedFile.delete();
            if (!deleted) {
                logger.warn("Unable to delete temporary file: {}", generatedFile.getAbsolutePath());
            } else {
                logger.debug("Temporary file deleted: {}", filename);
            }

            return new SettingsFile(settings);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ScriptExecutionException("Script execution interrupted", e);
        } catch (Exception e) {
            logger.error("Error executing Python script", e);
            throw new ScriptExecutionException("Python script execution failed: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts filename from script output.
     * Looks for the line "Plando File: XXX.json"
     */
    private String extractFilename(String output) throws ScriptExecutionException {
        Matcher matcher = FILENAME_PATTERN.matcher(output);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new ScriptExecutionException(
                "Could not extract filename from script output. Looking for 'Plando File: XXX.json' pattern.\n" +
                "Output was:\n" + output
        );
    }
}
