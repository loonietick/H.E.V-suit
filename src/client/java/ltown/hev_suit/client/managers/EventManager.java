package ltown.hev_suit.client.managers;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.FireballEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraft.util.math.Vec3d;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

public class EventManager {
    private static final Logger LOGGER = LogManager.getLogger("EventManager");


    private static final List<Double> DURABILITY_THRESHOLDS = Arrays.asList(
        0.50, 0.35, 0.25, 0.10, 0.05
    );
    private static final Map<Integer, Double> lastArmorThresholds = new HashMap<>();
    private static final Set<Integer> equippedArmorSlots = new HashSet<>();  // Add this line
    private static final Set<Integer> brokenArmor = new HashSet<>();
    private static final Map<Integer, Double> lastKnownDurability = new HashMap<>(); // Add this line

 
    private static int lastArmorValue = -1;
    private static float lastHealth = 20.0f;
    private static boolean wasPoisoned = false;
    private static long lastMorphineTime = 0;
    private static long lastBloodLossTime = 0;
    private static long lastFractureTime = 0;
    private static long lastGeneralAlertTime = 0;
    private static long lastHeatDamageTime = 0;
    private static long lastShockDamageTime = 0;
    private static long lastMajorLacerationTime = 0;
    private static long lastMinorLacerationTime = 0;

    private static final long HEAT_DAMAGE_COOLDOWN = 8000;
    private static final long GENERAL_COOLDOWN = 5000;
    private static final long BLOOD_LOSS_COOLDOWN = 8000;
    private static final long FRACTURE_COOLDOWN = 5000;
    private static final long LACERATION_COOLDOWN = 5000;
    private static final long MORPHINE_COOLDOWN = 1800000;

    public static void registerEventListeners() {
       
        ClientTickEvents.END_CLIENT_TICK.register(EventManager::onClientTick);
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> resetTracking());
    }

    private static void resetTracking() {
        lastHealth = 20.0f;
        lastArmorValue = -1;
        lastMorphineTime = 0;
        lastBloodLossTime = 0;
        lastFractureTime = 0;
        lastGeneralAlertTime = 0;
        lastHeatDamageTime = 0;
        lastShockDamageTime = 0;
        lastMajorLacerationTime = 0;
        lastMinorLacerationTime = 0;
        lastArmorThresholds.clear();
        equippedArmorSlots.clear();  // Add this line
        brokenArmor.clear();
        lastKnownDurability.clear(); // Add this line
    }

    private static void onClientTick(MinecraftClient client) {
        try {
            if (!SettingsManager.hevSuitEnabled && !SettingsManager.pvpModeEnabled) return;

            PlayerEntity player = client.player;
            if (player == null) return;


            checkArmorDurability(player);

            int currentArmor = player.getArmor();
            if (currentArmor != lastArmorValue) {
                if (currentArmor > lastArmorValue) {
                    int adjustedPercent = HudManager.getScaledArmorValue(player);

                    if (adjustedPercent > 0) {
                        List<String> components = new ArrayList<>();

                        if (adjustedPercent == 100) {
                            components.add(SettingsManager.useBlackMesaSFX ? "bm_power_level_is" : "power_level_is");
                            components.add(SettingsManager.useBlackMesaSFX ? "bm_100" : "100");
                        } else {
                            components.add(SettingsManager.useBlackMesaSFX ? "bm_power" : "power");
                            for (int part : getArmorAnnouncement(adjustedPercent)) {
                                components.add(SettingsManager.useBlackMesaSFX ? "bm_" + part : String.valueOf(part));
                            }
                        }
                        components.add(SettingsManager.useBlackMesaSFX ? "bm_percent" : "percent");

                        components.forEach(SoundManager::queueSound);
                    }
                }
                lastArmorValue = currentArmor;
            }

            handleHealthSystem(client, player);
            SoundManager.processSoundQueue(client);
        } catch (Exception e) {
            LOGGER.error("Error in HEV suit client tick", e);
        }
    }

    private static List<Integer> getArmorAnnouncement(int value) {
        if (value == 100) {
            // Exactly 100 remains as-is
            return Collections.singletonList(100);
        }
        // Round to closest multiple of 5
        int remainder = value % 5;
        int rounded = (remainder >= 3) ? (value + (5 - remainder)) : (value - remainder);
        // Handle "missing" multiples like 95, 85, etc.
        switch (rounded) {
            case 95: return Arrays.asList(90, 5);
            case 85: return Arrays.asList(80, 5);
            case 75: return Arrays.asList(70, 5);
            case 65: return Arrays.asList(60, 5);
            case 55: return Arrays.asList(50, 5);
            case 45: return Arrays.asList(40, 5);
            case 35: return Arrays.asList(30, 5);
            default:
                // If it's 25, 15, or 5, we have direct files; otherwise it's fine as a single segment
                return Collections.singletonList(rounded);
        }
    }

    private static void handleHealthSystem(MinecraftClient client, PlayerEntity player) {
        float currentHealth = player.getHealth();
        long currentTime = System.currentTimeMillis();

        if (currentHealth <= 0) {
            SoundManager.clearSoundQueue();
            wasPoisoned = false;
            return;
        }

        if (player.isOnFire() && SettingsManager.heatDamageEnabled && currentTime - lastHeatDamageTime >= HEAT_DAMAGE_COOLDOWN) {
            SoundManager.queueSound(SettingsManager.useBlackMesaSFX ? "bm_heat_damage" : "heat_damage");
            lastHeatDamageTime = currentTime;
        }

        if (currentHealth < lastHealth) {
            float damage = lastHealth - currentHealth;
            handleDamage(client, damage, player.getRecentDamageSource());
        }

        if (SettingsManager.healthAlertsEnabled) {
            String prefix = SettingsManager.useBlackMesaSFX ? "bm_" : "";
            if (currentHealth <= 3.0 && lastHealth > 3.0 && SettingsManager.nearDeathEnabled) {
                SoundManager.queueSound(prefix + "near_death");
            } else if (currentHealth <= 5.0 && lastHealth > 5.0 && SettingsManager.healthCriticalEnabled) {
                SoundManager.queueSound(prefix + "health_critical");
            } else if (currentHealth <= 10.0 && lastHealth > 10.0 && SettingsManager.seekMedicalEnabled) {
                SoundManager.queueSound(prefix + "seek_medical");
            } else if (currentHealth <= 15.0 && lastHealth > 15.0 && SettingsManager.healthCritical2Enabled) {
                SoundManager.queueSound(prefix + "health_critical2");
            }
        }

        if (SettingsManager.morphineEnabled && currentTime - lastMorphineTime >= MORPHINE_COOLDOWN && currentHealth < 20) {
            SoundManager.queueSound(SettingsManager.useBlackMesaSFX ? "bm_morphine_system" : "morphine_administered");
            lastMorphineTime = currentTime;
        }

        lastHealth = currentHealth;
    }

    private static void handleDamage(MinecraftClient client, float damage, DamageSource damageSource) {
        if (damageSource == null) return;
        long currentTime = System.currentTimeMillis();
        String prefix = SettingsManager.useBlackMesaSFX ? "bm_" : "";

        // Add damage indicator if feature is enabled and we have a damage source entity or position
        if (SettingsManager.damageIndicatorsEnabled && client.player != null) {
            Vec3d damagePos = null;

            Entity attacker = damageSource.getAttacker();
            if (attacker != null) {
                // Get position from attacker
                damagePos = attacker.getPos();
                LOGGER.debug("Damage from attacker: " + attacker); // Debug log
            } else {
                LOGGER.debug("Damage source has no attacker."); // Debug log
            }

            if (damagePos != null) {
                Vec3d playerPos = client.player.getPos();
                float playerYaw = client.player.getYaw();
                float playerPitch = client.player.getPitch();

                HudManager.addDamageIndicator(
                    damagePos,
                    playerPos,
                    playerYaw,
                    playerPitch
                );
            }
        }

        // Fall damage and fractures with cooldown
        if (damageSource.isOf(DamageTypes.FALL) && SettingsManager.fracturesEnabled && currentTime - lastFractureTime >= FRACTURE_COOLDOWN) {
            if (damage >= 6) {
                SoundManager.queueSound(prefix + "major_fracture");
                lastFractureTime = currentTime;
            } else if (damage >= 3) {
                SoundManager.queueSound(prefix + "minor_fracture");
                lastFractureTime = currentTime;
            }
        }

        // Chemical damage with cooldown
        if (SettingsManager.chemicalDamageEnabled && currentTime - lastGeneralAlertTime >= GENERAL_COOLDOWN) {
            if (client.player.hasStatusEffect(StatusEffects.POISON) && !wasPoisoned) {
                SoundManager.queueSound(prefix + "chemical");
                wasPoisoned = true;
                lastGeneralAlertTime = currentTime;
            } else if (!client.player.hasStatusEffect(StatusEffects.POISON)) {
                wasPoisoned = false;
            }
        }

        // Shock damage with cooldown
        if (SettingsManager.shockDamageEnabled && damageSource.isOf(DamageTypes.LIGHTNING_BOLT) && 
            currentTime - lastShockDamageTime >= GENERAL_COOLDOWN) {
            SoundManager.queueSound(prefix + "shock_damage");
            lastShockDamageTime = currentTime;
        }

        Entity damageEntity = damageSource.getSource();
        if (damageEntity instanceof ArrowEntity || damageEntity instanceof FireballEntity) {
            if (SettingsManager.bloodLossEnabled && currentTime - lastBloodLossTime >= BLOOD_LOSS_COOLDOWN) {
                SoundManager.queueSound(SettingsManager.useBlackMesaSFX ? "bm_blood_loss" : "blood_loss");
                lastBloodLossTime = currentTime;
            }
        }

        if (damageEntity instanceof HostileEntity && !(damageEntity instanceof CreeperEntity)) {
            if (SettingsManager.fracturesEnabled) {
                if (damage >= 4 && currentTime - lastMajorLacerationTime >= LACERATION_COOLDOWN) {
                    SoundManager.queueSound(prefix + "major_laceration");
                    lastMajorLacerationTime = currentTime;
                } else if (damage < 3 && currentTime - lastMinorLacerationTime >= LACERATION_COOLDOWN) {
                    SoundManager.queueSound(prefix + "minor_laceration");
                    lastMinorLacerationTime = currentTime;
                }
            }
          
        }
    }

    private static void checkArmorDurability(PlayerEntity player) {
        if (!SettingsManager.armorDurabilityEnabled || SettingsManager.useBlackMesaSFX) return;
        
        int slot = 0;
        boolean playedThresholdSound = false;
        Set<Integer> currentEquipped = new HashSet<>();
        
        for (var stack : player.getArmorItems()) {
            if (!stack.isEmpty()) {
                currentEquipped.add(slot);
                if (!equippedArmorSlots.contains(slot)) {
                    // New piece of armor equipped
                    equippedArmorSlots.add(slot);
                }
                
                int maxDurability = stack.getMaxDamage();
                int currentDamage = stack.getDamage();
                
                if (maxDurability > 0) {
                    double durabilityPercent = (maxDurability - currentDamage) / (double)maxDurability;
                    lastKnownDurability.put(slot, durabilityPercent);
                    
                    // Check thresholds if we haven't played a threshold sound yet
                    if (!playedThresholdSound) {
                        double lastThreshold = lastArmorThresholds.getOrDefault(slot, 1.0);
                        
                        for (double threshold : DURABILITY_THRESHOLDS) {
                            if (durabilityPercent <= threshold && lastThreshold > threshold) {
                                SoundManager.queueSound("hev_damage");
                                playedThresholdSound = true;
                                break;
                            }
                        }
                    }
                    
                    lastArmorThresholds.put(slot, durabilityPercent);
                }
            }
            slot++;
        }
        

    }
}
