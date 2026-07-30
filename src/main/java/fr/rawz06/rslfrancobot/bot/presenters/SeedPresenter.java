package fr.rawz06.rslfrancobot.bot.presenters;

import fr.rawz06.rslfrancobot.bot.models.DiscordButton;
import fr.rawz06.rslfrancobot.bot.models.DiscordMessage;
import fr.rawz06.rslfrancobot.bot.models.DiscordSelectMenu;
import fr.rawz06.rslfrancobot.engine.domain.entities.Preset;
import fr.rawz06.rslfrancobot.engine.domain.entities.SeedResult;
import fr.rawz06.rslfrancobot.engine.usecases.visibility.GetUserAvailableGenerateUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Presenter pour formater les messages liés aux seeds.
 * Transforme les objets du domaine en messages Discord.
 */
@Component
@RequiredArgsConstructor
public class SeedPresenter {

    @Value("${app.version}")
    private String appVersion;

    private final GetUserAvailableGenerateUseCase getUserAvailableGenerateUseCase;

    /**
     * Creates the initial seed mode selection message.
     */
    public DiscordMessage presentModeSelection(String username) {
        DiscordMessage message = new DiscordMessage("What type of seed do you want to generate?");

        // First row: Classic modes
        List<DiscordButton> classicRow = new ArrayList<>(List.of(
                new DiscordButton("Franco S6", "seed_franco", DiscordButton.Style.SUCCESS),
                new DiscordButton("S8", "seed_s8", DiscordButton.Style.SECONDARY),
                new DiscordButton("S9", "seed_s9", DiscordButton.Style.PRIMARY),
                new DiscordButton("ToT", "seed_tot", DiscordButton.Style.SECONDARY),
                new DiscordButton("Mixed Pool S5", "seed_mixed", DiscordButton.Style.SUCCESS)
        ));
        message.addButtonRow(classicRow);

        // Second row: RSL modes
        List<DiscordButton> rslRow = new ArrayList<>(List.of(
            new DiscordButton("Main (RSL)", "seed_rsl", DiscordButton.Style.PRIMARY),
            new DiscordButton("PoT (RSL)", "seed_pot", DiscordButton.Style.SECONDARY, true),
            new DiscordButton("Beginner (RSL)", "seed_beginner", DiscordButton.Style.SECONDARY, true),
            new DiscordButton("RoT (RSL)", "seed_rot", DiscordButton.Style.SUCCESS)
        ));
        
        message.addButtonRow(rslRow);

        return message;
    }

    /**
     * Creates the initial seed mode selection message.
     */
    public DiscordMessage presentModeAllSelection() {
        DiscordMessage message = new DiscordMessage("What type of seed do you want to generate?");

        // First row: Allsanity modes
        List<DiscordButton> allsanityRow = List.of(
                new DiscordButton("Allsanity + ER decoupled", "seed_allsanity_er_decoupled", DiscordButton.Style.SECONDARY),
                new DiscordButton("Allsanity + ER", "seed_allsanity_er", DiscordButton.Style.SECONDARY),
                new DiscordButton("Allsanity + ER without OW", "seed_allsanity_er_noow", DiscordButton.Style.SECONDARY),
                new DiscordButton("Allsanity only", "seed_allsanity_only", DiscordButton.Style.SECONDARY)
        );
        message.addButtonRow(allsanityRow);

        return message;
    }

    /**
     * Creates the initial seed mode selection message.
     */
    public DiscordMessage presentModeSaladSelection() {
        DiscordMessage message = new DiscordMessage("What type of seed do you want to generate?");

        // first row: Salad modes (first part)
        List<DiscordButton> saladRow = List.of(
                new DiscordButton("Monstre en folie", "seed_salad_enemy", DiscordButton.Style.SECONDARY),
                new DiscordButton("Rubis en folie", "seed_salad_rupee", DiscordButton.Style.SECONDARY),
                new DiscordButton("Chant en folie", "seed_salad_songs", DiscordButton.Style.SECONDARY)
        );
        message.addButtonRow(saladRow);

        // second row: Salad modes (second part)
        List<DiscordButton> salad2Row = List.of(
                new DiscordButton("Donjon en folie", "seed_salad_dungeon", DiscordButton.Style.SECONDARY),
                new DiscordButton("Mix en folie", "seed_salad_mix", DiscordButton.Style.SECONDARY),
                new DiscordButton("Salade en folie", "seed_salad_all", DiscordButton.Style.SECONDARY)
        );
        message.addButtonRow(salad2Row);

        // third row: Salad modes (second part)
        List<DiscordButton> salad3Row = List.of(
                new DiscordButton("Nature en folie", "seed_salad_nature", DiscordButton.Style.SECONDARY)
        );
        message.addButtonRow(salad3Row);

        return message;
    }

    /**
     * Presents the result of a generated seed (RSL/PoT mode).
     */
    public DiscordMessage presentSeedResult(SeedResult result, String seedType, String username) {
        StringBuilder content = new StringBuilder();
        content.append("✅ Seed ").append(seedType).append(" generated successfully by ").append(username).append("!\n\n");
        content.append("🔗 Link: ").append(result.seedUrl()).append("\n");

        if (result.version() != null) {
            content.append("📦 Version: ").append(result.version()).append("\n");
        }

        if (result.spoilers() != null) {
            content.append("👁️ Spoilers: ").append(result.spoilers() ? "Yes" : "No");
        }

        content.append("\n\n");
        content.append("🤖Bot Version: ").append(appVersion.split("-")[0]);

        return new DiscordMessage(content.toString());
    }

    /**
     * Presents the result of a generated Franco seed with selected options.
     */
    public DiscordMessage presentFrancoSeedResult(SeedResult result, List<String> selectedOptions, String username) {
        StringBuilder content = new StringBuilder();
        content.append("✅ **Franco seed generated successfully** for ").append(username).append("!\n\n");
        content.append("🔗 **Link:** ").append(result.seedUrl()).append("\n");

        if (result.version() != null) {
            content.append("📦 **Version:** ").append(result.version()).append("\n");
        }

        if (result.spoilers() != null) {
            content.append("👁️ **Spoilers:** ").append(result.spoilers() ? "Yes" : "No").append("\n");
        }

        // Add selected settings
        content.append("\n🔧 **Enabled options:**\n");
        if (selectedOptions == null || selectedOptions.isEmpty()) {
            content.append("_Base preset only (no specific options)_");
        } else {
            for (String optionId : selectedOptions) {
                content.append("• `").append(optionId).append("`\n");
            }
        }

        content.append("\n\n");
        content.append("🤖 Bot Version: ").append(appVersion.split("-")[0]);

        return new DiscordMessage(content.toString());
    }

    /**
     * Presents an error message.
     */
    public DiscordMessage presentError(String errorMessage) {
        if (errorMessage.startsWith("Invalid settings: Incompatibility detected: ")) {
            String cleanMessage = errorMessage
                    .replace("Invalid settings: Incompatibility detected: ", "");
            
            return new DiscordMessage("⚠️ **Oops! Incompatible settings**\n\n" +
                    "Some chosen options cannot work together:\n" +
                    "❌ " + cleanMessage + "\n\n" +
                    "Please try again by choosing compatible options.");
        }
        return new DiscordMessage("❌ **Error:** " + errorMessage);
    }

    /**
     * Presents the Franco options selection menu.
     */
    public DiscordMessage presentFrancoOptions(List<Preset.PresetOption> options) {
        DiscordMessage message = new DiscordMessage(
                "### 🇫🇷 Franco Mode - Options Selection\n" +
                "Choose additional options to enable:\n" +
                "_You can select multiple options in the menus below._"
        );

        // Split options into multiple menus if needed (max 25 per menu)
        int menuIndex = 0;
        for (int i = 0; i < options.size(); i += 25) {
            List<Preset.PresetOption> chunk = options.subList(i, Math.min(i + 25, options.size()));
            DiscordSelectMenu menu = createOptionsMenu(chunk, menuIndex++);
            message.addSelectMenu(menu);
        }

        // Add validation buttons
        message.addButton("Validate & Generate", "franco_validate", DiscordButton.Style.SUCCESS);
        message.addButton("Random Selection", "franco_random", DiscordButton.Style.PRIMARY);
        message.addButton("Cancel", "franco_cancel", DiscordButton.Style.SECONDARY);

        return message;
    }

    private DiscordSelectMenu createOptionsMenu(List<Preset.PresetOption> options, int menuIndex) {
        DiscordSelectMenu menu = new DiscordSelectMenu("franco_options_" + menuIndex);
        menu.setPlaceholder("Select your options...");
        menu.setMinValues(0);
        menu.setMaxValues(options.size());

        for (Preset.PresetOption option : options) {
            menu.addOption(option.label(), option.id(), option.description());
        }

        return menu;
    }

    /**
     * Confirmation message for RSL/PoT generation.
     */
    public DiscordMessage presentRSLConfirmation(String mode) {
        return new DiscordMessage(
                String.format("🎲 Generating a random %s seed...", mode)
        );
    }

    /**
     * Presents the selected options before seed generation.
     */
    public DiscordMessage presentSelectedOptions(List<String> selectedOptionIds) {
        StringBuilder content = new StringBuilder();
        content.append("🔧 **Selected Franco options:**\n\n");

        if (selectedOptionIds == null || selectedOptionIds.isEmpty()) {
            content.append("_No specific options (base preset only)_\n");
        } else {
            for (String optionId : selectedOptionIds) {
                content.append("✅ `").append(optionId).append("`\n");
            }
        }

        content.append("\n⏳ **Generating seed...**");

        return new DiscordMessage(content.toString());
    }
}
