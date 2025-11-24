package ltown.hev_suit.client.managers;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.LingeringPotionItem;
import net.minecraft.item.PotionItem;
import net.minecraft.item.SplashPotionItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
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

    private static final KeyBinding.Category CONFIG_CATEGORY;
    private static final KeyBinding OPEN_CONFIG_KEY;

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
    private static long lastMorphineTime = 0;
    private static long lastBloodLossTime = 0;
    private static long lastFractureTime = 0;
    private static long lastGeneralAlertTime = 0;
    private static long lastHeatDamageTime = 0;
    private static long lastShockDamageTime = 0;
    private static long lastMajorLacerationTime = 0;
    private static long lastMinorLacerationTime = 0;
    private static long lastHealthCritical2Time = 0;
    private static final long HEALTH_CRITICAL2_COOLDOWN = 5000; // 5 seconds
    private static boolean lastChestHadElytra = false;

    private static final long HEAT_DAMAGE_COOLDOWN = 8000;
    private static final long GENERAL_COOLDOWN = 5000;
    private static final long BLOOD_LOSS_COOLDOWN = 8000;
    private static final long FRACTURE_COOLDOWN = 5000;
    private static final long LACERATION_COOLDOWN = 7000;
    private static final long MORPHINE_COOLDOWN = 1800000;
    private static final long RADIATION_ALERT_COOLDOWN = 10000;
    private static final long INSUFFICIENT_MEDICAL_COOLDOWN = 30000;
    private static boolean wasInBasalt = false;
    private static long lastRadiationDetectedTime = 0;
    private static long lastInsufficientMedicalTime = 0;
    private static long lastSeekMedicalTime = 0;
    private static final long SEEK_MEDICAL_COOLDOWN = 5000;
    private static boolean totemEffectsActive = false;
    private static long lastTotemActivationTime = 0;
    private static final long TOTEM_ACTIVATION_COOLDOWN = 1000;
    private static boolean awaitingMedicalRepair = false;
    private static boolean wasPlayerDead = false;
    private static int lastWeaponCount = -1;
    private static long lastWeaponPickupTime = 0;
    private static final long WEAPON_PICKUP_COOLDOWN = 1000;
    private static long lastInternalBleedingTime = 0;
    private static final long INTERNAL_BLEEDING_COOLDOWN = 10000;
    private static long lastArmorBreakTime = 0;
    private static final long ARMOR_BREAK_COOLDOWN = 3000;
    private static long lastAmmoAlertTime = 0;
    private static final long AMMO_DEPLETED_COOLDOWN = 2000;
    private static final long INITIAL_ALERT_SUPPRESSION_MS = 4000;
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
        KeyBinding.Category category;
        try {
            category = KeyBinding.Category.create(Identifier.of("hev_suit", "config"));
        } catch (IllegalArgumentException ignored) {
            category = KeyBinding.Category.MISC;
        }
        CONFIG_CATEGORY = category;
        OPEN_CONFIG_KEY = KeyBindingHelper.registerKeyBinding(
                new KeyBinding(
                        "key.hev_suit.open_config",
                        InputUtil.Type.KEYSYM,
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

    private static void onClientTick(MinecraftClient client) {
        try {
            if (client == null) {
                return;
            }

            while (OPEN_CONFIG_KEY.wasPressed()) {
                if (client.currentScreen instanceof HevSuitConfigScreen configScreen) {
                    configScreen.close();
                } else {
                    client.setScreen(new HevSuitConfigScreen(client.currentScreen));
                }
            }

            if (!SettingsManager.hevSuitEnabled && !SettingsManager.pvpModeEnabled) return;

            PlayerEntity player = client.player;
            if (player == null) return;

            boolean inPowderSnow = player.getFrozenTicks() > 0
                    || client.world != null && (
                        client.world.getBlockState(player.getBlockPos()).isOf(Blocks.POWDER_SNOW)
                        || client.world.getBlockState(player.getBlockPos().down()).isOf(Blocks.POWDER_SNOW));
            HudManager.setColdActive(inPowderSnow);

            boolean hasChemicalEffect = player.hasStatusEffect(StatusEffects.POISON) || player.hasStatusEffect(StatusEffects.WITHER);
            HudManager.setBiohazardActive(hasChemicalEffect);

            handleDeathState(player);
            handleBasaltExposure(client, player);
            checkTotemEffects(player);
            trackWeaponPickups(player);
            trackAmmunition(player);

            // Elytra/chestplate equip detection
            ItemStack currentChest = player.getEquippedStack(EquipmentSlot.CHEST);
            boolean currentChestIsElytra = !currentChest.isEmpty() && currentChest.getItem().getTranslationKey().toLowerCase().contains("elytra");
            String currentChestName = currentChest.isEmpty() ? "" : currentChest.getName().getString();
            String currentChestId = currentChest.isEmpty() ? "" : currentChest.getItem().getTranslationKey();
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

            int currentArmor = player.getArmor();
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

    private static void handleHealthSystem(MinecraftClient client, PlayerEntity player) {
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
            handleDamage(client, damage, player.getRecentDamageSource());
        }

        if (SettingsManager.healthAlertsEnabled) {
            if (currentHealth <= 3.0 && lastHealth > 3.0 && SettingsManager.nearDeathEnabled) {
                SoundManager.queueSound("near_death");
            } else if (currentHealth <= 5.0 && lastHealth > 5.0 && SettingsManager.healthCriticalEnabled) {
                SoundManager.queueSound("health_critical");
            } else if (currentHealth <= 10.0 && lastHealth > 10.0 && SettingsManager.seekMedicalEnabled) {
                if (currentTime - lastSeekMedicalTime >= SEEK_MEDICAL_COOLDOWN) {
                    SoundManager.queueSound("seek_medical");
                    lastSeekMedicalTime = currentTime;
                }
            } else if (currentHealth <= 15.0 && lastHealth > 15.0 && SettingsManager.healthCritical2Enabled) {
                if (currentTime - lastHealthCritical2Time >= HEALTH_CRITICAL2_COOLDOWN) {
                    SoundManager.queueSound("health_critical2");
                    lastHealthCritical2Time = currentTime;
                }
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

        if (awaitingMedicalRepair && currentHealth >= player.getMaxHealth() && !player.isDead()) {
            SoundManager.queueSound("medical_repaired");
            awaitingMedicalRepair = false;
        }

        lastHealth = currentHealth;
    }

    private static void handleDamage(MinecraftClient client, float damage, DamageSource damageSource) {
        if (damageSource == null) return;
        long currentTime = System.currentTimeMillis();

        // Add damage indicator if feature is enabled and we have a damage source entity or position
        if (SettingsManager.damageIndicatorsEnabled && client.player != null) {
            Vec3d damagePos = null;

            Entity attacker = damageSource.getAttacker();
            if (attacker != null) {
                // Get position from attacker
                damagePos = getEntityPosition(attacker);
                LOGGER.debug("Damage from attacker: " + attacker); // Debug log
            } else {
                LOGGER.debug("Damage source has no attacker."); // Debug log
            }

            if (damagePos != null) {
                Vec3d playerPos = getEntityPosition(client.player);
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
        if (SettingsManager.chemicalDamageEnabled && currentTime - lastGeneralAlertTime >= GENERAL_COOLDOWN) {
            if ((client.player.hasStatusEffect(StatusEffects.POISON) || client.player.hasStatusEffect(StatusEffects.WITHER)) && !wasPoisoned) {
                SoundManager.queueSound("chemical");
                wasPoisoned = true;
                lastGeneralAlertTime = currentTime;
            } else if (!client.player.hasStatusEffect(StatusEffects.POISON) && !client.player.hasStatusEffect(StatusEffects.WITHER)) {
                wasPoisoned = false;
            }
        }

        // Shock damage with cooldown
        if (damageSource.isOf(DamageTypes.LIGHTNING_BOLT)) {
            HudManager.triggerElectricalAlert();
            if (SettingsManager.shockDamageEnabled && currentTime - lastShockDamageTime >= GENERAL_COOLDOWN) {
                SoundManager.queueSound("shock_damage");
                lastShockDamageTime = currentTime;
            }
        }

        Entity damageEntity = damageSource.getSource();
        if (SettingsManager.bloodLossEnabled
                && damage >= 4.0f
                && currentTime - lastBloodLossTime >= BLOOD_LOSS_COOLDOWN
                && (damageSource.isIn(DamageTypeTags.IS_PROJECTILE)
                    || damageEntity instanceof ArrowEntity
                    || damageEntity instanceof FireballEntity)) {
            SoundManager.queueSound("blood_loss");
            lastBloodLossTime = currentTime;
        }

        if (SettingsManager.internalBleedingEnabled
                && damage > 2.0f
                && damageSource.isIn(DamageTypeTags.IS_EXPLOSION)
                && currentTime - lastInternalBleedingTime >= INTERNAL_BLEEDING_COOLDOWN) {
            SoundManager.queueSound("internal_bleeding");
            lastInternalBleedingTime = currentTime;
        }

        if (damageEntity instanceof HostileEntity && !(damageEntity instanceof CreeperEntity)) {
            if (SettingsManager.fracturesEnabled) {
                if (damage >= 4 && currentTime - lastMajorLacerationTime >= LACERATION_COOLDOWN) {
                    SoundManager.queueSound("major_laceration");
                    lastMajorLacerationTime = currentTime;
                } else if (damage < 3 && currentTime - lastMinorLacerationTime >= LACERATION_COOLDOWN) {
                    SoundManager.queueSound("minor_laceration");
                    lastMinorLacerationTime = currentTime;
                }
            }
        }
          
    }

    private static boolean hasHealingSupplies(PlayerEntity player) {
        if (player == null) return false;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (isHealingItem(stack)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isHealingItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (stack.contains(DataComponentTypes.FOOD)) return true;
        if (stack.getItem() instanceof PotionItem) return true;
        return stack.isOf(Items.HONEY_BOTTLE)
                || stack.isOf(Items.MILK_BUCKET)
                || stack.isOf(Items.SUSPICIOUS_STEW)
                || stack.isOf(Items.MUSHROOM_STEW)
                || stack.isOf(Items.RABBIT_STEW)
                || stack.isOf(Items.BEETROOT_SOUP);
    }

    private static void handleDeathState(PlayerEntity player) {
        boolean isDead = player.isDead() || player.getHealth() <= 0;
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

    private static void handleBasaltExposure(MinecraftClient client, PlayerEntity player) {
        if (client.world == null) {
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

    private static boolean isInBasaltBiome(MinecraftClient client, PlayerEntity player) {
        RegistryEntry<Biome> biomeEntry = client.world.getBiome(player.getBlockPos());
        return biomeEntry != null && biomeEntry.matchesKey(BiomeKeys.BASALT_DELTAS);
    }

    private static void checkTotemEffects(PlayerEntity player) {
        StatusEffectInstance regen = player.getStatusEffect(StatusEffects.REGENERATION);
        StatusEffectInstance absorption = player.getStatusEffect(StatusEffects.ABSORPTION);
        StatusEffectInstance fireRes = player.getStatusEffect(StatusEffects.FIRE_RESISTANCE);

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

    private static void trackWeaponPickups(PlayerEntity player) {
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

    private static int countWeaponStacks(PlayerEntity player) {
        int total = 0;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (isWeaponItem(stack)) {
                total += stack.getCount();
            }
        }
        if (player instanceof ClientPlayerEntity clientPlayer) {
            ItemStack cursorStack = clientPlayer.currentScreenHandler != null ? clientPlayer.currentScreenHandler.getCursorStack() : ItemStack.EMPTY;
            if (isWeaponItem(cursorStack)) {
                total += cursorStack.getCount();
            }
        }
        return total;
    }

    private static boolean isWeaponItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        Identifier id = Registries.ITEM.getId(stack.getItem());
        if (id == null) return false;
        String path = id.getPath();
        List<String> keywords = SettingsManager.weaponKeywords;
        if (keywords == null || keywords.isEmpty()) {
            return false;
        }
        for (String keyword : keywords) {
            if (path.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private static void trackAmmunition(PlayerEntity player) {
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
        if (stack.isIn(ItemTags.ARROWS)) return true;
        Item item = stack.getItem();
        return ADDITIONAL_AMMO_ITEMS.contains(item)
                || item instanceof SplashPotionItem
                || item instanceof LingeringPotionItem;
    }

    private static AmmoSnapshot findHeldAmmo(PlayerEntity player) {
        ItemStack main = player.getMainHandStack();
        if (isAmmoItem(main)) {
            int slot = -1;
            for (int i = 0; i < player.getInventory().size(); i++) {
                if (player.getInventory().getStack(i) == main) {
                    slot = i;
                    break;
                }
            }
            if (slot < 0) {
                slot = player.getInventory().getSlotWithStack(main);
            }
            return new AmmoSnapshot(main.getItem(), main.getCount(), slot, false);
        }
        ItemStack off = player.getOffHandStack();
        if (isAmmoItem(off)) {
            return new AmmoSnapshot(off.getItem(), off.getCount(), -1, true);
        }
        return AmmoSnapshot.EMPTY;
    }

    private static int countAmmoInInventory(PlayerEntity player, Item item) {
        int total = 0;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static void handleElytraDamage(int damageDelta) {
        if (!SettingsManager.fracturesEnabled) return;
        long now = System.currentTimeMillis();
        if (now - lastFractureTime < FRACTURE_COOLDOWN) return;

        if (damageDelta >= 6) {
            SoundManager.queueSound("major_fracture");
            lastFractureTime = now;
        } else if (damageDelta >= 2) {
            SoundManager.queueSound("minor_fracture");
            lastFractureTime = now;
        }
    }

    private static boolean isElytra(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem().getTranslationKey().toLowerCase().contains("elytra");
    }

    private static boolean isHeatDamage(DamageSource damageSource) {
        return damageSource.isIn(DamageTypeTags.IS_FIRE)
                || damageSource.isOf(DamageTypes.HOT_FLOOR)
                || damageSource.isOf(DamageTypes.CAMPFIRE);
    }

    private static Vec3d getEntityPosition(Entity entity) {
        return new Vec3d(entity.getX(), entity.getY(), entity.getZ());
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

    private static void checkArmorDurability(PlayerEntity player) {
        if (!SettingsManager.armorDurabilityEnabled) return;
        boolean playedThresholdSound = false;
        Set<Integer> currentEquipped = new HashSet<>();
        EquipmentSlot[] armorSlots = new EquipmentSlot[] {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        };
        for (EquipmentSlot armorSlot : armorSlots) {
            int slotIndex = armorSlot.getEntitySlotId();
            ItemStack stack = player.getEquippedStack(armorSlot);
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
                int currentDamage = stack.getDamage();
                if (maxDurability > 0) {
                    double durabilityPercent = (maxDurability - currentDamage) / (double)maxDurability;
                    lastKnownDurability.put(slotIndex, durabilityPercent);
                    int previousDamage = lastRecordedItemDamage.getOrDefault(slotIndex, currentDamage);
                    int damageDelta = currentDamage - previousDamage;
                    if (armorSlot == EquipmentSlot.CHEST && isElytra(stack)) {
                        if (damageDelta > 0) {
                            handleElytraDamage(damageDelta);
                        }
                    }
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
