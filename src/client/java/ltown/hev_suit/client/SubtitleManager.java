package ltown.hev_suit.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;
import java.util.HashMap;
import java.util.Map;

public class SubtitleManager {
    private static final Map<String, String> CAPTIONS = new HashMap<>();
    private static boolean captionsEnabled = false;

    static {
        // Half-Life 1 captions
        CAPTIONS.put("major_laceration", "Major Lacerations Detected");
        CAPTIONS.put("minor_laceration", "Minor Lacerations Detected");
        CAPTIONS.put("major_fracture", "Major Fracture Detected");
        CAPTIONS.put("minor_fracture", "Minor Fracture Detected");
        CAPTIONS.put("blood_loss", "Blood Loss Detected");
        CAPTIONS.put("health_critical", "Vital Signs Critical");
        CAPTIONS.put("health_critical2", "Vital Signs are Dropping");
        CAPTIONS.put("morphine_administered", "Morphine Administered");
        CAPTIONS.put("seek_medical", "Seek Medical Attention");
        CAPTIONS.put("near_death", "Emergency! User Death Imminent");
        CAPTIONS.put("heat_damage", "Warning: Heat Damage Detected");
        CAPTIONS.put("shock_damage", "Warning: Eletrical Damage Detected");
        CAPTIONS.put("chemical", "Warning: Hazardous Chemical Detected");
        CAPTIONS.put("ammunition_depleted", "Ammunition Depleted");
        CAPTIONS.put("armor_gone", "Armor Compromised");
        CAPTIONS.put("hev_damage", "HEV Damage Sustained");
        
        // Black Mesa captions
        CAPTIONS.put("bm_major_laceration", "Major Lacerations Detected");
        CAPTIONS.put("bm_minor_laceration", "Minor Lacerations Detected");
        CAPTIONS.put("bm_major_fracture", "Major Fracture Detected");
        CAPTIONS.put("bm_minor_fracture", "Minor Fracture Detected");
        CAPTIONS.put("bm_blood_loss", "Blood Loss Detected");
        CAPTIONS.put("bm_health_critical", "Vital Signs Critical");
        CAPTIONS.put("bm_health_critical2", "Vital Signs are Dropping");
        CAPTIONS.put("bm_morphine_system", "Morphine Administered");
        CAPTIONS.put("bm_seek_medical", "Seek Medical Attention");
        CAPTIONS.put("bm_near_death", "Emergency! User Death Imminent");
        CAPTIONS.put("bm_chemical", "Warning: Hazardous Chemical Detected");

        // Power level related captions
        CAPTIONS.put("power", "Power");
        CAPTIONS.put("bm_power", "Power");
        CAPTIONS.put("power_level_is", "Power Level Is");
        CAPTIONS.put("bm_power_level_is", "Power Level Is");
    }

    public static void toggleCaptions() {
        captionsEnabled = !captionsEnabled;
        SettingsManager.captionsEnabled = captionsEnabled;
        SettingsManager.saveSettings();
    }

    public static boolean areCaptionsEnabled() {
        return SettingsManager.captionsEnabled;
    }

    public static void displayCaption(String soundName) {
        if (!SettingsManager.captionsEnabled) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        // Handle power level announcements
        if (soundName.equals("power") || soundName.equals("bm_power") || 
            soundName.equals("power_level_is") || soundName.equals("bm_power_level_is")) {
            String powerLevel = SoundManager.getPowerLevelFromQueue();
            if (powerLevel != null) {
                Text message = Text.literal("HEV Suit: Power: " + powerLevel + "%")
                    .setStyle(Style.EMPTY.withColor(Formatting.GOLD));
                client.player.sendMessage(message, false);
            }
            return;
        }

        // Only show regular captions for non-power-related sounds
        if (!isPowerNumber(soundName) && !soundName.contains("percent")) {
            String caption = CAPTIONS.get(soundName);
            if (caption != null) {
                Text message = Text.literal("HEV Suit: " + caption)
                    .setStyle(Style.EMPTY.withColor(Formatting.GOLD));
                client.player.sendMessage(message, false);
            }
        }
    }

    private static boolean isPowerNumber(String soundName) {
        try {
            String numStr = soundName.replace("bm_", "");
            Integer.parseInt(numStr);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
