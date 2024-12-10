package ltown.hev_suit.client;

import com.mojang.brigadier.arguments.FloatArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.Entity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.damage.DamageSource;
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
import net.minecraft.util.math.Box;
import net.minecraft.text.Text;
import net.minecraft.item.Items;
import java.util.*;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.Item;
import net.minecraft.entity.EquipmentSlot;

public class Hev_suitClient implements ClientModInitializer {

    private static final Map<String, SoundEvent> SOUND_EVENTS = new HashMap<>();
    private static final Map<String, Integer> SOUND_DURATIONS = new HashMap<>();
    private static final Queue<String> SOUND_QUEUE = new LinkedList<>();
    private static final Set<String> HEALTH_SOUNDS = Set.of(
            "major_laceration", "minor_laceration", "major_fracture",
            "blood_loss", "health_critical", "health_critical2",
            "seek_medical", "near_death", "heat_damage", "shock_damage", "internal_bleeding","minor_fracture","morphine_system"
    );
    private static final Set<Item> HEALING_ITEMS = Set.of(
            Items.GOLDEN_APPLE,
            Items.ENCHANTED_GOLDEN_APPLE,
            Items.GOLDEN_CARROT,
            Items.HONEY_BOTTLE,
            Items.SUSPICIOUS_STEW,
            Items.GLOW_BERRIES,
            Items.COOKED_CHICKEN,
            Items.COOKED_BEEF,
            Items.COOKED_PORKCHOP,
            Items.COOKED_MUTTON,
            Items.COOKED_COD,
            Items.COOKED_SALMON,
            Items.BREAD,
            Items.MUSHROOM_STEW,
            Items.RABBIT_STEW,
            Items.BEETROOT_SOUP
    );

    private static final long RECENT_SOUND_DUPLICATE_THRESHOLD = 5000; // 5 seconds
    private static final long SOUND_COOLDOWN = 3000;
    private static final long MORPHINE_COOLDOWN = 90000;
    private static final long BURNING_COOLDOWN = 5000;
    private static final long MAJOR_LACERATION_COOLDOWN = 15000;
    private static final long MINOR_LACERATION_COOLDOWN = 15000;
    private static final long BLOOD_LOSS_COOLDOWN = 15000;
    private static final long NO_MEDICAL_ALERT_COOLDOWN = 30000; // 30 seconds without healing
    private static final long RADAR_COOLDOWN = 5000;
    private static final long RADAR_VOICE_LINE_COOLDOWN = 10000;
    private static final long SOUND_PLAY_DELAY = 300;
    // Arrow tracking
    private int lastArrowCount = 0;
    private boolean wasHoldingBow = false;
    private boolean wasHoldingCrossbow = false;
    private int lastSpectralArrowCount = 0;
    // Snowball tracking
    private int lastSnowballCount = 0;
    private boolean wasHoldingSnowballItem = false;

    // Elytra and fireworks tracking
    private int lastFireworkCount = 0;
    private boolean wasUsingElytra = false;

    private float lastHealth = 20.0f;
    private boolean radarEnabled = false;
    private boolean hevSuitEnabled = true;
    private boolean isSoundPlaying = false;
    private boolean wasPoisoned = false;

    private long lastSoundTime = 0;
    private long lastMorphineTime = 0;
    private long lastBurningTime = 0;
    private long lastLacerationTime = 0;
    private long lastBloodLossTime = 0;
    private long lastRadarTime = 0;
    private long lastRadarVoiceLineTime = 0;
    private long soundEndTime = 0;

    private long lastHealingOrDamageTime = 0;
    private long lastNoMedicalAlertTime = 0;

    private static float volume = 1.0f;

    private final Set<String> recentSounds = new HashSet<>();
    private final Map<String, Long> soundTimestamps = new HashMap<>();
    private final Map<String, Boolean> healthSoundActiveStates = new HashMap<>();

    @Override
    public void onInitializeClient() {
        registerSounds();
        registerEventListeners();
        registerToggleCommands();
    }

    public static float getVolume() {
        return volume;
    }

    public static void setVolume(float newVolume) {
        volume = Math.max(0, Math.min(2, newVolume));
    }

    private void registerSounds() {
        String[] soundNames = {
                "major_laceration", "minor_laceration", "major_fracture", "blood_loss",
                "health_critical", "health_critical2", "morphine_administered", "seek_medical",
                "near_death", "heat_damage", "warning", "bio_reading",
                "danger", "evacuate_area", "immediately", "north", "south", "east", "west",
                "voice_on", "voice_off", "shock_damage", "internal_bleeding", "minor_fracture","chemical","ammunition_depleted", "morphine_system", "no_medical"
        };
        int[] durations = {
                300, 3000, 3000, 3000, 3000, 3000, 3000, 3000, 3000, 3000,
                3000, 3000, 3000, 3000, 3000, 3000, 3000, 3000, 3000, 3000,
                3000, 3000, 3000, 3000, 3000, 3000, 9000, 5240
        };

        for (int i = 0; i < soundNames.length; i++) {
            registerSound(soundNames[i], durations[i]);
        }
    }

    private void registerSound(String name, int duration) {
        SoundEvent sound = SoundEvent.of(new Identifier("hev_suit", name));
        Registry.register(Registries.SOUND_EVENT, sound.getId(), sound);
        SOUND_EVENTS.put(name, sound);
        SOUND_DURATIONS.put(name, duration);
    }

    private void registerEventListeners() {
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> resetTracking());
    }

    private void registerToggleCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("hevtoggle")
                    .executes(context -> {
                        hevSuitEnabled = !hevSuitEnabled;
                        String status = hevSuitEnabled ? "Activated" : "Deactivated";
                        context.getSource().sendFeedback(Text.literal("Voice System " + status));
                        queueSoundOverride(hevSuitEnabled ? "voice_on" : "voice_off");
                        return 1;
                    })
            );

            dispatcher.register(ClientCommandManager.literal("hevtoggleradar")
                    .executes(context -> {
                        radarEnabled = !radarEnabled;
                        String status = radarEnabled ? "Activated" : "Deactivated";
                        context.getSource().sendFeedback(Text.literal("HEV Radar " + status));
                        return 1;
                    })
            );

            dispatcher.register(ClientCommandManager.literal("hevpitch")
                    .then(ClientCommandManager.argument("pitch", FloatArgumentType.floatArg(0, 2))
                            .executes(context -> {
                                float newVolume = FloatArgumentType.getFloat(context, "pitch");
                                setVolume(newVolume);
                                context.getSource().sendFeedback(Text.literal("HEV Suit pitch set to " + (int)(newVolume * 100) + "%"));
                                return 1;
                            })
                    )
            );
        });
    }

    private void resetTracking() {
        lastHealth = 20.0f;
        lastSoundTime = 0;
        lastMorphineTime = 0;
        lastBurningTime = 0;
        lastLacerationTime = 0;
        lastBloodLossTime = 0;
        lastRadarTime = 0;
        lastRadarVoiceLineTime = 0;
        isSoundPlaying = false;
        soundEndTime = 0;
        recentSounds.clear();
        soundTimestamps.clear();
        healthSoundActiveStates.clear();
    }

    private void checkAmmunitionDepletion(PlayerEntity player) {
        int currentArrowCount = player.getInventory().count(Items.ARROW);
        int currentSpectralArrowCount = player.getInventory().count(Items.SPECTRAL_ARROW);
        int currentSnowballCount = player.getInventory().count(Items.SNOWBALL);
        int currentFireworkCount = player.getInventory().count(Items.FIREWORK_ROCKET);
        boolean isUsingElytra = player.getEquippedStack(EquipmentSlot.CHEST).getItem() instanceof ElytraItem && player.isFallFlying();


        // Arrow depletion
        if ((wasHoldingBow || wasHoldingCrossbow) && currentArrowCount == 0 && lastArrowCount > 0) {
            System.out.println("Arrows depleted. Queueing sound.");
            queueSound("ammunition_depleted");
        }

        // Spectral Arrow depletion
        if ((wasHoldingBow || wasHoldingCrossbow) && currentSpectralArrowCount == 0 && lastSpectralArrowCount > 0) {
            System.out.println("Spectral arrows depleted. Queueing sound.");
            queueSound("ammunition_depleted_spectral");
        }

        // Snowball depletion
        if (wasHoldingSnowballItem && currentSnowballCount == 0 && lastSnowballCount > 0) {
            System.out.println("Snowballs depleted. Queueing sound.");
            queueSound("ammunition_depleted");
        }

        // Firework depletion
        if (wasUsingElytra && isUsingElytra && currentFireworkCount == 0 && lastFireworkCount > 0) {
            System.out.println("Fireworks depleted during Elytra flight. Queueing sound.");
            queueSound("ammunition_depleted");
        }

        // Update state
        lastArrowCount = currentArrowCount;
        lastSpectralArrowCount = currentSpectralArrowCount;
        lastSnowballCount = currentSnowballCount;
        lastFireworkCount = currentFireworkCount;

        wasHoldingBow = player.getMainHandStack().getItem() instanceof BowItem;
        wasHoldingCrossbow = player.getMainHandStack().getItem() instanceof CrossbowItem;
        wasHoldingSnowballItem = player.getMainHandStack().getItem() == Items.SNOWBALL;
        wasUsingElytra = isUsingElytra;
    }


    private void onClientTick(MinecraftClient client) {
        if (!hevSuitEnabled) return;

        PlayerEntity player = client.player;
        if (player == null) return;

        long currentTime = System.currentTimeMillis();
        float currentHealth = player.getHealth();

        // Existing code...
        checkAmmunitionDepletion(player);
        detectHostileMobsNearby(client);

        if (currentHealth <= 0) {
            SOUND_QUEUE.clear();
            isSoundPlaying = false;
            wasPoisoned = false;
            soundEndTime = 0;
            healthSoundActiveStates.clear();
            return;
        }

        // Check for healing items
        boolean hasHealingItem = player.getInventory().main.stream()
                .anyMatch(stack -> HEALING_ITEMS.contains(stack.getItem()));

        // Update last healing or damage time
        if (currentHealth < lastHealth || hasHealingItem) {
            lastHealingOrDamageTime = currentTime;
        }

        // "No medical" alert takes precedence if no healing items
        if (currentHealth < 20 && !hasHealingItem &&
                currentTime - lastHealingOrDamageTime >= NO_MEDICAL_ALERT_COOLDOWN &&
                currentTime - lastNoMedicalAlertTime >= NO_MEDICAL_ALERT_COOLDOWN) {
            queueSound("no_medical");
            lastNoMedicalAlertTime = currentTime;
        }

        // Morphine system only plays if healing items are available
        if (hasHealingItem &&
                currentTime - lastMorphineTime >= MORPHINE_COOLDOWN &&
                currentHealth < 20) {
            queueSound("morphine_system");
            lastMorphineTime = currentTime;
        }

        // Existing health and damage handling code...
        handleHealthSoundState(currentHealth, lastHealth);
        if (player.isOnFire() && currentTime - lastBurningTime >= BURNING_COOLDOWN) {
            queueSound("heat_damage");
            lastBurningTime = currentTime;
        }

        if (currentHealth < lastHealth) {
            float damage = lastHealth - currentHealth;
            handleDamage(client, damage, player.getRecentDamageSource());
        }




        lastHealth = currentHealth;

        processSoundQueueOverride(client, currentTime);
    }

    private void handleHealthSoundState(float currentHealth, float lastHealth) {
        updateHealthSoundState("near_death", currentHealth <= 3.0);
        updateHealthSoundState("health_critical", currentHealth <= 5.0);
        updateHealthSoundState("seek_medical", currentHealth <= 10.0);
        updateHealthSoundState("health_critical2", currentHealth <= 17.0);
    }

    private void updateHealthSoundState(String soundName, boolean condition) {
        if (condition && !healthSoundActiveStates.getOrDefault(soundName, false)) {
            queueSound(soundName);
            healthSoundActiveStates.put(soundName, true);
        } else if (!condition && healthSoundActiveStates.getOrDefault(soundName, false)) {
            SOUND_QUEUE.removeIf(sound -> sound.equals(soundName));
            healthSoundActiveStates.put(soundName, false);
        }
    }

    private void detectHostileMobsNearby(MinecraftClient client) {
        if (!hevSuitEnabled || !radarEnabled) return;
        PlayerEntity player = client.player;
        if (player == null || client.world == null) return;

        long currentTime = System.currentTimeMillis();

        if (currentTime - lastRadarTime < RADAR_COOLDOWN) return;

        Box detectionBox = new Box(
                player.getX() - 10, player.getY() - 5, player.getZ() - 10,
                player.getX() + 10, player.getY() + 5, player.getZ() + 10
        );

        List<HostileEntity> detectedMobs = client.world.getEntitiesByClass(HostileEntity.class, detectionBox, e -> true);

        if (detectedMobs.size() > 3) {
            if (currentTime - lastRadarVoiceLineTime >= RADAR_VOICE_LINE_COOLDOWN) {
                queueSound("evacuate_area");
                queueSound("immediately");
                lastRadarVoiceLineTime = currentTime;
            }
        } else if (detectedMobs.size() > 0) {
            for (Entity entity : detectedMobs) {
                double deltaX = entity.getX() - player.getX();
                double deltaZ = entity.getZ() - player.getZ();

                String direction = getDirection(deltaX, deltaZ);

                if (direction != null) {
                    queueSound("warning");
                    queueSound(direction);
                    lastRadarTime = currentTime;
                    break;
                }
            }
        }
    }

    private String getDirection(double deltaX, double deltaZ) {
        double angle = Math.toDegrees(Math.atan2(deltaZ, deltaX)) + 180;
        if (angle >= 315 || angle < 45) {
            return "west";
        } else if (angle >= 45 && angle < 135) {
            return "north";
        } else if (angle >= 135 && angle < 225) {
            return "east";
        } else if (angle >= 225 && angle < 315) {
            return "south";
        }
        return null;
    }

    private void handleDamage(MinecraftClient client, float damage, DamageSource damageSource) {
        if (!hevSuitEnabled) return;

        long currentTime = System.currentTimeMillis();

        if (damageSource != null) {
            if (damageSource.getSource() instanceof TntEntity || damageSource.getSource() instanceof CreeperEntity) {
                queueSound("internal_bleeding");
            } else if (damageSource.getName().equals("explosion")) {
                queueSound("internal_bleeding");
            }
            if (damageSource.getName().equals("lightningBolt")) {
                queueSound("shock_damage");
            }
            if (client.player.hasStatusEffect(StatusEffects.POISON)) {
                if (!wasPoisoned) {
                    queueSound("chemical");
                    wasPoisoned = true;
                }
            } else {
                wasPoisoned = false;
            }

            Entity damageEntity = damageSource.getSource();
            if (damageEntity instanceof ArrowEntity || damageEntity instanceof FireballEntity) {
                if (currentTime - lastBloodLossTime >= BLOOD_LOSS_COOLDOWN) {
                    queueSound("blood_loss");
                    lastBloodLossTime = currentTime;
                }
            } else if (damageEntity instanceof HostileEntity) {
                if (currentTime - lastLacerationTime >= BLOOD_LOSS_COOLDOWN) {
                    if (damage >= 5) {
                        queueSound("major_laceration");
                    } else {
                        queueSound("minor_laceration");
                    }
                    lastLacerationTime = currentTime;
                }
            }
        }
    }

    private void queueSound(String soundName) {
        SOUND_QUEUE.add(soundName);
    }

    private void queueSoundOverride(String soundName) {
        SOUND_QUEUE.add(soundName);
        processSoundQueueOverride(MinecraftClient.getInstance(), System.currentTimeMillis());
    }

    private void processSoundQueueOverride(MinecraftClient client, long currentTime) {
        // Clean up expired recent sounds
        recentSounds.removeIf(sound ->
                currentTime - soundTimestamps.getOrDefault(sound, 0L) > RECENT_SOUND_DUPLICATE_THRESHOLD
        );

        if (!SOUND_QUEUE.isEmpty()) {
            if (!isSoundPlaying || (currentTime - soundEndTime >= SOUND_PLAY_DELAY)) {
                if (currentTime - lastSoundTime >= SOUND_COOLDOWN) {
                    String soundName = getNextPrioritizedSound(recentSounds);
                    if (soundName != null) {
                        SoundEvent sound = SOUND_EVENTS.get(soundName);
                        if (sound != null) {
                            client.getSoundManager().play(PositionedSoundInstance.master(sound, volume));
                            lastSoundTime = currentTime;
                            isSoundPlaying = true;
                            soundEndTime = currentTime + SOUND_DURATIONS.get(soundName) + SOUND_PLAY_DELAY;

                            // Track the recently played sound
                            recentSounds.add(soundName);
                            soundTimestamps.put(soundName, currentTime);
                        }
                    }
                }
            }
        } else {
            isSoundPlaying = false;
        }
    }

    private String getNextPrioritizedSound(Set<String> recentSounds) {
        // First, prioritize health sounds not recently played
        for (String sound : SOUND_QUEUE) {
            if (HEALTH_SOUNDS.contains(sound) && !recentSounds.contains(sound)) {
                SOUND_QUEUE.remove(sound);
                return sound;
            }
        }

        // Then, find the first sound not recently played
        Iterator<String> iterator = SOUND_QUEUE.iterator();
        while (iterator.hasNext()) {
            String sound = iterator.next();
            if (!recentSounds.contains(sound)) {
                iterator.remove();
                return sound;
            }
        }

        return null;
    }
}