package ltown.hev_suit.client;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EventManager {
    private static final Logger LOGGER = LogManager.getLogger("EventManager");

    // Armor tracking
    private static int lastArmorValue = -1;
    private static final List<Integer> PROTECTION_SOUNDS = Arrays.asList(
            100, 90, 80, 70, 60, 50, 40, 30, 25, 20, 15, 10, 5
    );

    // Original tracking fields
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

    private static final long HEAT_DAMAGE_COOLDOWN = 5000;
    private static final long GENERAL_COOLDOWN = 5000;
    private static final long FRACTURE_COOLDOWN = 5000;
    private static final long LACERATION_COOLDOWN = 5000;
    private static final long MORPHINE_COOLDOWN = 300000;

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
    }

    private static void onClientTick(MinecraftClient client) {
        try {
            if (!SettingsManager.hevSuitEnabled && !SettingsManager.pvpModeEnabled) return;

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
                            components.add(SettingsManager.useBlackMesaSFX ? "bm_power_level_is" : "power_level_is");
                            components.add(SettingsManager.useBlackMesaSFX ? "bm_100" : "100");
                        } else {
                            components.add(SettingsManager.useBlackMesaSFX ? "bm_power" : "power");
                            decomposePercentage(percent, components);
                        }
                        components.add(SettingsManager.useBlackMesaSFX ? "bm_percent" : "percent");

                        components.forEach(SoundManager::queueSound);
                    }
                }
                // Update lastArmorValue regardless of increase or decrease
                lastArmorValue = currentArmor;
            }

            handleHealthSystem(client, player);
            SoundManager.processSoundQueue(client);
        } catch (Exception e) {
            LOGGER.error("Error in HEV suit client tick", e);
        }
    }

    private static void decomposePercentage(int remaining, List<String> components) {
        for (int num : PROTECTION_SOUNDS) {
            if (remaining <= 0) break;

            if (remaining >= num) {
                components.add((SettingsManager.useBlackMesaSFX ? "bm_" : "") + num);
                remaining -= num;
            }
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
            if (SettingsManager.bloodLossEnabled && currentTime - lastBloodLossTime >= GENERAL_COOLDOWN) {
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
}
