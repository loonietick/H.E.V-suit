package ltown.hev_suit.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class SoundManager {
    private static final Logger LOGGER = LogManager.getLogger("SoundManager");
    static final Map<String, SoundEvent> SOUND_EVENTS = new HashMap<>();
    private static final Random RANDOM = new Random();
    private static final float MIN_PITCH = 0.98f;
    private static final float MAX_PITCH = 1.05f;
    private static SoundInstance currentSound;
    private static final Queue<String> soundQueue = new LinkedList<>();

    public static void registerSounds() {
        String[] soundNames = {
                // Half-Life 1 HEV suit sounds
                "major_laceration", "minor_laceration", "major_fracture", "blood_loss",
                "health_critical", "health_critical2", "morphine_administered", "seek_medical",
                "near_death", "heat_damage", "warning", "bio_reading", "danger", "evacuate_area",
                "immediately", "north", "south", "east", "west", "voice_on", "voice_off",
                "shock_damage", "internal_bleeding", "minor_fracture", "chemical",
                "ammunition_depleted", "morphine_system", "no_medical", "armor_gone", "hev_damage",

                // Half-Life 1 hev suit armor percentage sfx
                "power", "power_level_is", "percent", "5","10", "15", "20", "25", "30",
                "40", "50", "60", "70", "80", "90", "100",
                
                // Black Mesa HEV suit sounds
                "bm_major_laceration", "bm_minor_laceration", "bm_major_fracture", "bm_blood_loss",
                "bm_health_critical", "bm_health_critical2", "bm_morphine_system", "bm_seek_medical",
                "bm_near_death", "bm_chemical",
                "bm_minor_fracture",
                
                // Black Mesa Armor percentage sfx
                "bm_power", "bm_power_level_is", "bm_percent", "bm_5", "bm_10", "bm_15", "bm_20", "bm_25",
                "bm_30", "bm_40", "bm_50", "bm_60", "bm_70", "bm_80", "bm_90", "bm_100"
        };

        for (String soundName : soundNames) {
            registerSound(soundName);
        }
    }

    private static void registerSound(String name) {
        try {
            Identifier soundId = Identifier.of("hev_suit", name);
            SoundEvent sound = SoundEvent.of(soundId);
            Registry.register(Registries.SOUND_EVENT, soundId, sound);
            SOUND_EVENTS.put(name, sound);
        } catch (Exception e) {
            LOGGER.error("Failed to register sound: {}", name, e);
        }
    }

    public static void processSoundQueue(MinecraftClient client) {
        if (currentSound == null || !client.getSoundManager().isPlaying(currentSound)) {
            currentSound = null;
            if (!soundQueue.isEmpty()) {
                playNextSound(client);
            }
        }
    }

    private static void playNextSound(MinecraftClient client) {
        String soundName = soundQueue.poll();
        if (soundName == null) return;

        SoundEvent sound = SOUND_EVENTS.get(soundName);
        if (sound != null) {
            try {
                LOGGER.debug("Playing sound: " + soundName);
                currentSound = PositionedSoundInstance.master(
                        sound,
                        MIN_PITCH + RANDOM.nextFloat() * (MAX_PITCH - MIN_PITCH)
                );
                client.getSoundManager().play(currentSound);
                LOGGER.debug("Displaying caption for: " + soundName + ", captions enabled: " + SettingsManager.captionsEnabled);
                SubtitleManager.displayCaption(soundName);
            } catch (Exception e) {
                LOGGER.error("Error playing sound: {}", soundName, e);
                currentSound = null;
            }
        } else {
            LOGGER.warn("Sound not found: {}", soundName);
        }
    }

    public static void queueSound(String soundName) {
        soundQueue.offer(soundName);
    }

    public static void clearSoundQueue() {
        // Clear any pending sounds
        if (soundQueue != null) {
            soundQueue.clear();
        }
    }

    public static void clearQueue() {
        clearSoundQueue();
    }
}
