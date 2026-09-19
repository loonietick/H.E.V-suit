package ltown.hev_suit.client.api;

import net.minecraft.entity.damage.DamageSource;

import java.util.function.Predicate;

/**
 * A companion mod's rule for "when a hit matches this, play this voice line (rate-limited by this
 * cooldown tier) and optionally flash this status icon." Registered via
 * {@link HevSuitApi#registerDamageReaction(HevDamageReaction)}; checked additively, after the base
 * mod's own built-in damage diagnosis in EventManager, never replacing it.
 *
 * @param matcher      tested against the DamageSource of each hit the player takes
 * @param voiceLineId  the id passed to {@link HevSuitApi#triggerDiagnosis(String, HevCooldownTier)}
 * @param cooldownTier how often this reaction is allowed to re-fire
 * @param statusIcon   optional HUD icon to flash alongside the line; null for none
 */
public record HevDamageReaction(
        Predicate<DamageSource> matcher,
        String voiceLineId,
        HevCooldownTier cooldownTier,
        HevStatusIcon statusIcon
) {
    public HevDamageReaction(Predicate<DamageSource> matcher, String voiceLineId, HevCooldownTier cooldownTier) {
        this(matcher, voiceLineId, cooldownTier, null);
    }
}
