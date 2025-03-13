package ltown.hev_suit.client.managers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class SettingsManager {
    private static final Logger LOGGER = LogManager.getLogger("SettingsManager");
    private static final File CONFIG_FILE = new File("config/hev_suit_settings.json");
    private static final Gson GSON = new Gson();

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
    public static boolean useBlackMesaSFX = false;
    public static boolean captionsEnabled = false;
    public static boolean damageIndicatorsEnabled = true;
    public static boolean hudHealthEnabled = true;
    public static boolean hudArmorEnabled = true;
    public static boolean hudAmmoEnabled = true;
    public static boolean threatIndicatorsEnabled = false;
    
    public static int hudPrimaryColor = 0xFFFFAE00; 
    public static int hudSecondaryColor = 0xFF8B5E00; 

    public static void loadSettings() {
        if (CONFIG_FILE.exists()) {
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
                morphineEnabled = getOrDefault(json, "morphineEnabled", true);
                healthCritical2Enabled = getOrDefault(json, "healthCritical2Enabled", true);
                seekMedicalEnabled = getOrDefault(json, "seekMedicalEnabled", true);
                healthCriticalEnabled = getOrDefault(json, "healthCriticalEnabled", true);
                nearDeathEnabled = getOrDefault(json, "nearDeathEnabled", true);
                useBlackMesaSFX = getOrDefault(json, "useBlackMesaSFX", false);
                captionsEnabled = getOrDefault(json, "captionsEnabled", false);
                damageIndicatorsEnabled = getOrDefault(json, "damageIndicatorsEnabled", true);
                hudHealthEnabled = getOrDefault(json, "hudHealthEnabled", true);
                hudArmorEnabled = getOrDefault(json, "hudArmorEnabled", true);
                hudAmmoEnabled = getOrDefault(json, "hudAmmoEnabled", true);
                threatIndicatorsEnabled = getOrDefault(json, "threatIndicatorsEnabled", false);
                hudPrimaryColor = getOrDefaultInt(json, "hudPrimaryColor", 0xFFFFAE00);
                hudSecondaryColor = getOrDefaultInt(json, "hudSecondaryColor", 0xFF8B5E00);
                LOGGER.debug("Settings loaded: useBlackMesaSFX = " + useBlackMesaSFX);
            } catch (IOException e) {
                LOGGER.error("Failed to load settings", e);
            }
        }
    }

    private static boolean getOrDefault(JsonObject json, String key, boolean defaultValue) {
        return json.has(key) ? json.get(key).getAsBoolean() : defaultValue;
    }
    
    // New helper method to get integer values with default
    private static int getOrDefaultInt(JsonObject json, String key, int defaultValue) {
        return json.has(key) ? json.get(key).getAsInt() : defaultValue;
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
        json.addProperty("morphineEnabled", morphineEnabled);
        json.addProperty("healthCritical2Enabled", healthCritical2Enabled);
        json.addProperty("seekMedicalEnabled", seekMedicalEnabled);
        json.addProperty("healthCriticalEnabled", healthCriticalEnabled);
        json.addProperty("nearDeathEnabled", nearDeathEnabled);
        json.addProperty("useBlackMesaSFX", useBlackMesaSFX);
        json.addProperty("captionsEnabled", captionsEnabled);
        json.addProperty("damageIndicatorsEnabled", damageIndicatorsEnabled);
        json.addProperty("hudHealthEnabled", hudHealthEnabled);
        json.addProperty("hudArmorEnabled", hudArmorEnabled);
        json.addProperty("hudAmmoEnabled", hudAmmoEnabled);
        json.addProperty("threatIndicatorsEnabled", threatIndicatorsEnabled);
        // Save custom color settings
        json.addProperty("hudPrimaryColor", hudPrimaryColor);
        json.addProperty("hudSecondaryColor", hudSecondaryColor);

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
