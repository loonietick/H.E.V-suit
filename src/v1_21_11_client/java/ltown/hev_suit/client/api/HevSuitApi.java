package ltown.hev_suit.client.api;

import ltown.hev_suit.client.managers.FlashlightManager;
import ltown.hev_suit.client.managers.HudManager;
import ltown.hev_suit.client.managers.SettingsManager;
import ltown.hev_suit.client.managers.SoundManager;
import ltown.hev_suit.client.managers.SubtitleManager;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Public entry point for a companion mod (e.g. a real wearable HEV Suit item mod) to hook into
 * this mod's HUD/voice/flashlight systems instead of duplicating them. See
 * {@code HEV_SUIT_API_DESIGN.md} for the full design rationale.
 *
 * Every method here is additive: absent a companion mod calling any of it, nothing about the base
 * mod's standalone behavior changes.
 */
public final class HevSuitApi {
    private HevSuitApi() {}

    /**
     * Item tag a companion mod tags its armor into to be recognized as "the suit."
     */
    public static final TagKey<Item> SUIT_PIECES = TagKey.of(RegistryKeys.ITEM, Identifier.of("hev_suit", "suit_pieces"));

    private static final List<HevDamageReaction> DAMAGE_REACTIONS = new CopyOnWriteArrayList<>();
    private static final ConcurrentHashMap<String, Long> LAST_TRIGGERED_AT = new ConcurrentHashMap<>();

    // === Suit-worn detection ===

    /**
     * Gates the suit's audio/voice systems and flashlight -- the body (chest slot) specifically.
     * These are suit-powered systems, not visor-specific, so they run off the body being worn
     * regardless of whether the helmet is on.
     */
    public static boolean isWearingSuit(PlayerEntity player) {
        if (player == null) return false;
        ItemStack chest = player.getEquippedStack(EquipmentSlot.CHEST);
        return !chest.isEmpty() && chest.isIn(SUIT_PIECES);
    }

    /**
     * Gates the HUD specifically -- the helmet slot. A "heads-up display" reads off the visor:
     * with the item mod's one-suit design (body+legs+boots always move together, only the helmet
     * is independently removable), taking the helmet off kills the display even while the rest of
     * the suit (and its audio/flashlight) keeps running.
     */
    public static boolean isHelmetOn(PlayerEntity player) {
        if (player == null) return false;
        ItemStack helmet = player.getEquippedStack(EquipmentSlot.HEAD);
        return !helmet.isEmpty() && helmet.isIn(SUIT_PIECES);
    }

    /**
     * True if some mod has actually tagged an item into {@link #SUIT_PIECES} -- i.e. a real,
     * wearable suit exists in this install, not just this base mod on its own. Standalone
     * installs (no companion item mod) always get false here, which is exactly the signal
     * EventManager/FlashlightManager use to decide whether "wearing the suit" should gate
     * anything at all, or whether to fall back to the original toggle-only behavior.
     */
    public static boolean isRealSuitAvailable() {
        return Registries.ITEM.iterateEntries(SUIT_PIECES).iterator().hasNext();
    }

    // === Suit power ===

    /**
     * Call once at companion-mod init. Before this call, the HUD's power/armor readout derives
     * from vanilla armor value x durability, exactly as it does standalone today. After this call,
     * it renders only what's pushed via {@link #setSuitPower(int)}.
     */
    public static void registerSuitPowerSource() {
        HudManager.registerSuitPowerSource();
    }

    public static void setSuitPower(int value) {
        HudManager.setSuitPower(value);
    }

    public static int getSuitPower() {
        return HudManager.getSuitPower();
    }

    // === Damage diagnosis ===

    /**
     * Layers a new damage-type reaction on top of (never replacing) the base mod's built-in
     * damage diagnosis. Checked every time the player takes damage.
     */
    public static void registerDamageReaction(HevDamageReaction reaction) {
        if (reaction == null) return;
        DAMAGE_REACTIONS.add(reaction);
    }

    /**
     * Internal dispatch point, called by EventManager after its own built-in diagnosis checks.
     * Not intended to be called by a companion mod directly -- use
     * {@link #registerDamageReaction(HevDamageReaction)} instead.
     */
    public static void dispatchDamageReactions(DamageSource damageSource, float damage) {
        if (damageSource == null || DAMAGE_REACTIONS.isEmpty()) return;
        for (HevDamageReaction reaction : DAMAGE_REACTIONS) {
            if (reaction.matcher() != null && reaction.matcher().test(damageSource)) {
                triggerDiagnosis(reaction.voiceLineId(), reaction.cooldownTier());
                if (reaction.statusIcon() != null) {
                    setStatusIcon(reaction.statusIcon(), true);
                }
            }
        }
    }

    /**
     * Lower-level trigger for lines that aren't tied to a damage event at all (e.g. an ammo
     * dry-fire announcement), rate-limited by the given cooldown tier.
     */
    public static void triggerDiagnosis(String lineId, HevCooldownTier tier) {
        if (lineId == null || lineId.isEmpty()) return;
        long cooldownMillis = tier == null ? 0 : tier.millis();
        long now = System.currentTimeMillis();
        Long last = LAST_TRIGGERED_AT.get(lineId);
        if (last != null && now - last < cooldownMillis) return;
        LAST_TRIGGERED_AT.put(lineId, now);
        queueVoiceLine(lineId);
    }

    // === Voice lines ===

    /** Adds a companion mod's own line id/sound/caption without touching the base mod's internals. */
    public static void registerVoiceLine(String id, SoundEvent event, String captionText) {
        if (id == null || id.isEmpty() || event == null) return;
        SoundManager.registerExternalSound(id, event);
        if (captionText != null && !captionText.isEmpty()) {
            SubtitleManager.registerCaption(id, captionText);
        }
    }

    public static void queueVoiceLine(String id) {
        SoundManager.queueSound(id);
    }

    public static void playImmediateVoiceLine(String id) {
        SoundManager.playImmediateSound(id);
    }

    // === Ammo override ===

    public static void setAmmoOverride(int loaded, int reserve) {
        HudManager.setAmmoOverride(loaded, reserve);
    }

    public static void clearAmmoOverride() {
        HudManager.clearAmmoOverride();
    }

    // === HUD status icons ===

    public static void setStatusIcon(HevStatusIcon icon, boolean active) {
        if (icon == null) return;
        switch (icon) {
            case COLD -> HudManager.setColdActive(active);
            case RADIATION -> HudManager.setRadiationActive(active);
            case BIOHAZARD -> HudManager.setBiohazardActive(active);
            case ELECTRICAL -> {
                if (active) {
                    HudManager.triggerElectricalAlert();
                } else {
                    HudManager.clearElectricalAlert();
                }
            }
            case NERVEGAS -> HudManager.setNervegasActive(active);
            case WASTE -> HudManager.setWasteActive(active);
        }
    }

    // === Flashlight (read-only) ===

    public static int getFlashlightBattery() {
        return FlashlightManager.getBattery();
    }

    public static boolean isFlashlightOn() {
        return FlashlightManager.isOn();
    }

    // === Config ===

    /**
     * Typed get/set over {@link SettingsManager}'s existing fields, so a future internal refactor
     * of its storage can't silently break a companion mod reaching in via raw field access.
     */
    public static final class Settings {
        private Settings() {}

        /** Called whenever any setting changes (via this API, the config screen, or /hev commands). */
        public static void registerChangeListener(Runnable listener) {
            SettingsManager.registerChangeListener(listener);
        }

        public static boolean isHevSuitEnabled() { return SettingsManager.hevSuitEnabled; }
        public static void setHevSuitEnabled(boolean v) { SettingsManager.hevSuitEnabled = v; SettingsManager.saveSettings(); }

        public static boolean isArmorDurabilityEnabled() { return SettingsManager.armorDurabilityEnabled; }
        public static void setArmorDurabilityEnabled(boolean v) { SettingsManager.armorDurabilityEnabled = v; SettingsManager.saveSettings(); }

        public static boolean isHealthAlertsEnabled() { return SettingsManager.healthAlertsEnabled; }
        public static void setHealthAlertsEnabled(boolean v) { SettingsManager.healthAlertsEnabled = v; SettingsManager.saveSettings(); }

        public static boolean isHudEnabled() { return SettingsManager.hudEnabled; }
        public static void setHudEnabled(boolean v) { SettingsManager.hudEnabled = v; SettingsManager.saveSettings(); }

        public static boolean isFracturesEnabled() { return SettingsManager.fracturesEnabled; }
        public static void setFracturesEnabled(boolean v) { SettingsManager.fracturesEnabled = v; SettingsManager.saveSettings(); }

        public static boolean isHeatDamageEnabled() { return SettingsManager.heatDamageEnabled; }
        public static void setHeatDamageEnabled(boolean v) { SettingsManager.heatDamageEnabled = v; SettingsManager.saveSettings(); }

        public static boolean isBloodLossEnabled() { return SettingsManager.bloodLossEnabled; }
        public static void setBloodLossEnabled(boolean v) { SettingsManager.bloodLossEnabled = v; SettingsManager.saveSettings(); }

        public static boolean isShockDamageEnabled() { return SettingsManager.shockDamageEnabled; }
        public static void setShockDamageEnabled(boolean v) { SettingsManager.shockDamageEnabled = v; SettingsManager.saveSettings(); }

        public static boolean isChemicalDamageEnabled() { return SettingsManager.chemicalDamageEnabled; }
        public static void setChemicalDamageEnabled(boolean v) { SettingsManager.chemicalDamageEnabled = v; SettingsManager.saveSettings(); }

        public static boolean isPvpModeEnabled() { return SettingsManager.pvpModeEnabled; }
        public static void setPvpModeEnabled(boolean v) { SettingsManager.pvpModeEnabled = v; SettingsManager.saveSettings(); }

        public static boolean isMorphineEnabled() { return SettingsManager.morphineEnabled; }
        public static void setMorphineEnabled(boolean v) { SettingsManager.morphineEnabled = v; SettingsManager.saveSettings(); }

        public static boolean isHealthCritical2Enabled() { return SettingsManager.healthCritical2Enabled; }
        public static void setHealthCritical2Enabled(boolean v) { SettingsManager.healthCritical2Enabled = v; SettingsManager.saveSettings(); }

        public static boolean isSeekMedicalEnabled() { return SettingsManager.seekMedicalEnabled; }
        public static void setSeekMedicalEnabled(boolean v) { SettingsManager.seekMedicalEnabled = v; SettingsManager.saveSettings(); }

        public static boolean isHealthCriticalEnabled() { return SettingsManager.healthCriticalEnabled; }
        public static void setHealthCriticalEnabled(boolean v) { SettingsManager.healthCriticalEnabled = v; SettingsManager.saveSettings(); }

        public static boolean isNearDeathEnabled() { return SettingsManager.nearDeathEnabled; }
        public static void setNearDeathEnabled(boolean v) { SettingsManager.nearDeathEnabled = v; SettingsManager.saveSettings(); }

        public static boolean isCaptionsEnabled() { return SettingsManager.captionsEnabled; }
        public static void setCaptionsEnabled(boolean v) { SettingsManager.captionsEnabled = v; SettingsManager.saveSettings(); }

        public static boolean isDamageIndicatorsEnabled() { return SettingsManager.damageIndicatorsEnabled; }
        public static void setDamageIndicatorsEnabled(boolean v) { SettingsManager.damageIndicatorsEnabled = v; SettingsManager.saveSettings(); }

        public static boolean isHudHealthEnabled() { return SettingsManager.hudHealthEnabled; }
        public static void setHudHealthEnabled(boolean v) { SettingsManager.hudHealthEnabled = v; SettingsManager.saveSettings(); }

        public static boolean isHudArmorEnabled() { return SettingsManager.hudArmorEnabled; }
        public static void setHudArmorEnabled(boolean v) { SettingsManager.hudArmorEnabled = v; SettingsManager.saveSettings(); }

        public static boolean isHudAmmoEnabled() { return SettingsManager.hudAmmoEnabled; }
        public static void setHudAmmoEnabled(boolean v) { SettingsManager.hudAmmoEnabled = v; SettingsManager.saveSettings(); }

        public static boolean isHudFlashlightEnabled() { return SettingsManager.hudFlashlightEnabled; }
        public static void setHudFlashlightEnabled(boolean v) { SettingsManager.hudFlashlightEnabled = v; SettingsManager.saveSettings(); }

        public static boolean isThreatIndicatorsEnabled() { return SettingsManager.threatIndicatorsEnabled; }
        public static void setThreatIndicatorsEnabled(boolean v) { SettingsManager.threatIndicatorsEnabled = v; SettingsManager.saveSettings(); }

        public static boolean isHudAlignmentMode() { return SettingsManager.hudAlignmentMode; }
        public static void setHudAlignmentMode(boolean v) { SettingsManager.hudAlignmentMode = v; SettingsManager.saveSettings(); }

        public static boolean isInsufficientMedicalEnabled() { return SettingsManager.insufficientMedicalEnabled; }
        public static void setInsufficientMedicalEnabled(boolean v) { SettingsManager.insufficientMedicalEnabled = v; SettingsManager.saveSettings(); }

        public static boolean isHevDamageEnabled() { return SettingsManager.hevDamageEnabled; }
        public static void setHevDamageEnabled(boolean v) { SettingsManager.hevDamageEnabled = v; SettingsManager.saveSettings(); }

        public static boolean isPowerArmorOverloadEnabled() { return SettingsManager.powerArmorOverloadEnabled; }
        public static void setPowerArmorOverloadEnabled(boolean v) { SettingsManager.powerArmorOverloadEnabled = v; SettingsManager.saveSettings(); }

        public static boolean isAdministeringMedicalEnabled() { return SettingsManager.administeringMedicalEnabled; }
        public static void setAdministeringMedicalEnabled(boolean v) { SettingsManager.administeringMedicalEnabled = v; SettingsManager.saveSettings(); }

        public static boolean isDeathSfxEnabled() { return SettingsManager.deathSfxEnabled; }
        public static void setDeathSfxEnabled(boolean v) { SettingsManager.deathSfxEnabled = v; SettingsManager.saveSettings(); }

        public static boolean isWeaponPickupEnabled() { return SettingsManager.weaponPickupEnabled; }
        public static void setWeaponPickupEnabled(boolean v) { SettingsManager.weaponPickupEnabled = v; SettingsManager.saveSettings(); }

        public static boolean isAmmoDepletedEnabled() { return SettingsManager.ammoDepletedEnabled; }
        public static void setAmmoDepletedEnabled(boolean v) { SettingsManager.ammoDepletedEnabled = v; SettingsManager.saveSettings(); }

        public static boolean isHevLogonEnabled() { return SettingsManager.hevLogonEnabled; }
        public static void setHevLogonEnabled(boolean v) { SettingsManager.hevLogonEnabled = v; SettingsManager.saveSettings(); }

        public static boolean isElytraEquipSfxEnabled() { return SettingsManager.elytraEquipSfxEnabled; }
        public static void setElytraEquipSfxEnabled(boolean v) { SettingsManager.elytraEquipSfxEnabled = v; SettingsManager.saveSettings(); }

        public static boolean isInternalBleedingEnabled() { return SettingsManager.internalBleedingEnabled; }
        public static void setInternalBleedingEnabled(boolean v) { SettingsManager.internalBleedingEnabled = v; SettingsManager.saveSettings(); }

        public static boolean isRadiationSfxEnabled() { return SettingsManager.radiationSfxEnabled; }
        public static void setRadiationSfxEnabled(boolean v) { SettingsManager.radiationSfxEnabled = v; SettingsManager.saveSettings(); }

        public static boolean isHl2FlashlightEnabled() { return SettingsManager.hl2FlashlightEnabled; }
        public static void setHl2FlashlightEnabled(boolean v) { SettingsManager.hl2FlashlightEnabled = v; SettingsManager.saveSettings(); }

        public static boolean isChattySuitEnabled() { return SettingsManager.chattySuitEnabled; }
        public static void setChattySuitEnabled(boolean v) { SettingsManager.chattySuitEnabled = v; SettingsManager.saveSettings(); }

        public static int getHudPrimaryColor() { return SettingsManager.hudPrimaryColor; }
        public static void setHudPrimaryColor(int argb) { SettingsManager.hudPrimaryColor = argb; SettingsManager.saveSettings(); }

        public static int getHudSecondaryColor() { return SettingsManager.hudSecondaryColor; }
        public static void setHudSecondaryColor(int argb) { SettingsManager.hudSecondaryColor = argb; SettingsManager.saveSettings(); }

        public static float getVolumeMultiplier() { return SettingsManager.hevVolumeMul; }
        public static void setVolumeMultiplier(float v) { SettingsManager.hevVolumeMul = Math.max(0f, Math.min(1f, v)); SettingsManager.saveSettings(); }

        public static List<String> getWeaponKeywords() { return List.copyOf(SettingsManager.weaponKeywords); }
        public static void setWeaponKeywords(List<String> keywords) {
            SettingsManager.weaponKeywords = keywords == null ? new java.util.ArrayList<>() : new java.util.ArrayList<>(keywords);
            SettingsManager.saveSettings();
        }
    }
}
