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

    public static boolean hevSuitEnabled = true;
    public static boolean armorDurabilityEnabled = true;
    public static boolean healthAlertsEnabled = true;
    public static boolean hudEnabled = true;
    public static boolean fracturesEnabled = true;
    public static boolean heatDamageEnabled = true;
    public static boolean bloodLossEnabled = true;
    public static boolean shockDamageEnabled = true;
    public static boolean chemicalDamageEnabled = true;
    public static boolean pvpModeEnabled = false;
    public static boolean morphineEnabled = true;
    public static boolean healthCritical2Enabled = true;
    public static boolean seekMedicalEnabled = true;
    public static boolean healthCriticalEnabled = true;
    public static boolean nearDeathEnabled = true;
    public static boolean captionsEnabled = false;
    public static boolean damageIndicatorsEnabled = true;
    public static boolean hudHealthEnabled = true;
    public static boolean hudArmorEnabled = true;
    public static boolean hudAmmoEnabled = true;
    public static boolean threatIndicatorsEnabled = false;
    public static boolean hudAlignmentMode = false; // render all assets centered for offset tuning
    public static boolean insufficientMedicalEnabled = true;
    public static boolean hevDamageEnabled = true;
    public static boolean powerArmorOverloadEnabled = true;
    public static boolean administeringMedicalEnabled = true;
    public static boolean deathSfxEnabled = true;
    public static boolean weaponPickupEnabled = true;
    public static boolean ammoDepletedEnabled = true;
    public static boolean hevLogonEnabled = true;
    public static boolean elytraEquipSfxEnabled = true;
    public static boolean internalBleedingEnabled = true;
    public static boolean radiationSfxEnabled = true;

    public static int hudPrimaryColor = 0xFFFFAE00;
    public static int hudSecondaryColor = 0xFF8B5E00;
    public static List<String> weaponKeywords = new ArrayList<>(DEFAULT_WEAPON_KEYWORDS);

    public static void loadSettings() {
        if (!CONFIG_FILE.exists()) {
            return;
        }
        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            hevSuitEnabled = getOrDefault(json, "hevSuitEnabled", true);
            armorDurabilityEnabled = getOrDefault(json, "armorDurabilityEnabled", true);
            healthAlertsEnabled = getOrDefault(json, "healthAlertsEnabled", true);
            hudEnabled = getOrDefault(json, "hudEnabled", true);
            fracturesEnabled = getOrDefault(json, "fracturesEnabled", true);
            heatDamageEnabled = getOrDefault(json, "heatDamageEnabled", true);
            bloodLossEnabled = getOrDefault(json, "bloodLossEnabled", true);
            shockDamageEnabled = getOrDefault(json, "shockDamageEnabled", true);
            chemicalDamageEnabled = getOrDefault(json, "chemicalDamageEnabled", true);
            pvpModeEnabled = getOrDefault(json, "pvpModeEnabled", false);
            morphineEnabled = getOrDefault(json, "morphineEnabled", true);
            healthCritical2Enabled = getOrDefault(json, "healthCritical2Enabled", true);
            seekMedicalEnabled = getOrDefault(json, "seekMedicalEnabled", true);
            healthCriticalEnabled = getOrDefault(json, "healthCriticalEnabled", true);
            nearDeathEnabled = getOrDefault(json, "nearDeathEnabled", true);
            captionsEnabled = getOrDefault(json, "captionsEnabled", false);
            damageIndicatorsEnabled = getOrDefault(json, "damageIndicatorsEnabled", true);
            hudHealthEnabled = getOrDefault(json, "hudHealthEnabled", true);
            hudArmorEnabled = getOrDefault(json, "hudArmorEnabled", true);
            hudAmmoEnabled = getOrDefault(json, "hudAmmoEnabled", true);
            threatIndicatorsEnabled = getOrDefault(json, "threatIndicatorsEnabled", false);
            hudAlignmentMode = getOrDefault(json, "hudAlignmentMode", false);
            insufficientMedicalEnabled = getOrDefault(json, "insufficientMedicalEnabled", true);
            hevDamageEnabled = getOrDefault(json, "hevDamageEnabled", true);
            powerArmorOverloadEnabled = getOrDefault(json, "powerArmorOverloadEnabled", true);
            administeringMedicalEnabled = getOrDefault(json, "administeringMedicalEnabled", true);
            deathSfxEnabled = getOrDefault(json, "deathSfxEnabled", true);
            weaponPickupEnabled = getOrDefault(json, "weaponPickupEnabled", true);
            ammoDepletedEnabled = getOrDefault(json, "ammoDepletedEnabled", true);
            hevLogonEnabled = getOrDefault(json, "hevLogonEnabled", true);
            elytraEquipSfxEnabled = getOrDefault(json, "elytraEquipSfxEnabled", true);
            internalBleedingEnabled = getOrDefault(json, "internalBleedingEnabled", true);
            radiationSfxEnabled = getOrDefault(json, "radiationSfxEnabled", true);
            hudPrimaryColor = getOrDefaultInt(json, "hudPrimaryColor", 0xFFFFAE00);
            hudSecondaryColor = getOrDefaultInt(json, "hudSecondaryColor", 0xFF8B5E00);
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
