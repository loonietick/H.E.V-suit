package ltown.hev_suit.client.managers;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.hurtingprojectile.Fireball;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.LingeringPotionItem;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.SplashPotionItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Holder;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

import ltown.hev_suit.client.screen.HevSuitConfigScreen;
import org.lwjgl.glfw.GLFW;

public class EventManager {
    private static final Logger LOGGER = LogManager.getLogger("EventManager");

    static final KeyMapping.Category CONFIG_CATEGORY;
    private static final KeyMapping OPEN_CONFIG_KEY;

    private static final List<Double> DURABILITY_THRESHOLDS = Arrays.asList(
        0.50, 0.35, 0.25, 0.10, 0.05
    );
    private static final Map<Integer, Double> lastArmorThresholds = new HashMap<>();
    private static final Set<Integer> equippedArmorSlots = new HashSet<>();  // Add this line
    private static final Set<Integer> brokenArmor = new HashSet<>();
    private static final Map<Integer, Double> lastKnownDurability = new HashMap<>(); // Add this line
    private static final Map<Integer, Integer> lastRecordedItemDamage = new HashMap<>();
    private static final Map<Integer, ItemStack> lastEquippedArmorStacks = new HashMap<>();
    private static final Set<Integer> equipDamageAlertedSlots = new HashSet<>();
    private static final Map<Integer, Set<Double>> triggeredArmorThresholds = new HashMap<>();
    private static String lastChestName = "";
    private static String lastChestItemId = "";

 
    private static int lastArmorValue = -1;
    private static float lastHealth = 20.0f;
    private static boolean wasPoisoned = false;
    // getLastDamageSource() is only valid for ~40 ticks (2s) after the hurt event lands on the
    // client, and can be overwritten by a second damage instance before we get to read it. Health
    // sync can lag a tick or two behind the hurt event, so sampling live every tick and caching the
    // last non-null value bridges that gap instead of racing the vanilla TTL.
    private static DamageSource cachedDamageSource = null;
    private static long cachedDamageSourceAt = 0L;
    private static final long DAMAGE_SOURCE_CACHE_GRACE_MS = 750;
    private static long lastMorphineTime = 0;
    private static long lastBloodLossTime = 0;
    private static long lastFractureTime = 0;
    private static long lastGeneralAlertTime = 0;
    private static long lastHeatDamageTime = 0;
    private static long lastShockDamageTime = 0;
    private static long lastMajorLacerationTime = 0;
    private static long lastMinorLacerationTime = 0;
    private static long lastHealthCritical2Time = 0;
    private static final long HEALTH_CRITICAL2_COOLDOWN_ACCURATE = 600000; // 10 minutes (HL1 !HEV_HLTH1 SUIT_NEXT_IN_10MIN)
    private static final long HEALTH_CRITICAL2_COOLDOWN_CLASSIC = 5000; // pre-accuracy-pass value
    private static long lastNearDeathTime = 0;
    private static final long NEAR_DEATH_COOLDOWN_ACCURATE = 600000; // 10 minutes (HL1 !HEV_HLTH3 SUIT_NEXT_IN_10MIN)
    private static final long NEAR_DEATH_COOLDOWN_CLASSIC = 0; // pre-accuracy-pass had no cooldown at all, just the crossing edge
    private static long lastHealthCriticalTime = 0;
    private static final long HEALTH_CRITICAL_COOLDOWN_ACCURATE = 600000; // 10 minutes (HL1 !HEV_HLTH2 SUIT_NEXT_IN_10MIN)
    private static final long HEALTH_CRITICAL_COOLDOWN_CLASSIC = 0; // pre-accuracy-pass had no cooldown at all, just the crossing edge
    private static boolean lastChestHadElytra = false;

    private static final long HEAT_DAMAGE_COOLDOWN = 8000;
    private static final long GENERAL_COOLDOWN_ACCURATE = 60000; // 1 minute (HL1 DET0/DET1/DMG3 SUIT_NEXT_IN_1MIN)
    private static final long GENERAL_COOLDOWN_CLASSIC = 5000; // pre-accuracy-pass value
    private static final long BLOOD_LOSS_COOLDOWN_ACCURATE = 30000; // 30 seconds (HL1 !HEV_DMG6 SUIT_NEXT_IN_30SEC)
    private static final long BLOOD_LOSS_COOLDOWN_CLASSIC = 8000; // pre-accuracy-pass value
    private static final long FRACTURE_COOLDOWN_ACCURATE = 30000; // 30 seconds (HL1 !HEV_DMG4/5 SUIT_NEXT_IN_30SEC)
    private static final long FRACTURE_COOLDOWN_CLASSIC = 5000; // pre-accuracy-pass value
    private static final long LACERATION_COOLDOWN_ACCURATE = 30000; // 30 seconds (HL1 !HEV_DMG0/1 SUIT_NEXT_IN_30SEC)
    private static final long LACERATION_COOLDOWN_CLASSIC = 7000; // pre-accuracy-pass value
    private static final long MORPHINE_COOLDOWN = 1800000;
    private static final long RADIATION_ALERT_COOLDOWN = 10000;
    private static final long INSUFFICIENT_MEDICAL_COOLDOWN = 30000;
    private static boolean wasInBasalt = false;
    private static long lastRadiationDetectedTime = 0;
    private static long lastInsufficientMedicalTime = 0;
    private static long lastSeekMedicalTime = 0;
    private static final long SEEK_MEDICAL_COOLDOWN_ACCURATE = 300000; // 5 minutes (HL1 !HEV_DMG7 SUIT_NEXT_IN_5MIN)
    private static final long SEEK_MEDICAL_COOLDOWN_CLASSIC = 5000; // pre-accuracy-pass value
    private static boolean totemEffectsActive = false;
    private static long lastTotemActivationTime = 0;
    private static final long TOTEM_ACTIVATION_COOLDOWN = 1000;
    private static boolean awaitingMedicalRepair = false;
    private static boolean wasPlayerDead = false;
    private static int lastWeaponCount = -1;
    private static long lastWeaponPickupTime = 0;
    private static final long WEAPON_PICKUP_COOLDOWN = 1000;
    private static long lastInternalBleedingTime = 0;
    private static final long INTERNAL_BLEEDING_COOLDOWN_ACCURATE = 60000; // 1 minute (HL1 !HEV_DMG2 SUIT_NEXT_IN_1MIN)
    private static final long INTERNAL_BLEEDING_COOLDOWN_CLASSIC = 10000; // pre-accuracy-pass value
    private static long lastArmorBreakTime = 0;
    private static final long ARMOR_BREAK_COOLDOWN = 3000;
    private static long lastAmmoAlertTime = 0;
    private static final long AMMO_DEPLETED_COOLDOWN = 2000;
    private static final long INITIAL_ALERT_SUPPRESSION_MS = 4000;
    private static final Set<Item> WEAPON_ITEMS = initWeaponItems();
    private static final Set<Item> ADDITIONAL_AMMO_ITEMS = Set.of(
            Items.SNOWBALL,
            Items.EGG,
            Items.ENDER_PEARL,
            Items.FIREWORK_ROCKET,
            Items.FIRE_CHARGE,
            Items.EXPERIENCE_BOTTLE
    );
    private static Item lastHeldAmmoItem = null;
    private static int lastHeldAmmoCount = -1;
    private static int lastHeldAmmoSlot = -2;
    private static boolean lastHeldAmmoFromOffhand = false;

    static {
        KeyMapping.Category category;
        try {
            category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("hev_suit", "config"));
        } catch (IllegalArgumentException ignored) {
            category = KeyMapping.Category.MISC;
        }
        CONFIG_CATEGORY = category;
        OPEN_CONFIG_KEY = KeyMappingHelper.registerKeyMapping(
                new KeyMapping(
                        "key.hev_suit.open_config",
                        InputConstants.Type.KEYSYM,
                        GLFW.GLFW_KEY_H,
                        CONFIG_CATEGORY
                )
        );
    }

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
        lastRecordedItemDamage.clear();
        awaitingMedicalRepair = false;
        lastInternalBleedingTime = 0;
        lastArmorBreakTime = 0;
        lastAmmoAlertTime = 0;
        lastHeldAmmoItem = null;
        lastHeldAmmoCount = -1;
        lastHeldAmmoSlot = -2;
        lastHeldAmmoFromOffhand = false;
        lastEquippedArmorStacks.clear();
        equipDamageAlertedSlots.clear();
        triggeredArmorThresholds.clear();
        wasInBasalt = false;
        lastRadiationDetectedTime = 0;
        lastInsufficientMedicalTime = 0;
        lastSeekMedicalTime = 0;
        lastHealthCritical2Time = 0;
        lastNearDeathTime = 0;
        lastHealthCriticalTime = 0;
        totemEffectsActive = false;
        lastTotemActivationTime = 0;
        wasPlayerDead = false;
        lastWeaponCount = -1;
        lastWeaponPickupTime = 0;
        SoundManager.stopGeigerLoop();
        SoundManager.stopFlatline();
        HudManager.setColdActive(false);
        HudManager.setBiohazardActive(false);
        HudManager.clearElectricalAlert();
        HudManager.setRadiationActive(false);
        SoundManager.suppressAlertsFor(INITIAL_ALERT_SUPPRESSION_MS);
    }

    private static void onClientTick(Minecraft client) {
        try {
            if (client == null) {
                return;
            }

            while (OPEN_CONFIG_KEY.consumeClick()) {
                if (client.screen instanceof HevSuitConfigScreen configScreen) {
                    configScreen.onClose();
                } else {
                    client.setScreen(new HevSuitConfigScreen(client.screen));
                }
            }

            if (!SettingsManager.hevSuitEnabled && !SettingsManager.pvpModeEnabled) return;

            Player player = client.player;
            if (player == null) return;

            // Refresh the damage-source cache every tick (not just when we already see a health
            // drop) so a hurt event that lands this tick isn't lost if the health sync arrives late.
            resolveDamageSource(player);

            boolean inPowderSnow = player.getTicksFrozen() > 0
                    || client.level != null && (
                        client.level.getBlockState(player.blockPosition()).getBlock() == Blocks.POWDER_SNOW
                        || client.level.getBlockState(player.blockPosition()).getBlock() == Blocks.POWDER_SNOW);
            HudManager.setColdActive(inPowderSnow);

            boolean hasChemicalEffect = player.hasEffect(MobEffects.POISON) || player.hasEffect(MobEffects.WITHER);
            HudManager.setBiohazardActive(hasChemicalEffect);

            handleDeathState(player);
            handleBasaltExposure(client, player);
            checkTotemEffects(player);
            trackWeaponPickups(player);
            trackAmmunition(player);

            // Elytra/chestplate equip detection
            ItemStack currentChest = player.getItemBySlot(EquipmentSlot.CHEST);
            boolean currentChestIsElytra = !currentChest.isEmpty() && currentChest.getItem().getDescriptionId().toLowerCase().contains("elytra");
            String currentChestName = currentChest.isEmpty() ? "" : currentChest.getHoverName().getString();
            String currentChestId = currentChest.isEmpty() ? "" : currentChest.getItem().getDescriptionId();
            boolean chestNameChanged = !currentChestName.equals(lastChestName) || !currentChestId.equals(lastChestItemId);
            if (chestNameChanged) {
                // Elytra equip (only on first equip)
                if (currentChestIsElytra && !lastChestHadElytra && SettingsManager.elytraEquipSfxEnabled) {
                    SoundManager.queueSound("powermove_on");
                }
                // HEV chestplate equip (only on first equip, and only if name starts with HEV)
                if (!currentChest.isEmpty()
                        && currentChestName.toUpperCase().startsWith("HEV")
                        && SettingsManager.hevLogonEnabled) {
                    SoundManager.queueSound("hev_logon");
                }
                lastChestName = currentChestName;
                lastChestItemId = currentChestId;
            }
            lastChestHadElytra = currentChestIsElytra;

            checkArmorDurability(player);

            int currentArmor = player.getArmorValue();
            if (currentArmor != lastArmorValue) {
                if (currentArmor > lastArmorValue) {
                    int adjustedPercent = HudManager.getScaledArmorValue(player);

                    if (adjustedPercent > 0 && adjustedPercent <= 100) { 
                        List<String> components = new ArrayList<>();

                        if (adjustedPercent == 100) {
                            components.add("power_level_is");
                            components.add("100");
                        } else {
                            components.add("power");
                            for (int part : getArmorAnnouncement(adjustedPercent)) {
                                components.add(String.valueOf(part));
                            }
                        }
                        components.add("percent");

                        components.forEach(SoundManager::queueSound);
                    } else if (adjustedPercent > 100) { 
                        SoundManager.queueSound("hev_general_fail");
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

    private static void handleHealthSystem(Minecraft client, Player player) {
        float currentHealth = player.getHealth();
        long currentTime = System.currentTimeMillis();

        if (currentHealth <= 0) {
            SoundManager.clearSoundQueue();
            wasPoisoned = false;
            awaitingMedicalRepair = false;
            return;
        }

        if (currentHealth < lastHealth) {
            float damage = lastHealth - currentHealth;
            handleDamage(client, damage, resolveDamageSource(player));
        }

        // Real HEV suit re-checks these on every damage event while health stays low, gated
        // only by each line's own no-repeat cooldown (HL1 CBasePlayer::TakeDamage) -- not a
        // one-shot notice fired only the instant you first cross the line. "Chatty suit" mode
        // reverts to the original shorter cooldowns, and for near_death/health_critical
        // specifically (which had no cooldown at all pre-accuracy-pass) restores the original
        // one-shot crossing requirement instead of the 10-minute gate.
        boolean chatty = SettingsManager.chattySuitEnabled;
        if (SettingsManager.healthAlertsEnabled && currentHealth < lastHealth) {
            if (currentHealth <= 3.0 && SettingsManager.nearDeathEnabled
                    && (!chatty || lastHealth > 3.0)
                    && currentTime - lastNearDeathTime >= cooldownFor(NEAR_DEATH_COOLDOWN_ACCURATE, NEAR_DEATH_COOLDOWN_CLASSIC)) {
                SoundManager.queueSound("near_death");
                lastNearDeathTime = currentTime;
            } else if (currentHealth <= 5.0 && SettingsManager.healthCriticalEnabled
                    && (!chatty || lastHealth > 5.0)
                    && currentTime - lastHealthCriticalTime >= cooldownFor(HEALTH_CRITICAL_COOLDOWN_ACCURATE, HEALTH_CRITICAL_COOLDOWN_CLASSIC)) {
                SoundManager.queueSound("health_critical");
                lastHealthCriticalTime = currentTime;
            } else if (currentHealth <= 10.0 && SettingsManager.seekMedicalEnabled
                    && (!chatty || lastHealth > 10.0)
                    && currentTime - lastSeekMedicalTime >= cooldownFor(SEEK_MEDICAL_COOLDOWN_ACCURATE, SEEK_MEDICAL_COOLDOWN_CLASSIC)) {
                SoundManager.queueSound("seek_medical");
                lastSeekMedicalTime = currentTime;
            } else if (currentHealth <= 15.0 && SettingsManager.healthCritical2Enabled
                    && (!chatty || lastHealth > 15.0)
                    && currentTime - lastHealthCritical2Time >= cooldownFor(HEALTH_CRITICAL2_COOLDOWN_ACCURATE, HEALTH_CRITICAL2_COOLDOWN_CLASSIC)) {
                SoundManager.queueSound("health_critical2");
                lastHealthCritical2Time = currentTime;
            }
        }

        if (SettingsManager.healthAlertsEnabled && SettingsManager.insufficientMedicalEnabled
                && currentHealth <= 10.0f && currentHealth < lastHealth
                && currentTime - lastInsufficientMedicalTime >= INSUFFICIENT_MEDICAL_COOLDOWN
                && !hasHealingSupplies(player)) {
            SoundManager.queueSound("insufficient_medical");
            lastInsufficientMedicalTime = currentTime;
            awaitingMedicalRepair = true;
        }

        // Only play morphine SFX if damage taken is 5 or more (2.5 hearts)
        if (SettingsManager.morphineEnabled && currentTime - lastMorphineTime >= MORPHINE_COOLDOWN && currentHealth < 20) {
            float damage = lastHealth - currentHealth;
            if (damage >= 6.0f) {
                SoundManager.queueSound("morphine_administered");
                lastMorphineTime = currentTime;
            }
        }

        if (awaitingMedicalRepair && currentHealth >= player.getMaxHealth() && !player.isDeadOrDying()) {
            SoundManager.queueSound("medical_repaired");
            awaitingMedicalRepair = false;
        }

        lastHealth = currentHealth;
    }


    // Resolves a cooldown to its classic (pre-accuracy-pass) value when "chatty suit" mode is
    // on, otherwise the real HL1-derived value.
    private static long cooldownFor(long accurate, long classic) {
        return SettingsManager.chattySuitEnabled ? classic : accurate;
    }

    private static DamageSource resolveDamageSource(Player player) {
        DamageSource live = player.getLastDamageSource();
        long now = System.currentTimeMillis();
        if (live != null) {
            cachedDamageSource = live;
            cachedDamageSourceAt = now;
            return live;
        }
        if (cachedDamageSource != null && now - cachedDamageSourceAt <= DAMAGE_SOURCE_CACHE_GRACE_MS) {
            return cachedDamageSource;
        }
        return null;
    }

    private static void handleDamage(Minecraft client, float damage, DamageSource damageSource) {
        if (damageSource == null) return;
        long currentTime = System.currentTimeMillis();

        // Add damage indicator if feature is enabled and we have a damage source entity or position
        if (SettingsManager.damageIndicatorsEnabled && client.player != null) {
            Vec3 damagePos = null;

            Entity attacker = damageSource.getEntity();
            if (attacker != null) {
                // Get position from attacker
                damagePos = getEntityPosition(attacker);
                LOGGER.debug("Damage from attacker: " + attacker); // Debug log
            } else {
                LOGGER.debug("Damage source has no attacker."); // Debug log
            }

            if (damagePos != null) {
                Vec3 playerPos = getEntityPosition(client.player);
                float playerYaw = client.player.getYRot();
                float playerPitch = client.player.getXRot();

                HudManager.addDamageIndicator(
                    damagePos,
                    playerPos,
                    playerYaw,
                    playerPitch
                );
            }
        }

        // Fall damage and fractures with cooldown
        if (damageSource.is(DamageTypes.FALL) && SettingsManager.fracturesEnabled && currentTime - lastFractureTime >= cooldownFor(FRACTURE_COOLDOWN_ACCURATE, FRACTURE_COOLDOWN_CLASSIC)) {
            if (damage >= 6) {
                SoundManager.queueSound("major_fracture");
                lastFractureTime = currentTime;
            } else if (damage >= 3) {
                SoundManager.queueSound("minor_fracture");
                lastFractureTime = currentTime;
            }
        }

        if (SettingsManager.heatDamageEnabled && damage >= 4.0f && isHeatDamage(damageSource) &&
                currentTime - lastHeatDamageTime >= HEAT_DAMAGE_COOLDOWN) {
            SoundManager.queueSound("heat_damage");
            lastHeatDamageTime = currentTime;
        }

        // Chemical damage with cooldown
        if (SettingsManager.chemicalDamageEnabled && currentTime - lastGeneralAlertTime >= cooldownFor(GENERAL_COOLDOWN_ACCURATE, GENERAL_COOLDOWN_CLASSIC)) {
            if ((client.player.hasEffect(MobEffects.POISON) || client.player.hasEffect(MobEffects.WITHER)) && !wasPoisoned) {
                SoundManager.queueSound("chemical");
                wasPoisoned = true;
                lastGeneralAlertTime = currentTime;
            } else if (!client.player.hasEffect(MobEffects.POISON) && !client.player.hasEffect(MobEffects.WITHER)) {
                wasPoisoned = false;
            }
        }

        // Shock damage with cooldown
        if (damageSource.is(DamageTypes.LIGHTNING_BOLT)) {
            HudManager.triggerElectricalAlert();
            if (SettingsManager.shockDamageEnabled && currentTime - lastShockDamageTime >= cooldownFor(GENERAL_COOLDOWN_ACCURATE, GENERAL_COOLDOWN_CLASSIC)) {
                SoundManager.queueSound("shock_damage");
                lastShockDamageTime = currentTime;
            }
        }

        Entity damageEntity = damageSource.getDirectEntity();
        if (SettingsManager.bloodLossEnabled
                && damage >= 4.0f
                && currentTime - lastBloodLossTime >= cooldownFor(BLOOD_LOSS_COOLDOWN_ACCURATE, BLOOD_LOSS_COOLDOWN_CLASSIC)
                && (damageSource.is(DamageTypeTags.IS_PROJECTILE)
                    || damageEntity instanceof Arrow
                    || damageEntity instanceof Fireball)) {
            SoundManager.queueSound("blood_loss");
            lastBloodLossTime = currentTime;
        }

        if (SettingsManager.internalBleedingEnabled
                && damage > 2.0f
                && damageSource.is(DamageTypeTags.IS_EXPLOSION)
                && currentTime - lastInternalBleedingTime >= cooldownFor(INTERNAL_BLEEDING_COOLDOWN_ACCURATE, INTERNAL_BLEEDING_COOLDOWN_CLASSIC)) {
            SoundManager.queueSound("internal_bleeding");
            lastInternalBleedingTime = currentTime;
        }

        if (damageEntity instanceof Monster && !(damageEntity instanceof Creeper)) {
            if (SettingsManager.fracturesEnabled) {
                if (damage >= 4 && currentTime - lastMajorLacerationTime >= cooldownFor(LACERATION_COOLDOWN_ACCURATE, LACERATION_COOLDOWN_CLASSIC)) {
                    SoundManager.queueSound("major_laceration");
                    lastMajorLacerationTime = currentTime;
                } else if (damage < 3 && currentTime - lastMinorLacerationTime >= cooldownFor(LACERATION_COOLDOWN_ACCURATE, LACERATION_COOLDOWN_CLASSIC)) {
                    SoundManager.queueSound("minor_laceration");
                    lastMinorLacerationTime = currentTime;
                }
            }
        }
          
    }

    private static boolean hasHealingSupplies(Player player) {
        if (player == null) return false;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (isHealingItem(stack)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isHealingItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (stack.has(DataComponents.FOOD)) return true;
        if (stack.getItem() instanceof PotionItem) return true;
        return stack.getItem() == Items.HONEY_BOTTLE
                || stack.getItem() == Items.MILK_BUCKET
                || stack.getItem() == Items.SUSPICIOUS_STEW
                || stack.getItem() == Items.MUSHROOM_STEW
                || stack.getItem() == Items.RABBIT_STEW
                || stack.getItem() == Items.BEETROOT_SOUP;
    }

    private static void handleDeathState(Player player) {
        boolean isDead = player.isDeadOrDying() || player.getHealth() <= 0;
        if (isDead) {
            if (!wasPlayerDead && SettingsManager.deathSfxEnabled) {
                SoundManager.playFlatline();
            }
            awaitingMedicalRepair = false;
            wasPlayerDead = true;
        } else if (wasPlayerDead) {
            SoundManager.stopFlatline();
            wasPlayerDead = false;
        }
    }

    private static void handleBasaltExposure(Minecraft client, Player player) {
        if (client.level == null) {
            if (wasInBasalt) {
                SoundManager.stopGeigerLoop();
                wasInBasalt = false;
            }
            return;
        }

        if (!SettingsManager.radiationSfxEnabled) {
            if (wasInBasalt) {
                SoundManager.stopGeigerLoop();
                wasInBasalt = false;
            }
            return;
        }

        boolean inBasalt = isInBasaltBiome(client, player);
        if (inBasalt) {
            SoundManager.startGeigerLoop();
            long now = System.currentTimeMillis();
            if (!wasInBasalt && now - lastRadiationDetectedTime >= RADIATION_ALERT_COOLDOWN) {
                SoundManager.playImmediateSound("radiation_detected");
                lastRadiationDetectedTime = now;
            }
        } else {
            SoundManager.stopGeigerLoop();
        }
        wasInBasalt = inBasalt;
    }

    private static boolean isInBasaltBiome(Minecraft client, Player player) {
        Holder<Biome> biomeEntry = client.level.getBiome(player.blockPosition());
        return biomeEntry != null && biomeEntry.is(Biomes.BASALT_DELTAS);
    }

    private static void checkTotemEffects(Player player) {
        MobEffectInstance regen = player.getEffect(MobEffects.REGENERATION);
        MobEffectInstance absorption = player.getEffect(MobEffects.ABSORPTION);
        MobEffectInstance fireRes = player.getEffect(MobEffects.FIRE_RESISTANCE);

        boolean hasTotemRegen = regen != null && regen.getAmplifier() >= 1 && regen.getDuration() > 0;
        boolean hasTotemAbsorption = absorption != null && absorption.getAmplifier() >= 1 && absorption.getDuration() > 0;
        boolean hasTotemFireRes = fireRes != null && fireRes.getDuration() >= 200;
        boolean hasTotemEffects = hasTotemRegen && hasTotemAbsorption && hasTotemFireRes;

        if (hasTotemEffects && !totemEffectsActive) {
            long now = System.currentTimeMillis();
            if (now - lastTotemActivationTime >= TOTEM_ACTIVATION_COOLDOWN) {
                onTotemActivated();
                lastTotemActivationTime = now;
            }
        }

        totemEffectsActive = hasTotemEffects;
    }

    private static void onTotemActivated() {
        if (SettingsManager.administeringMedicalEnabled) {
            SoundManager.playImmediateSound("administering_medical");
            SoundManager.queueSound("morphine_administered");
        }
        awaitingMedicalRepair = true;
    }

    private static void trackWeaponPickups(Player player) {
        if (!SettingsManager.weaponPickupEnabled) {
            lastWeaponCount = countWeaponStacks(player);
            return;
        }
        int weaponCount = countWeaponStacks(player);
        if (lastWeaponCount == -1) {
            lastWeaponCount = weaponCount;
            return;
        }
        if (weaponCount > lastWeaponCount) {
            long now = System.currentTimeMillis();
            if (now - lastWeaponPickupTime >= WEAPON_PICKUP_COOLDOWN) {
                SoundManager.queueSound("weapon_pickup");
                lastWeaponPickupTime = now;
            }
        }
        lastWeaponCount = weaponCount;
    }

    private static int countWeaponStacks(Player player) {
        int total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (isWeaponItem(stack)) {
                total += stack.getCount();
            }
        }
        if (player instanceof LocalPlayer clientPlayer) {
            ItemStack cursorStack = clientPlayer.containerMenu != null ? clientPlayer.containerMenu.getCarried() : ItemStack.EMPTY;
            if (isWeaponItem(cursorStack)) {
                total += cursorStack.getCount();
            }
        }
        return total;
    }

    private static boolean isWeaponItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return WEAPON_ITEMS.contains(stack.getItem());
    }

    private static void trackAmmunition(Player player) {
        if (player == null) return;

        AmmoSnapshot current = findHeldAmmo(player);
        if (!current.isEmpty()) {
            if (lastHeldAmmoItem == null
                    || lastHeldAmmoItem != current.item
                    || lastHeldAmmoSlot != current.slot
                    || lastHeldAmmoFromOffhand != current.offhand) {
                lastHeldAmmoItem = current.item;
                lastHeldAmmoCount = current.count;
                lastHeldAmmoSlot = current.slot;
                lastHeldAmmoFromOffhand = current.offhand;
            } else {
                lastHeldAmmoCount = current.count;
            }
            return;
        }

        if (lastHeldAmmoItem != null && lastHeldAmmoCount > 0) {
            int remaining = countAmmoInInventory(player, lastHeldAmmoItem);
            if (remaining <= 0) {
                long now = System.currentTimeMillis();
                if (SettingsManager.ammoDepletedEnabled && now - lastAmmoAlertTime >= AMMO_DEPLETED_COOLDOWN) {
                    SoundManager.queueSound("ammunition_depleted");
                    lastAmmoAlertTime = now;
                }
            }
        }

        lastHeldAmmoItem = null;
        lastHeldAmmoCount = -1;
        lastHeldAmmoSlot = -2;
        lastHeldAmmoFromOffhand = false;
    }

    private static boolean isAmmoItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (stack.typeHolder().is(ItemTags.ARROWS)) return true;
        Item item = stack.getItem();
        return ADDITIONAL_AMMO_ITEMS.contains(item)
                || item instanceof SplashPotionItem
                || item instanceof LingeringPotionItem;
    }

    private static AmmoSnapshot findHeldAmmo(Player player) {
        ItemStack main = player.getMainHandItem();
        if (isAmmoItem(main)) {
            int slot = -1;
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                if (player.getInventory().getItem(i) == main) {
                    slot = i;
                    break;
                }
            }
            if (slot < 0) {
                slot = player.getInventory().findSlotMatchingItem(main);
            }
            return new AmmoSnapshot(main.getItem(), main.getCount(), slot, false);
        }
        ItemStack off = player.getOffhandItem();
        if (isAmmoItem(off)) {
            return new AmmoSnapshot(off.getItem(), off.getCount(), -1, true);
        }
        return AmmoSnapshot.EMPTY;
    }

    private static int countAmmoInInventory(Player player, Item item) {
        int total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static boolean isElytra(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem().getDescriptionId().toLowerCase().contains("elytra");
    }

    private static boolean isHeatDamage(DamageSource damageSource) {
        return damageSource.is(DamageTypeTags.IS_FIRE)
                || damageSource.is(DamageTypes.HOT_FLOOR)
                || damageSource.is(DamageTypes.CAMPFIRE);
    }

    private static Vec3 getEntityPosition(Entity entity) {
        return new Vec3(entity.getX(), entity.getY(), entity.getZ());
    }

    private static Set<Item> initWeaponItems() {
        Set<Item> items = new HashSet<>();
        addIfPresent(items, Identifier.fromNamespaceAndPath("minecraft", "trident"));
        addIfPresent(items, Identifier.fromNamespaceAndPath("minecraft", "bow"));
        addIfPresent(items, Identifier.fromNamespaceAndPath("minecraft", "crossbow"));
        addIfPresent(items, Identifier.fromNamespaceAndPath("minecraft", "mace"));
        for (String material : List.of("copper", "iron", "diamond", "netherite")) {
            addIfPresent(items, Identifier.fromNamespaceAndPath("minecraft", material + "_sword"));
        }
        return items;
    }

    private static void addIfPresent(Set<Item> items, Identifier id) {
        BuiltInRegistries.ITEM.get(id).ifPresent(ref -> items.add(ref.value()));
    }

    private static class AmmoSnapshot {
        static final AmmoSnapshot EMPTY = new AmmoSnapshot(null, 0, -2, false);
        final Item item;
        final int count;
        final int slot;
        final boolean offhand;

        AmmoSnapshot(Item item, int count, int slot, boolean offhand) {
            this.item = item;
            this.count = count;
            this.slot = slot;
            this.offhand = offhand;
        }

        boolean isEmpty() {
            return item == null || count <= 0;
        }
    }

    private static void checkArmorDurability(Player player) {
        if (!SettingsManager.armorDurabilityEnabled) return;
        boolean playedThresholdSound = false;
        Set<Integer> currentEquipped = new HashSet<>();
        EquipmentSlot[] armorSlots = new EquipmentSlot[] {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        };
        for (EquipmentSlot armorSlot : armorSlots) {
            int slotIndex = armorSlot.getId();
            ItemStack stack = player.getItemBySlot(armorSlot);
            if (!stack.isEmpty()) {
                currentEquipped.add(slotIndex);
                ItemStack previousTracked = lastEquippedArmorStacks.get(slotIndex);
                boolean newStack = previousTracked != stack;
                lastEquippedArmorStacks.put(slotIndex, stack);
                if (!equippedArmorSlots.contains(slotIndex)) {
                    equippedArmorSlots.add(slotIndex);
                }
                if (newStack) {
                    equipDamageAlertedSlots.remove(slotIndex);
                    triggeredArmorThresholds.remove(slotIndex);
                }
                Set<Double> triggeredThresholds = triggeredArmorThresholds.computeIfAbsent(slotIndex, key -> new HashSet<>());
                brokenArmor.remove(slotIndex);
                int maxDurability = stack.getMaxDamage();
                int currentDamage = stack.getDamageValue();
                if (maxDurability > 0) {
                    double durabilityPercent = (maxDurability - currentDamage) / (double)maxDurability;
                    lastKnownDurability.put(slotIndex, durabilityPercent);
                    int previousDamage = lastRecordedItemDamage.getOrDefault(slotIndex, currentDamage);
                    int damageDelta = currentDamage - previousDamage;
                    lastRecordedItemDamage.put(slotIndex, currentDamage);
                    if (damageDelta > 0) {
                        double breakThreshold = Math.max(0.01, 1.0 / maxDurability);
                        if (durabilityPercent <= breakThreshold) {
                            brokenArmor.add(slotIndex);
                        } else if (durabilityPercent > breakThreshold * 2) {
                            brokenArmor.remove(slotIndex);
                        }
                    }
                    if (!playedThresholdSound) {
                        double lastThreshold = lastArmorThresholds.getOrDefault(slotIndex, 1.0);
                        for (double threshold : DURABILITY_THRESHOLDS) {
                            if (durabilityPercent <= threshold && lastThreshold > threshold) {
                                if (armorSlot == EquipmentSlot.CHEST && isElytra(stack)) {
                                    if (SettingsManager.powerArmorOverloadEnabled) {
                                        SoundManager.queueSound("powermove_overload");
                                    }
                                } else if (SettingsManager.hevDamageEnabled) {
                                    if ((!newStack || !equipDamageAlertedSlots.contains(slotIndex))
                                            && !triggeredThresholds.contains(threshold)) {
                                        SoundManager.queueSound("hev_damage");
                                        triggeredThresholds.add(threshold);
                                        if (newStack) {
                                            equipDamageAlertedSlots.add(slotIndex);
                                        }
                                    }
                                }
                                playedThresholdSound = true;
                                break;
                            }
                        }
                    }
                    lastArmorThresholds.put(slotIndex, durabilityPercent);
                }
            } else {
                lastEquippedArmorStacks.remove(slotIndex);
                equipDamageAlertedSlots.remove(slotIndex);
                triggeredArmorThresholds.remove(slotIndex);
            }
        }
        Set<Integer> damageSlots = new HashSet<>(lastRecordedItemDamage.keySet());
        damageSlots.removeAll(currentEquipped);
        for (Integer index : damageSlots) {
            lastRecordedItemDamage.remove(index);
            lastEquippedArmorStacks.remove(index);
            equipDamageAlertedSlots.remove(index);
            triggeredArmorThresholds.remove(index);
        }
    }
}
