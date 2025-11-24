package ltown.hev_suit.client.managers;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SettingsManager {
    private static final Logger LOGGER = LogManager.getLogger("SettingsManager");
    private static final File CONFIG_FILE = new File("config/hev_suit_settings.json");
    private static final Gson GSON = new Gson();
    private static final List<String> DEFAULT_WEAPON_KEYWORDS = List.of("sword", "bow", "crossbow", "trident", "mace");
    private static final boolean DEFAULT_HEV_SUIT_ENABLED = true;
    private static final boolean DEFAULT_ARMOR_DURABILITY_ENABLED = true;
    private static final boolean DEFAULT_HEALTH_ALERTS_ENABLED = true;
    private static final boolean DEFAULT_HUD_ENABLED = true;
    private static final boolean DEFAULT_FRACTURES_ENABLED = true;
    private static final boolean DEFAULT_HEAT_DAMAGE_ENABLED = true;
    private static final boolean DEFAULT_BLOOD_LOSS_ENABLED = true;
    private static final boolean DEFAULT_SHOCK_DAMAGE_ENABLED = true;
    private static final boolean DEFAULT_CHEMICAL_DAMAGE_ENABLED = true;
    private static final boolean DEFAULT_PVP_MODE_ENABLED = false;
    private static final boolean DEFAULT_MORPHINE_ENABLED = true;
    private static final boolean DEFAULT_HEALTH_CRITICAL_2_ENABLED = true;
    private static final boolean DEFAULT_SEEK_MEDICAL_ENABLED = true;
    private static final boolean DEFAULT_HEALTH_CRITICAL_ENABLED = true;
    private static final boolean DEFAULT_NEAR_DEATH_ENABLED = true;
    private static final boolean DEFAULT_CAPTIONS_ENABLED = false;
    private static final boolean DEFAULT_DAMAGE_INDICATORS_ENABLED = true;
    private static final boolean DEFAULT_HUD_HEALTH_ENABLED = true;
    private static final boolean DEFAULT_HUD_ARMOR_ENABLED = true;
    private static final boolean DEFAULT_HUD_AMMO_ENABLED = true;
    private static final boolean DEFAULT_THREAT_INDICATORS_ENABLED = false;
    private static final boolean DEFAULT_HUD_ALIGNMENT_MODE = false;
    private static final boolean DEFAULT_INSUFFICIENT_MEDICAL_ENABLED = false;
    private static final boolean DEFAULT_HEV_DAMAGE_ENABLED = false;
    private static final boolean DEFAULT_POWER_ARMOR_OVERLOAD_ENABLED = false;
    private static final boolean DEFAULT_ADMINISTERING_MEDICAL_ENABLED = false;
    private static final boolean DEFAULT_DEATH_SFX_ENABLED = true;
    private static final boolean DEFAULT_WEAPON_PICKUP_ENABLED = false;
    private static final boolean DEFAULT_AMMO_DEPLETED_ENABLED = true;
    private static final boolean DEFAULT_HEV_LOGON_ENABLED = true;
    private static final boolean DEFAULT_ELYTRA_EQUIP_SFX_ENABLED = false;
    private static final boolean DEFAULT_INTERNAL_BLEEDING_ENABLED = true;
    private static final boolean DEFAULT_RADIATION_SFX_ENABLED = true;
    private static final int DEFAULT_HUD_PRIMARY_COLOR = 0xFFFFAE00;
    private static final int DEFAULT_HUD_SECONDARY_COLOR = 0xFF8B5E00;

    public static boolean hevSuitEnabled = DEFAULT_HEV_SUIT_ENABLED;
    public static boolean armorDurabilityEnabled = DEFAULT_ARMOR_DURABILITY_ENABLED;
    public static boolean healthAlertsEnabled = DEFAULT_HEALTH_ALERTS_ENABLED;
    public static boolean hudEnabled = DEFAULT_HUD_ENABLED;
    public static boolean fracturesEnabled = DEFAULT_FRACTURES_ENABLED;
    public static boolean heatDamageEnabled = DEFAULT_HEAT_DAMAGE_ENABLED;
    public static boolean bloodLossEnabled = DEFAULT_BLOOD_LOSS_ENABLED;
    public static boolean shockDamageEnabled = DEFAULT_SHOCK_DAMAGE_ENABLED;
    public static boolean chemicalDamageEnabled = DEFAULT_CHEMICAL_DAMAGE_ENABLED;
    public static boolean pvpModeEnabled = DEFAULT_PVP_MODE_ENABLED;
    public static boolean morphineEnabled = DEFAULT_MORPHINE_ENABLED;
    public static boolean healthCritical2Enabled = DEFAULT_HEALTH_CRITICAL_2_ENABLED;
    public static boolean seekMedicalEnabled = DEFAULT_SEEK_MEDICAL_ENABLED;
    public static boolean healthCriticalEnabled = DEFAULT_HEALTH_CRITICAL_ENABLED;
    public static boolean nearDeathEnabled = DEFAULT_NEAR_DEATH_ENABLED;
    public static boolean captionsEnabled = DEFAULT_CAPTIONS_ENABLED;
    public static boolean damageIndicatorsEnabled = DEFAULT_DAMAGE_INDICATORS_ENABLED;
    public static boolean hudHealthEnabled = DEFAULT_HUD_HEALTH_ENABLED;
    public static boolean hudArmorEnabled = DEFAULT_HUD_ARMOR_ENABLED;
    public static boolean hudAmmoEnabled = DEFAULT_HUD_AMMO_ENABLED;
    public static boolean threatIndicatorsEnabled = DEFAULT_THREAT_INDICATORS_ENABLED;
    public static boolean hudAlignmentMode = DEFAULT_HUD_ALIGNMENT_MODE; // render all assets centered for offset tuning
    public static boolean insufficientMedicalEnabled = DEFAULT_INSUFFICIENT_MEDICAL_ENABLED;
    public static boolean hevDamageEnabled = DEFAULT_HEV_DAMAGE_ENABLED;
    public static boolean powerArmorOverloadEnabled = DEFAULT_POWER_ARMOR_OVERLOAD_ENABLED;
    public static boolean administeringMedicalEnabled = DEFAULT_ADMINISTERING_MEDICAL_ENABLED;
    public static boolean deathSfxEnabled = DEFAULT_DEATH_SFX_ENABLED;
    public static boolean weaponPickupEnabled = DEFAULT_WEAPON_PICKUP_ENABLED;
    public static boolean ammoDepletedEnabled = DEFAULT_AMMO_DEPLETED_ENABLED;
    public static boolean hevLogonEnabled = DEFAULT_HEV_LOGON_ENABLED;
    public static boolean elytraEquipSfxEnabled = DEFAULT_ELYTRA_EQUIP_SFX_ENABLED;
    public static boolean internalBleedingEnabled = DEFAULT_INTERNAL_BLEEDING_ENABLED;
    public static boolean radiationSfxEnabled = DEFAULT_RADIATION_SFX_ENABLED;

    public static int hudPrimaryColor = DEFAULT_HUD_PRIMARY_COLOR;
    public static int hudSecondaryColor = DEFAULT_HUD_SECONDARY_COLOR;
    public static List<String> weaponKeywords = new ArrayList<>(DEFAULT_WEAPON_KEYWORDS);

    public static void loadSettings() {
        if (!CONFIG_FILE.exists()) {
            return;
        }
        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            hevSuitEnabled = getOrDefault(json, "hevSuitEnabled", DEFAULT_HEV_SUIT_ENABLED);
            armorDurabilityEnabled = getOrDefault(json, "armorDurabilityEnabled", DEFAULT_ARMOR_DURABILITY_ENABLED);
            healthAlertsEnabled = getOrDefault(json, "healthAlertsEnabled", DEFAULT_HEALTH_ALERTS_ENABLED);
            hudEnabled = getOrDefault(json, "hudEnabled", DEFAULT_HUD_ENABLED);
            fracturesEnabled = getOrDefault(json, "fracturesEnabled", DEFAULT_FRACTURES_ENABLED);
            heatDamageEnabled = getOrDefault(json, "heatDamageEnabled", DEFAULT_HEAT_DAMAGE_ENABLED);
            bloodLossEnabled = getOrDefault(json, "bloodLossEnabled", DEFAULT_BLOOD_LOSS_ENABLED);
            shockDamageEnabled = getOrDefault(json, "shockDamageEnabled", DEFAULT_SHOCK_DAMAGE_ENABLED);
            chemicalDamageEnabled = getOrDefault(json, "chemicalDamageEnabled", DEFAULT_CHEMICAL_DAMAGE_ENABLED);
            pvpModeEnabled = getOrDefault(json, "pvpModeEnabled", DEFAULT_PVP_MODE_ENABLED);
            morphineEnabled = getOrDefault(json, "morphineEnabled", DEFAULT_MORPHINE_ENABLED);
            healthCritical2Enabled = getOrDefault(json, "healthCritical2Enabled", DEFAULT_HEALTH_CRITICAL_2_ENABLED);
            seekMedicalEnabled = getOrDefault(json, "seekMedicalEnabled", DEFAULT_SEEK_MEDICAL_ENABLED);
            healthCriticalEnabled = getOrDefault(json, "healthCriticalEnabled", DEFAULT_HEALTH_CRITICAL_ENABLED);
            nearDeathEnabled = getOrDefault(json, "nearDeathEnabled", DEFAULT_NEAR_DEATH_ENABLED);
            captionsEnabled = getOrDefault(json, "captionsEnabled", DEFAULT_CAPTIONS_ENABLED);
            damageIndicatorsEnabled = getOrDefault(json, "damageIndicatorsEnabled", DEFAULT_DAMAGE_INDICATORS_ENABLED);
            hudHealthEnabled = getOrDefault(json, "hudHealthEnabled", DEFAULT_HUD_HEALTH_ENABLED);
            hudArmorEnabled = getOrDefault(json, "hudArmorEnabled", DEFAULT_HUD_ARMOR_ENABLED);
            hudAmmoEnabled = getOrDefault(json, "hudAmmoEnabled", DEFAULT_HUD_AMMO_ENABLED);
            threatIndicatorsEnabled = getOrDefault(json, "threatIndicatorsEnabled", DEFAULT_THREAT_INDICATORS_ENABLED);
            hudAlignmentMode = getOrDefault(json, "hudAlignmentMode", DEFAULT_HUD_ALIGNMENT_MODE);
            insufficientMedicalEnabled = getOrDefault(json, "insufficientMedicalEnabled", DEFAULT_INSUFFICIENT_MEDICAL_ENABLED);
            hevDamageEnabled = getOrDefault(json, "hevDamageEnabled", DEFAULT_HEV_DAMAGE_ENABLED);
            powerArmorOverloadEnabled = getOrDefault(json, "powerArmorOverloadEnabled", DEFAULT_POWER_ARMOR_OVERLOAD_ENABLED);
            administeringMedicalEnabled = getOrDefault(json, "administeringMedicalEnabled", DEFAULT_ADMINISTERING_MEDICAL_ENABLED);
            deathSfxEnabled = getOrDefault(json, "deathSfxEnabled", DEFAULT_DEATH_SFX_ENABLED);
            weaponPickupEnabled = getOrDefault(json, "weaponPickupEnabled", DEFAULT_WEAPON_PICKUP_ENABLED);
            ammoDepletedEnabled = getOrDefault(json, "ammoDepletedEnabled", DEFAULT_AMMO_DEPLETED_ENABLED);
            hevLogonEnabled = getOrDefault(json, "hevLogonEnabled", DEFAULT_HEV_LOGON_ENABLED);
            elytraEquipSfxEnabled = getOrDefault(json, "elytraEquipSfxEnabled", DEFAULT_ELYTRA_EQUIP_SFX_ENABLED);
            internalBleedingEnabled = getOrDefault(json, "internalBleedingEnabled", DEFAULT_INTERNAL_BLEEDING_ENABLED);
            radiationSfxEnabled = getOrDefault(json, "radiationSfxEnabled", DEFAULT_RADIATION_SFX_ENABLED);
            hudPrimaryColor = getOrDefaultInt(json, "hudPrimaryColor", DEFAULT_HUD_PRIMARY_COLOR);
            hudSecondaryColor = getOrDefaultInt(json, "hudSecondaryColor", DEFAULT_HUD_SECONDARY_COLOR);
            weaponKeywords = getStringList(json, "weaponKeywords", DEFAULT_WEAPON_KEYWORDS);
        } catch (IOException e) {
            LOGGER.error("Failed to load settings", e);
        }
    }

    private static boolean getOrDefault(JsonObject json, String key, boolean defaultValue) {
        return json.has(key) ? json.get(key).getAsBoolean() : defaultValue;
    }

    private static int getOrDefaultInt(JsonObject json, String key, int defaultValue) {
        return json.has(key) ? json.get(key).getAsInt() : defaultValue;
    }

    private static List<String> getStringList(JsonObject json, String key, List<String> defaultValue) {
        if (!json.has(key)) {
            return new ArrayList<>(defaultValue);
        }
        JsonElement element = json.get(key);
        List<String> result = new ArrayList<>();
        if (element.isJsonArray()) {
            for (JsonElement entry : element.getAsJsonArray()) {
                if (entry.isJsonPrimitive()) {
                    String value = entry.getAsString().trim().toLowerCase();
                    if (!value.isEmpty()) {
                        result.add(value);
                    }
                }
            }
        } else if (element.isJsonPrimitive()) {
            String[] parts = element.getAsString().split(",");
            for (String part : parts) {
                String value = part.trim().toLowerCase();
                if (!value.isEmpty()) {
                    result.add(value);
                }
            }
        }
        if (result.isEmpty()) {
            result.addAll(defaultValue);
        }
        return result;
    }

    public static void saveSettings() {
        JsonObject json = new JsonObject();
        json.addProperty("hevSuitEnabled", hevSuitEnabled);
        json.addProperty("armorDurabilityEnabled", armorDurabilityEnabled);
        json.addProperty("healthAlertsEnabled", healthAlertsEnabled);
        json.addProperty("hudEnabled", hudEnabled);
        json.addProperty("fracturesEnabled", fracturesEnabled);
        json.addProperty("heatDamageEnabled", heatDamageEnabled);
        json.addProperty("bloodLossEnabled", bloodLossEnabled);
        json.addProperty("shockDamageEnabled", shockDamageEnabled);
        json.addProperty("chemicalDamageEnabled", chemicalDamageEnabled);
        json.addProperty("pvpModeEnabled", pvpModeEnabled);
        json.addProperty("morphineEnabled", morphineEnabled);
        json.addProperty("healthCritical2Enabled", healthCritical2Enabled);
        json.addProperty("seekMedicalEnabled", seekMedicalEnabled);
        json.addProperty("healthCriticalEnabled", healthCriticalEnabled);
        json.addProperty("nearDeathEnabled", nearDeathEnabled);
        json.addProperty("captionsEnabled", captionsEnabled);
        json.addProperty("damageIndicatorsEnabled", damageIndicatorsEnabled);
        json.addProperty("hudHealthEnabled", hudHealthEnabled);
        json.addProperty("hudArmorEnabled", hudArmorEnabled);
        json.addProperty("hudAmmoEnabled", hudAmmoEnabled);
        json.addProperty("threatIndicatorsEnabled", threatIndicatorsEnabled);
        json.addProperty("hudAlignmentMode", hudAlignmentMode);
        json.addProperty("insufficientMedicalEnabled", insufficientMedicalEnabled);
        json.addProperty("hevDamageEnabled", hevDamageEnabled);
        json.addProperty("powerArmorOverloadEnabled", powerArmorOverloadEnabled);
        json.addProperty("administeringMedicalEnabled", administeringMedicalEnabled);
        json.addProperty("deathSfxEnabled", deathSfxEnabled);
        json.addProperty("weaponPickupEnabled", weaponPickupEnabled);
        json.addProperty("ammoDepletedEnabled", ammoDepletedEnabled);
        json.addProperty("hevLogonEnabled", hevLogonEnabled);
        json.addProperty("elytraEquipSfxEnabled", elytraEquipSfxEnabled);
        json.addProperty("internalBleedingEnabled", internalBleedingEnabled);
        json.addProperty("radiationSfxEnabled", radiationSfxEnabled);
        json.addProperty("hudPrimaryColor", hudPrimaryColor);
        json.addProperty("hudSecondaryColor", hudSecondaryColor);

        JsonArray keywordArray = new JsonArray();
        for (String keyword : weaponKeywords) {
            keywordArray.add(keyword);
        }
        json.add("weaponKeywords", keywordArray);

        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(json, writer);
        } catch (IOException e) {
            LOGGER.error("Failed to save settings", e);
        }
    }

    public static void resetMainToggles() {
        hevSuitEnabled = DEFAULT_HEV_SUIT_ENABLED;
        pvpModeEnabled = DEFAULT_PVP_MODE_ENABLED;
        captionsEnabled = DEFAULT_CAPTIONS_ENABLED;
    }

    public static void resetHudToggles() {
        hudEnabled = DEFAULT_HUD_ENABLED;
        hudHealthEnabled = DEFAULT_HUD_HEALTH_ENABLED;
        hudArmorEnabled = DEFAULT_HUD_ARMOR_ENABLED;
        hudAmmoEnabled = DEFAULT_HUD_AMMO_ENABLED;
        damageIndicatorsEnabled = DEFAULT_DAMAGE_INDICATORS_ENABLED;
        threatIndicatorsEnabled = DEFAULT_THREAT_INDICATORS_ENABLED;
    }

    public static void resetHudColors() {
        hudPrimaryColor = DEFAULT_HUD_PRIMARY_COLOR;
        hudSecondaryColor = DEFAULT_HUD_SECONDARY_COLOR;
    }

    public static void resetAudibleAlerts() {
        fracturesEnabled = DEFAULT_FRACTURES_ENABLED;
        bloodLossEnabled = DEFAULT_BLOOD_LOSS_ENABLED;
        morphineEnabled = DEFAULT_MORPHINE_ENABLED;
        armorDurabilityEnabled = DEFAULT_ARMOR_DURABILITY_ENABLED;
        heatDamageEnabled = DEFAULT_HEAT_DAMAGE_ENABLED;
        shockDamageEnabled = DEFAULT_SHOCK_DAMAGE_ENABLED;
        chemicalDamageEnabled = DEFAULT_CHEMICAL_DAMAGE_ENABLED;
        hevDamageEnabled = DEFAULT_HEV_DAMAGE_ENABLED;
        powerArmorOverloadEnabled = DEFAULT_POWER_ARMOR_OVERLOAD_ENABLED;
        hevLogonEnabled = DEFAULT_HEV_LOGON_ENABLED;
        elytraEquipSfxEnabled = DEFAULT_ELYTRA_EQUIP_SFX_ENABLED;
        radiationSfxEnabled = DEFAULT_RADIATION_SFX_ENABLED;
    }

    public static void resetHealthAlerts() {
        healthAlertsEnabled = DEFAULT_HEALTH_ALERTS_ENABLED;
        healthCritical2Enabled = DEFAULT_HEALTH_CRITICAL_2_ENABLED;
        seekMedicalEnabled = DEFAULT_SEEK_MEDICAL_ENABLED;
        healthCriticalEnabled = DEFAULT_HEALTH_CRITICAL_ENABLED;
        nearDeathEnabled = DEFAULT_NEAR_DEATH_ENABLED;
        insufficientMedicalEnabled = DEFAULT_INSUFFICIENT_MEDICAL_ENABLED;
        administeringMedicalEnabled = DEFAULT_ADMINISTERING_MEDICAL_ENABLED;
        deathSfxEnabled = DEFAULT_DEATH_SFX_ENABLED;
        internalBleedingEnabled = DEFAULT_INTERNAL_BLEEDING_ENABLED;
    }

    public static void resetWeaponAlerts() {
        weaponPickupEnabled = DEFAULT_WEAPON_PICKUP_ENABLED;
        ammoDepletedEnabled = DEFAULT_AMMO_DEPLETED_ENABLED;
        weaponKeywords = new ArrayList<>(DEFAULT_WEAPON_KEYWORDS);
    }

    // Calculate a darker shade of a color for secondary elements
    public static int calculateDarkerShade(int color) {
        float darkenFactor = 0.65f; // How much darker to make the color

        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        r = Math.max(0, Math.round(r * darkenFactor));
        g = Math.max(0, Math.round(g * darkenFactor));
        b = Math.max(0, Math.round(b * darkenFactor));

        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
