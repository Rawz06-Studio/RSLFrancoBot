package fr.rawz06.rslfrancobot.web.dto;

import fr.rawz06.rslfrancobot.engine.domain.entities.Preset;
import fr.rawz06.rslfrancobot.engine.domain.entities.SeedResult;

import java.util.List;

/**
 * Response object for Franco seed generation API.
 */
public record FrancoSeedResponse(SeedResult seed, List<Preset.PresetOption> options) {}
