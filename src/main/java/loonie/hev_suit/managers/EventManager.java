package loonie.hev_suit.managers;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.PotionEvent;
import java.util.*;

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
        MinecraftForge.EVENT_BUS.register(new EventManager());
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

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        try {
            Minecraft client = Minecraft.getMinecraft();
            if (!SettingsManager.hevSuitEnabled && !SettingsManager.pvpModeEnabled) return;

            EntityPlayer player = client.player;
            if (player == null) return;
            checkArmorDurability(player);

            int currentArmor = player.getTotalArmorValue();
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

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        resetTracking();
    }
    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) event.getEntity();
            Minecraft client = Minecraft.getMinecraft();
            if (client.player != null && client.player.equals(player)) {
                handleDamage(client, event.getAmount(), event.getSource());
            }
        }
    }
    @SubscribeEvent
    public void onPotionAdded(PotionEvent.PotionAddedEvent event) {
        if (event.getEntity() instanceof EntityPlayer && event.getPotionEffect().getPotion() == MobEffects.POISON) {
            EntityPlayer player = (EntityPlayer) event.getEntity();
            Minecraft client = Minecraft.getMinecraft();
            if (client.player != null && client.player.equals(player) && SettingsManager.chemicalDamageEnabled) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastGeneralAlertTime >= GENERAL_COOLDOWN) {
                    String prefix = SettingsManager.useBlackMesaSFX ? "bm_" : "";
                    SoundManager.queueSound(prefix + "chemical");
                    lastGeneralAlertTime = currentTime;
                }
         }
        }
    }
    private static void handleDamage(Minecraft client, float damage, DamageSource damageSource) {
        if (damageSource == null) return;
        long currentTime = System.currentTimeMillis();
        String prefix = SettingsManager.useBlackMesaSFX ? "bm_" : "";
    
        Entity immediateSource = damageSource.getImmediateSource();
        Entity trueSource = damageSource.getTrueSource();
    
        // Blood loss check (projectiles)
        if (immediateSource instanceof EntityArrow || immediateSource instanceof EntityFireball) {
            if (SettingsManager.bloodLossEnabled && currentTime - lastBloodLossTime >= BLOOD_LOSS_COOLDOWN) {
                SoundManager.queueSound(prefix + "blood_loss");
                lastBloodLossTime = currentTime;
            }
        }
    
        // Fractures (fall damage)
        if (damageSource == DamageSource.FALL && SettingsManager.fracturesEnabled) {
            if (currentTime - lastFractureTime >= FRACTURE_COOLDOWN) {
                if (damage >= 6) {
                    SoundManager.queueSound(prefix + "major_fracture");
                    lastFractureTime = currentTime;
                } else if (damage >= 3) {
                    SoundManager.queueSound(prefix + "minor_fracture");
                    lastFractureTime = currentTime;
                }
            }
        }
    
        // Lacerations (melee mob attacks)
        if (trueSource instanceof EntityMob && !(trueSource instanceof EntityCreeper)) {
            // Check if it's a direct melee attack (source and immediate source are same)
            if (trueSource == immediateSource && SettingsManager.fracturesEnabled) {
                if (damage >= 4 && currentTime - lastMajorLacerationTime >= LACERATION_COOLDOWN) {
                    SoundManager.queueSound(prefix + "major_laceration");
                    lastMajorLacerationTime = currentTime;
                } else if (damage < 4 && currentTime - lastMinorLacerationTime >= LACERATION_COOLDOWN) {
                    SoundManager.queueSound(prefix + "minor_laceration");
                    lastMinorLacerationTime = currentTime;
                }
            }
        }
    
        // Shock damage
        if (SettingsManager.shockDamageEnabled && damageSource == DamageSource.LIGHTNING_BOLT && 
            currentTime - lastShockDamageTime >= GENERAL_COOLDOWN) {
                SoundManager.queueSound(prefix + "shock_damage");
                lastShockDamageTime = currentTime;
        }
    }

    private static void checkArmorDurability(EntityPlayer player) {
        if (!SettingsManager.armorDurabilityEnabled || SettingsManager.useBlackMesaSFX) return;

        int slot = 0;
        boolean playedThresholdSound = false;
        Set<Integer> currentEquipped = new HashSet<>();

        for (ItemStack stack : player.inventory.armorInventory) {
            if (stack != null && !stack.isEmpty()) {
                currentEquipped.add(slot);
                if (!equippedArmorSlots.contains(slot)) {
                    // New piece of armor equipped
                    equippedArmorSlots.add(slot);
                }
                
                int maxDurability = stack.getMaxDamage();
                int currentDamage = stack.getItemDamage();
                
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

    private static List<Integer> getArmorAnnouncement(int value) {
        if (value == 100) {
            return Collections.singletonList(100);
        }
        int remainder = value % 5;
        int rounded = (remainder >= 3) ? (value + (5 - remainder)) : (value - remainder);
        
        switch (rounded) {
            case 95: return Arrays.asList(90, 5);
            case 85: return Arrays.asList(80, 5);
            case 75: return Arrays.asList(70, 5);
            case 65: return Arrays.asList(60, 5);
            case 55: return Arrays.asList(50, 5);
            case 45: return Arrays.asList(40, 5);
            case 35: return Arrays.asList(30, 5);
            default:
                return Collections.singletonList(rounded);
        }
    }

    private static void handleHealthSystem(Minecraft client, EntityPlayer player) {
        float currentHealth = player.getHealth();
        long currentTime = System.currentTimeMillis();

        if (currentHealth <= 0) {
            SoundManager.clearSoundQueue();
            return;
        }

        if (player.isBurning() && SettingsManager.heatDamageEnabled && 
            currentTime - lastHeatDamageTime >= HEAT_DAMAGE_COOLDOWN) {
            SoundManager.queueSound(SettingsManager.useBlackMesaSFX ? "bm_heat_damage" : "heat_damage");
            lastHeatDamageTime = currentTime;
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
}
