package fr.rawz06.rslfrancobot.bot.presenters;

import fr.rawz06.rslfrancobot.api.randomizer.GeneratorVersionService;
import fr.rawz06.rslfrancobot.bot.models.DiscordMessage;
import fr.rawz06.rslfrancobot.engine.domain.entities.GroupMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootVersion;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Component
public class InfoPresenter {

    private final GeneratorVersionService versionService;
    private final String botVersion;

    public InfoPresenter(GeneratorVersionService versionService, @Value("${app.version:unknown}") String botVersion) {
        this.versionService = versionService;
        this.botVersion = botVersion;
    }

    public DiscordMessage presentInfo() {
        // Fetch versions from external API
        Map<GroupMode, String> versions = versionService.fetchVersions();

        StringBuilder sb = new StringBuilder();
        sb.append("ℹ️ **Bot Information**\n\n");

        sb.append("**System Versions:**\n");
        sb.append("- Bot: `").append(botVersion).append("`\n");
        sb.append("- Spring Boot: `").append(SpringBootVersion.getVersion()).append("`\n");
        sb.append("- Java: `").append(System.getProperty("java.version")).append("`\n\n");

        sb.append("**Generator Versions:**\n\n");
        
        // Classic Versions
        sb.append("__Classic Versions__\n");
        sb.append("- S8: `").append(versions.getOrDefault(GroupMode.S8, "N/A")).append("`\n");
        sb.append("- S9: `").append(versions.getOrDefault(GroupMode.S9, "N/A")).append("`\n");
        sb.append("- ToT: `").append(versions.getOrDefault(GroupMode.TOT, "N/A")).append("`\n");
        sb.append("- Mixed Pool: `").append(versions.getOrDefault(GroupMode.MIXED, "N/A")).append("`\n");
        sb.append("- Franco: `").append(versions.getOrDefault(GroupMode.FRANCO, "N/A")).append("`\n\n");

        // RSL Versions
        sb.append("__RSL Versions__\n");
        sb.append("- RSL: `").append(versions.getOrDefault(GroupMode.RSL, "N/A")).append("`\n\n");

        // Salad Versions
        sb.append("__Salad Versions__\n");
        sb.append("- Enemy Salad: `").append(versions.getOrDefault(GroupMode.SALAD_ENEMY, "N/A")).append("`\n");
        sb.append("- Salad: `").append(versions.getOrDefault(GroupMode.SALAD, "N/A")).append("`\n\n");

        // All Sanity
        sb.append("__All Sanity__\n");
        sb.append("- Enemizer Allsanity: `").append(versions.getOrDefault(GroupMode.ALLSANITY, "N/A")).append("`\n");

        RuntimeMXBean rb = ManagementFactory.getRuntimeMXBean();
        long startTime = rb.getStartTime();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
        String formattedStartTime = formatter.format(Instant.ofEpochMilli(startTime));

        sb.append("**Started at:** `").append(formattedStartTime).append("`\n");

        return new DiscordMessage(sb.toString());
    }
}
