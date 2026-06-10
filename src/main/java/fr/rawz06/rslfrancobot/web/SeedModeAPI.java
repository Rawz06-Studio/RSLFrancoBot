package fr.rawz06.rslfrancobot.web;

import fr.rawz06.rslfrancobot.engine.domain.entities.SeedMode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public enum SeedModeAPI {
    SEED_S8("seed_s8", SeedMode.S8),
    SEED_S9("seed_s9", SeedMode.S9),
    SEED_TOT("seed_tot", SeedMode.TOT),
    SEED_MIXED("seed_mixed", SeedMode.MIXED),

    SEED_RSL("seed_rsl", SeedMode.RSL),
    SEED_POT("seed_pot", SeedMode.POT),
    SEED_BEGINNER("seed_beginner", SeedMode.BEGINNER),
    SEED_ROT("seed_rot", SeedMode.ROT),

    SEED_ALLSANITY_ER_DECOUPLED("seed_allsanity_er_decoupled", SeedMode.ALLSANITY_ER_DECOUPLED),
    SEED_ALLSANITY_ER("seed_allsanity_er", SeedMode.ALLSANITY_ER),
    SEED_ALLSANITY_ONLY("seed_allsanity_only", SeedMode.ALLSANITY_ONLY),

    SEED_SALAD_ENEMY("seed_salad_enemy", SeedMode.SALAD_ENEMY),
    SEED_SALAD_RUPEE("seed_salad_rupee", SeedMode.SALAD_RUPEES),
    SEED_SALAD_SONGS("seed_salad_songs", SeedMode.SALAD_SONGS),
    SEED_SALAD_DUNGEON("seed_salad_dungeon", SeedMode.SALAD_DUNGEONS),
    SEED_SALAD_MIX("seed_salad_mix", SeedMode.SALAD_MIX),
    SEED_SALAD_ALL("seed_salad_all", SeedMode.SALAD_ALL),
    SEED_SALAD_NATURE("seed_salad_nature", SeedMode.SALAD_NATURE);

    private final String apiId;
    private final SeedMode seedMode;

    private static final Map<String, SeedModeAPI> BY_API_ID = Arrays.stream(values())
            .collect(Collectors.toMap(SeedModeAPI::getApiId, Function.identity()));

    public static Optional<SeedModeAPI> fromApiId(String apiId) {
        return Optional.ofNullable(BY_API_ID.get(apiId));
    }
}
