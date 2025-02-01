package ltown.hev_suit.client;

import net.minecraft.client.sound.PositionedSoundInstance;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.entity.Entity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class Hev_suitClient implements ClientModInitializer {

    private static final Logger LOGGER = LogManager.getLogger("Hev_suitClient");
    private static final Map<String, SoundEvent> SOUND_EVENTS = new HashMap<>();

    // Armor tracking
    private int lastArmorValue = -1;
    private static final List<Integer> PROTECTION_SOUNDS = Arrays.asList(
            100, 90, 80, 70, 60, 50, 40, 30, 25,
            20, 19, 18, 17, 16, 15, 14, 13, 12, 11, 10,
            9, 8, 7, 6, 5, 4, 3, 2, 1
    );

    // Original tracking fields
    private float lastHealth = 20.0f;
    private boolean hevSuitEnabled = true;
    private SoundInstance currentSound;
    private boolean wasPoisoned = false;
    private long lastMorphineTime = 0;
    private long lastBurningTime = 0;
    private long lastLacerationTime = 0;
    private long lastBloodLossTime = 0;
    private static final Random RANDOM = new Random();
    private static final float MIN_PITCH = 0.98f;
    private static final float MAX_PITCH = 1.05f;
    private static final long MORPHINE_COOLDOWN = 90000;
    private static final long BURNING_COOLDOWN = 5000;
    private static final long BLOOD_LOSS_COOLDOWN = 5000;

    private final Queue<String> soundQueue = new LinkedList<>();

    @Override
    public void onInitializeClient() {
        registerSounds();
        registerEventListeners();
        registerToggleCommands();
    }

    private void registerSounds() {
        String[] soundNames = {
                // Original sounds
                "major_laceration", "minor_laceration", "major_fracture", "blood_loss",
                "health_critical", "health_critical2", "morphine_administered", "seek_medical",
                "near_death", "heat_damage", "warning", "bio_reading", "danger", "evacuate_area",
                "immediately", "north", "south", "east", "west", "voice_on", "voice_off",
                "shock_damage", "internal_bleeding", "minor_fracture", "chemical",
                "ammunition_depleted", "morphine_system", "no_medical",

                // Armor percentage system
                "power", "power_level_is", "percent", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
                "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "25", "30",
                "40", "50", "60", "70", "80", "90", "100"
        };

        for (String soundName : soundNames) {
            registerSound(soundName);
        }
    }

    private void registerSound(String name) {
        try {
            Identifier soundId = Identifier.of("hev_suit", name);
            SoundEvent sound = SoundEvent.of(soundId);
            Registry.register(Registries.SOUND_EVENT, soundId, sound);
            SOUND_EVENTS.put(name, sound);
        } catch (Exception e) {
            LOGGER.error("Failed to register sound: {}", name, e);
        }
    }

    private void registerEventListeners() {
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> resetTracking());
    }

    private void registerToggleCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> { // haha lambda error go brr
            dispatcher.register(ClientCommandManager.literal("hev_suit")
                    .executes(context -> {
                        hevSuitEnabled = !hevSuitEnabled;
                        String status = hevSuitEnabled ? "Activated" : "Deactivated";
                        context.getSource().sendFeedback(Text.literal("Voice System " + status));
                        queueSound(hevSuitEnabled ? "voice_on" : "voice_off");
                        return 1;
                    })
            );
        });
    }

    private void resetTracking() {
        lastHealth = 20.0f;
        lastArmorValue = -1;
        lastMorphineTime = 0;
        lastBurningTime = 0;
        lastLacerationTime = 0;
        lastBloodLossTime = 0;
    }


    private void onClientTick(MinecraftClient client) {
        try {
            if (!hevSuitEnabled) return;
    
            PlayerEntity player = client.player;
            if (player == null) return;
    
            // Armor percentage system
            int currentArmor = player.getArmor();
            if (currentArmor != lastArmorValue) {
                // Only play sound when armor increases
                if (currentArmor > lastArmorValue) {
                    int percent = currentArmor * 5;
    
                    if (percent > 0) {
                        List<String> components = new ArrayList<>();
    
                        if (percent == 100) {
                            components.add("power_level_is");
                            components.add("100");
                        } else {
                            components.add("power");
                            decomposePercentage(percent, components);
                        }
                        components.add("percent");
    
                        components.forEach(this::queueSound);
                    }
                }
                // Update lastArmorValue regardless of increase or decrease
                lastArmorValue = currentArmor;
            }
    
            handleHealthSystem(client, player);
            processSoundQueue(client);
        } catch (Exception e) {
            LOGGER.error("Error in HEV suit client tick", e);
        }
    }

    private void decomposePercentage(int remaining, List<String> components) {
        for (int num : PROTECTION_SOUNDS) {
            if (remaining <= 0) break;

            while (remaining >= num) {
                components.add(String.valueOf(num));
                remaining -= num;
                if (remaining == 0) break;
            }
        }
    }

    private void handleHealthSystem(MinecraftClient client, PlayerEntity player) {
        float currentHealth = player.getHealth();
        long currentTime = System.currentTimeMillis();

        if (currentHealth <= 0) {
            soundQueue.clear();
            wasPoisoned = false;
            return;
        }

        if (player.isOnFire() && currentTime - lastBurningTime >= BURNING_COOLDOWN) {
            queueSound("heat_damage");
            lastBurningTime = currentTime;
        }

        if (currentHealth < lastHealth) {
            float damage = lastHealth - currentHealth;
            handleDamage(client, damage, player.getRecentDamageSource());
        }

        if (currentHealth <= 3.0 && lastHealth > 3.0) {
            queueSound("near_death");
        } else if (currentHealth <= 5.0 && lastHealth > 5.0) {
            queueSound("health_critical");
        } else if (currentHealth <= 10.0 && lastHealth > 10.0) {
            queueSound("seek_medical");
        } else if (currentHealth <= 17.0 && lastHealth > 17.0) {
            queueSound("health_critical2");
        }

        if (currentTime - lastMorphineTime >= MORPHINE_COOLDOWN && currentHealth < 20) {
            queueSound("morphine_administered");
            lastMorphineTime = currentTime;
        }

        lastHealth = currentHealth;
    }

    private void handleDamage(MinecraftClient client, float damage, DamageSource damageSource) {
        if (!hevSuitEnabled) return;
        long currentTime = System.currentTimeMillis();

        if (damageSource != null) {
            if (damageSource.isOf(DamageTypes.FALL)) {
                queueSound(damage >= 6 ? "major_fracture" : "minor_fracture");
            }
            if (damageSource.isOf(DamageTypes.EXPLOSION) ||
                    damageSource.getSource() instanceof TntEntity ||
                    damageSource.getSource() instanceof CreeperEntity) {
                queueSound("internal_bleeding");
            }
            if (damageSource.isOf(DamageTypes.LIGHTNING_BOLT)) {
                queueSound("shock_damage");
            }
            if (client.player.hasStatusEffect(StatusEffects.POISON) && !wasPoisoned) {
                queueSound("chemical");
                wasPoisoned = true;
            } else if (!client.player.hasStatusEffect(StatusEffects.POISON)) {
                wasPoisoned = false;
            }

            Entity damageEntity = damageSource.getSource();
            if (damageEntity instanceof ArrowEntity || damageEntity instanceof FireballEntity) {
                if (currentTime - lastBloodLossTime >= BLOOD_LOSS_COOLDOWN) {
                    queueSound("blood_loss");
                    lastBloodLossTime = currentTime;
                }
            } else if (damageEntity instanceof HostileEntity &&
                    currentTime - lastLacerationTime >= BLOOD_LOSS_COOLDOWN) {
                queueSound(damage >= 5 ? "major_laceration" : "minor_laceration");
                lastLacerationTime = currentTime;
            }
        }
    }

    private void processSoundQueue(MinecraftClient client) {
        if (currentSound == null || !client.getSoundManager().isPlaying(currentSound)) {
            currentSound = null;
            if (!soundQueue.isEmpty()) {
                playNextSound(client);
            }
        }
    }

    private void playNextSound(MinecraftClient client) {
        String soundName = soundQueue.poll();
        if (soundName == null) return;

        SoundEvent sound = SOUND_EVENTS.get(soundName);
        if (sound != null) {
            try {
                currentSound = PositionedSoundInstance.master(
                        sound,
                        MIN_PITCH + RANDOM.nextFloat() * (MAX_PITCH - MIN_PITCH)
                );
                client.getSoundManager().play(currentSound);
            } catch (Exception e) {
                LOGGER.error("Error playing sound: {}", soundName, e);
                currentSound = null;
            }
        } else {
            LOGGER.warn("Sound not found: {}", soundName);
        }
    }

    private void queueSound(String soundName) {
        soundQueue.offer(soundName);
    }
}