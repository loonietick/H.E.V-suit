package ltown.hev_suit.client.api;

/**
 * No-repeat cooldown tiers for {@link HevSuitApi#triggerDiagnosis(String, HevCooldownTier)} and
 * {@link HevSuitApi#registerDamageReaction(HevDamageReaction)}, matching the five real no-repeat
 * durations from HL1's suit voice system (dlls/player.cpp SUIT_NEXT_IN_30SEC/_1MIN/_5MIN/_10MIN/_30MIN),
 * plus NONE for lines that can repeat freely (SUIT_REPEAT_OK).
 *
 * This is a separate, new set of cooldowns for the extensible API -- it does not touch or replace
 * EventManager's existing hardcoded per-line cooldown fields for the mod's built-in HL1 lines,
 * which keep their own historical "classic"/"accurate" dual values untouched.
 */
public enum HevCooldownTier {
    NONE(0),
    THIRTY_SECONDS(30_000),
    ONE_MINUTE(60_000),
    FIVE_MINUTES(300_000),
    TEN_MINUTES(600_000),
    THIRTY_MINUTES(1_800_000);

    private final long millis;

    HevCooldownTier(long millis) {
        this.millis = millis;
    }

    public long millis() {
        return millis;
    }
}
