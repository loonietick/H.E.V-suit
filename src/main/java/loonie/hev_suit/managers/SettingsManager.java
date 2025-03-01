package loonie.hev_suit.managers;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraftforge.fml.common.Loader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class SettingsManager {
    private static final Logger LOGGER = LogManager.getLogger("SettingsManager");
    private static final File CONFIG_FILE = new File(Loader.instance().getConfigDir(), "hev_suit_settings.json");
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

    public static void loadSettings() {
        if (!CONFIG_FILE.getParentFile().exists()) {
            CONFIG_FILE.getParentFile().mkdirs();
        }
        
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
            } catch (IOException e) {
                LOGGER.error("Failed to load settings", e);
            }
        } else {
            saveSettings(); // Create default settings file if it doesn't exist
        }
    }
    
    // Add getOrDefault helper method
    private static boolean getOrDefault(JsonObject json, String key, boolean defaultValue) {
        if (json.has(key) && json.get(key).isJsonPrimitive()) {
            try {
                return json.get(key).getAsBoolean();
            } catch (Exception e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
    
    // Add the saveSettings method
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
        
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(json, writer);
        } catch (IOException e) {
            LOGGER.error("Failed to save settings", e);
        }
    }
}
