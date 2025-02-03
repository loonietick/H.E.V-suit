package ltown.hev_suit.client;

import net.minecraft.client.sound.PositionedSoundInstance;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
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
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraft.item.Item;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import java.util.*;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class Hev_suitClient implements ClientModInitializer {

    private static final Logger LOGGER = LogManager.getLogger("Hev_suitClient");
    private static final Map<String, SoundEvent> SOUND_EVENTS = new HashMap<>();
    private static final File CONFIG_FILE = new File("config/hev_suit_settings.json");
    private static final Gson GSON = new Gson();

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
    private long lastLacerationTime = 0;
    private long lastBloodLossTime = 0;
    private static final Random RANDOM = new Random();
    private static final float MIN_PITCH = 0.98f;
    private static final float MAX_PITCH = 1.05f;
    private static final long MORPHINE_COOLDOWN = 90000;
    private static final long BLOOD_LOSS_COOLDOWN = 5000;
    private static final int AMBER_COLOR = 0xFFFFAE00;
    private static final int DARK_AMBER = 0xFF8B5E00;
    private final Queue<String> soundQueue = new LinkedList<>();
    private boolean healthAlertsEnabled = true;
    private boolean hudEnabled = true;
    private boolean fracturesEnabled = true;
    private boolean heatDamageEnabled = true;
    private boolean bloodLossEnabled = true;
    private boolean shockDamageEnabled = true;
    private boolean chemicalDamageEnabled = true;
    private boolean pvpModeEnabled = false;
    private boolean morphineEnabled = true;
    private long lastHeatDamageTime = 0;
    private long lastShockDamageTime = 0;

    private static final long HEAT_DAMAGE_COOLDOWN = 5000;
    private static final long SHOCK_DAMAGE_COOLDOWN = 5000;
    private static final long CHEMICAL_DAMAGE_COOLDOWN = 5000;

    private static final long MAJOR_LACERATION_COOLDOWN = 5000;
    private static final long MINOR_LACERATION_COOLDOWN = 5000;
    private boolean healthCritical2Enabled = true;
    private boolean seekMedicalEnabled = true;
    private boolean healthCriticalEnabled = true;
    private boolean nearDeathEnabled = true;
    private boolean useBlackMesaSFX = false;
// alll of the shit that 
    @Override
    public void onInitializeClient() {
        LOGGER.debug("Initializing HEV Suit Client");
        loadSettings();
        registerSounds();
        registerEventListeners();
        registerToggleCommands();
        registerHud();
    }

    private void loadSettings() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
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
                LOGGER.debug("Settings loaded: useBlackMesaSFX = " + useBlackMesaSFX);
            } catch (IOException e) {
                LOGGER.error("Failed to load settings", e);
            }
        }
    }

    private boolean getOrDefault(JsonObject json, String key, boolean defaultValue) {
        return json.has(key) ? json.get(key).getAsBoolean() : defaultValue;
    }

    private void saveSettings() {
        JsonObject json = new JsonObject();
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

        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(json, writer);
        } catch (IOException e) {
            LOGGER.error("Failed to save settings", e);
        }
    }

    private void registerSounds() {
        String[] soundNames = {
                // Half-Life 1 HEV suit sounds
                "major_laceration", "minor_laceration", "major_fracture", "blood_loss",
                "health_critical", "health_critical2", "morphine_administered", "seek_medical",
                "near_death", "heat_damage", "warning", "bio_reading", "danger", "evacuate_area",
                "immediately", "north", "south", "east", "west", "voice_on", "voice_off",
                "shock_damage", "internal_bleeding", "minor_fracture", "chemical",
                "ammunition_depleted", "morphine_system", "no_medical",

                // Half-Life 1 hev suit armor percentage sfx
                "power", "power_level_is", "percent", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
                "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "25", "30",
                "40", "50", "60", "70", "80", "90", "100",
                
                // Black Mesa HEV suit sounds
                "bm_major_laceration", "bm_minor_laceration", "bm_major_fracture", "bm_blood_loss",
                "bm_health_critical", "bm_health_critical2", "bm_morphine_system", "bm_seek_medical",
                "bm_near_death", "bm_chemical"


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
// commands
    private void registerToggleCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("hev")
                .then(ClientCommandManager.literal("toggle")
                    .then(ClientCommandManager.literal("health")
                        .then(ClientCommandManager.literal("health_critical2")
                            .executes(context -> {
                                if (!useBlackMesaSFX) {
                                    healthCritical2Enabled = !healthCritical2Enabled;
                                    context.getSource().sendFeedback(Text.literal("Health Critical 2 alerts " + (healthCritical2Enabled ? "enabled" : "disabled")));
                                    saveSettings();
                                }
                                return 1;
                            })
                        )
                        .then(ClientCommandManager.literal("seek_medical")
                            .executes(context -> {
                                if (!useBlackMesaSFX) {
                                    seekMedicalEnabled = !seekMedicalEnabled;
                                    context.getSource().sendFeedback(Text.literal("Seek Medical alerts " + (seekMedicalEnabled ? "enabled" : "disabled")));
                                    saveSettings();
                                }
                                return 1;
                            })
                        )
                        .then(ClientCommandManager.literal("health_critical")
                            .executes(context -> {
                                if (!useBlackMesaSFX) {
                                    healthCriticalEnabled = !healthCriticalEnabled;
                                    context.getSource().sendFeedback(Text.literal("Health Critical alerts " + (healthCriticalEnabled ? "enabled" : "disabled")));
                                    saveSettings();
                                }
                                return 1;
                            })
                        )
                        .then(ClientCommandManager.literal("near_death")
                            .executes(context -> {
                                if (!useBlackMesaSFX) {
                                    nearDeathEnabled = !nearDeathEnabled;
                                    context.getSource().sendFeedback(Text.literal("Near Death alerts " + (nearDeathEnabled ? "enabled" : "disabled")));
                                    saveSettings();
                                }
                                return 1;
                            })
                        )
                    )
                    .then(ClientCommandManager.literal("all")
                        .executes(context -> {
                            if (!useBlackMesaSFX) {
                                boolean newState = !(healthAlertsEnabled && hudEnabled && fracturesEnabled && heatDamageEnabled && bloodLossEnabled && shockDamageEnabled && chemicalDamageEnabled && morphineEnabled && healthCritical2Enabled && seekMedicalEnabled && healthCriticalEnabled && nearDeathEnabled);
                                healthAlertsEnabled = newState;
                                hudEnabled = newState;
                                fracturesEnabled = newState;
                                heatDamageEnabled = newState;
                                bloodLossEnabled = newState;
                                shockDamageEnabled = newState;
                                chemicalDamageEnabled = newState;
                                morphineEnabled = newState;
                                healthCritical2Enabled = newState;
                                seekMedicalEnabled = newState;
                                healthCriticalEnabled = newState;
                                nearDeathEnabled = newState;
                                context.getSource().sendFeedback(Text.literal("All alerts " + (newState ? "enabled" : "disabled")));
                                saveSettings();
                            }
                            return 1;
                        })
                    )
                    .then(ClientCommandManager.literal("hud")
                        .executes(context -> {
                            if (!useBlackMesaSFX) {
                                hudEnabled = !hudEnabled;
                                context.getSource().sendFeedback(Text.literal("HUD " + (hudEnabled ? "enabled" : "disabled")));
                                saveSettings();
                            }
                            return 1;
                        })
                    )
                    .then(ClientCommandManager.literal("fractures")
                        .executes(context -> {
                            if (!useBlackMesaSFX) {
                                fracturesEnabled = !fracturesEnabled;
                                context.getSource().sendFeedback(Text.literal("Fracture alerts " + (fracturesEnabled ? "enabled" : "disabled")));
                                saveSettings();
                            }
                            return 1;
                        })
                    )
                    .then(ClientCommandManager.literal("heatdamage")
                        .executes(context -> {
                            if (!useBlackMesaSFX) {
                                heatDamageEnabled = !heatDamageEnabled;
                                context.getSource().sendFeedback(Text.literal("Heat damage alerts " + (heatDamageEnabled ? "enabled" : "disabled")));
                                saveSettings();
                            }
                            return 1;
                        })
                    )
                    .then(ClientCommandManager.literal("bloodloss")
                        .executes(context -> {
                            if (!useBlackMesaSFX) {
                                bloodLossEnabled = !bloodLossEnabled;
                                context.getSource().sendFeedback(Text.literal("Blood loss alerts " + (bloodLossEnabled ? "enabled" : "disabled")));
                                saveSettings();
                            }
                            return 1;
                        })
                    )
                    .then(ClientCommandManager.literal("shockdamage")
                        .executes(context -> {
                            if (!useBlackMesaSFX) {
                                shockDamageEnabled = !shockDamageEnabled;
                                context.getSource().sendFeedback(Text.literal("Shock damage alerts " + (shockDamageEnabled ? "enabled" : "disabled")));
                                saveSettings();
                            }
                            return 1;
                        })
                    )
                    .then(ClientCommandManager.literal("chemicaldamage")
                        .executes(context -> {
                            if (!useBlackMesaSFX) {
                                chemicalDamageEnabled = !chemicalDamageEnabled;
                                context.getSource().sendFeedback(Text.literal("Chemical damage alerts " + (chemicalDamageEnabled ? "enabled" : "disabled")));
                                saveSettings();
                            }
                            return 1;
                        })
                    )
                    .then(ClientCommandManager.literal("morphine")
                        .executes(context -> {
                            if (!useBlackMesaSFX) {
                                morphineEnabled = !morphineEnabled;
                                context.getSource().sendFeedback(Text.literal("Morphine alerts " + (morphineEnabled ? "enabled" : "disabled")));
                                saveSettings();
                            }
                            return 1;
                        })
                    )
                    .executes(context -> {
                        if (!useBlackMesaSFX) {
                            hevSuitEnabled = !hevSuitEnabled;
                            String status = hevSuitEnabled ? "Activated" : "Deactivated";
                            context.getSource().sendFeedback(Text.literal("Voice System " + status));                
                            saveSettings();
                        }
                        return 1;
                    })
                )
                .then(ClientCommandManager.literal("clearqueue")
                    .executes(context -> {
                        soundQueue.clear();
                        context.getSource().sendFeedback(Text.literal("Queue cleared."));
                        return 1;
                    })
                )
                .then(ClientCommandManager.literal("pvp")
                    .executes(context -> {
                        if (!useBlackMesaSFX) {
                            pvpModeEnabled = !pvpModeEnabled;
                            if (pvpModeEnabled) {
                                healthAlertsEnabled = true;
                                hudEnabled = true;
                                fracturesEnabled = false;
                                heatDamageEnabled = false;
                                bloodLossEnabled = false;
                                shockDamageEnabled = false;
                                chemicalDamageEnabled = false;
                                morphineEnabled = false;
                                hevSuitEnabled = false;
                                healthCritical2Enabled = false;
                                context.getSource().sendFeedback(Text.literal("PVP mode activated: Only HUD and health alerts enabled."));
                            } else {
                                healthAlertsEnabled = true;
                                hudEnabled = true;
                                fracturesEnabled = true;
                                heatDamageEnabled = true;
                                bloodLossEnabled = true;
                                shockDamageEnabled = true;
                                chemicalDamageEnabled = true;
                                morphineEnabled = true;
                                hevSuitEnabled = true;
                                healthCritical2Enabled = true;
                                context.getSource().sendFeedback(Text.literal("PVP mode deactivated: All features re-enabled."));
                            }
                            saveSettings();
                        }
                        return 1;
                    })
                )
                .then(ClientCommandManager.literal("queuesound")
                    .then(ClientCommandManager.argument("sound", StringArgumentType.string())
                        .executes(context -> {
                            String soundName = StringArgumentType.getString(context, "sound");
                            if (SOUND_EVENTS.containsKey(soundName)) {
                                queueSound(soundName);
                                context.getSource().sendFeedback(Text.literal("Queuing sound: " + soundName));
                            } else {
                                context.getSource().sendFeedback(Text.literal("Sound not found: " + soundName));
                            }
                            return 1;
                        })
                    )
                )
                .then(ClientCommandManager.literal("toggleSFX")
                    .executes(context -> {
                        useBlackMesaSFX = !useBlackMesaSFX;
                        LOGGER.debug("Toggled Black Mesa SFX: " + useBlackMesaSFX);
                        // Remove the feature toggling, just switch sound sets
                        context.getSource().sendFeedback(Text.literal(useBlackMesaSFX ? 
                            "Black Mesa SFX enabled" : 
                            "Half-Life 1 SFX enabled"));
                        saveSettings();
                        return 1;
                    })
                )
            );
        });
    }

    private void resetTracking() {
        lastHealth = 20.0f;
        lastArmorValue = -1;
        lastMorphineTime = 0;
        lastLacerationTime = 0;
        lastBloodLossTime = 0;
    }
    private int calculateTotalAmmo(PlayerEntity player, Item item) {
        int total = 0;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() == item) {
                total += stack.getCount();
            }
        }
        return total;
    }
    // hud
    private void registerHud() {
        HudRenderCallback.EVENT.register((graphics, tickDelta) -> {
            if (!hudEnabled) return;

            MinecraftClient client = MinecraftClient.getInstance();
            PlayerEntity player = client.player;
    
            if (player == null || client.options.hudHidden) return;
    
            int width = client.getWindow().getScaledWidth();
            int height = client.getWindow().getScaledHeight();
            int baseY = height - 29;
            TextRenderer textRenderer = client.textRenderer;
    
            // Health and Armor display
            int scaledHealth = (int)((player.getHealth() / player.getMaxHealth()) * 100);
            int scaledArmor = (int)((player.getArmor() / 20.0f) * 100);
    
            drawNumericDisplay(graphics, textRenderer, 10, baseY, scaledHealth, "HEALTH");
            if (scaledArmor > 0) {
                drawNumericDisplay(graphics, textRenderer, 10 + 100, baseY, scaledArmor, "ARMOR");
            }
    
            // Ammo display
            ItemStack mainHand = player.getMainHandStack();
            if (!mainHand.isEmpty()) {
                int currentAmmo = mainHand.getCount();
                int totalAmmo = calculateTotalAmmo(player, mainHand.getItem());
                drawAmmoDisplay(graphics, textRenderer, width - 110, baseY, currentAmmo, totalAmmo);
            }
        });
    }
    
    private void drawNumericDisplay(DrawContext graphics, TextRenderer textRenderer, int x, int y, int value, String label) {
        graphics.fill(x - 2, y - 2, x + 90, y + 12, 0x80000000);
        graphics.drawTextWithShadow(textRenderer, String.format("%d", value), x, y, AMBER_COLOR);
        graphics.drawTextWithShadow(textRenderer, label, x, y - 10, DARK_AMBER);
    }
    
    private void drawAmmoDisplay(DrawContext graphics, TextRenderer textRenderer, int x, int y, int currentAmmo, int totalAmmo) {
        graphics.fill(x - 2, y - 2, x + 90, y + 12, 0x80000000);
        graphics.drawTextWithShadow(textRenderer, String.format("%d/%d", currentAmmo, totalAmmo), x, y, AMBER_COLOR);
        graphics.drawTextWithShadow(textRenderer, "AMMO", x, y - 10, DARK_AMBER);
    }
   // client tick: contains power level and calls health alerts
    private void onClientTick(MinecraftClient client) {
        try {
            if (!hevSuitEnabled && !pvpModeEnabled) return;
    
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
    // health system
    private void handleHealthSystem(MinecraftClient client, PlayerEntity player) {
        float currentHealth = player.getHealth();
        long currentTime = System.currentTimeMillis();

        if (currentHealth <= 0) {
            soundQueue.clear();
            wasPoisoned = false;
            return;
        }

        if (player.isOnFire() && heatDamageEnabled && currentTime - lastHeatDamageTime >= HEAT_DAMAGE_COOLDOWN) {
            queueSound(useBlackMesaSFX ? "bm_heat_damage" : "heat_damage");
            lastHeatDamageTime = currentTime;
        }

        if (currentHealth < lastHealth) {
            float damage = lastHealth - currentHealth;
            handleDamage(client, damage, player.getRecentDamageSource());
        }

        // Fix health alerts by removing unnecessary conditions
        if (healthAlertsEnabled) {
            String prefix = useBlackMesaSFX ? "bm_" : "";
            if (currentHealth <= 3.0 && lastHealth > 3.0 && nearDeathEnabled) {
                queueSound(prefix + "near_death");
            } else if (currentHealth <= 5.0 && lastHealth > 5.0 && healthCriticalEnabled) {
                queueSound(prefix + "health_critical");
            } else if (currentHealth <= 10.0 && lastHealth > 10.0 && seekMedicalEnabled) {
                queueSound(prefix + "seek_medical");
            } else if (currentHealth <= 17.0 && lastHealth > 17.0 && healthCritical2Enabled) {
                queueSound(prefix + "health_critical2");
            }
        }

        // Fix morphine system
        if (morphineEnabled && currentTime - lastMorphineTime >= MORPHINE_COOLDOWN && currentHealth < 20) {
            queueSound(useBlackMesaSFX ? "bm_morphine_system" : "morphine_administered");
            lastMorphineTime = currentTime;
        }

        lastHealth = currentHealth;
    }

    private void handleDamage(MinecraftClient client, float damage, DamageSource damageSource) {
        if (damageSource == null) return;
        long currentTime = System.currentTimeMillis();
        String prefix = useBlackMesaSFX ? "bm_" : "";

        // Fix fall damage detection and fractures
        if (damageSource.isOf(DamageTypes.FALL) && fracturesEnabled) {
            if (damage >= 6) {
                queueSound(prefix + "major_fracture");
            } else if (damage >= 3) {
                queueSound(prefix + "minor_fracture");
            }
        }

        // Fix blood loss detection
        if (bloodLossEnabled && currentTime - lastBloodLossTime >= BLOOD_LOSS_COOLDOWN) {
            if (damageSource.isOf(DamageTypes.EXPLOSION) || 
                damageSource.getSource() instanceof TntEntity || 
                damageSource.getSource() instanceof CreeperEntity) {
                queueSound(prefix + "blood_loss");
                lastBloodLossTime = currentTime;
            }
        }

        // Fix chemical damage detection
        if (chemicalDamageEnabled && client.player.hasStatusEffect(StatusEffects.POISON) && !wasPoisoned) {
            queueSound(prefix + "chemical");
            wasPoisoned = true;
        } else if (!client.player.hasStatusEffect(StatusEffects.POISON)) {
            wasPoisoned = false;
        }

        // Fix shock damage
        if (shockDamageEnabled && damageSource.isOf(DamageTypes.LIGHTNING_BOLT) && 
            currentTime - lastShockDamageTime >= SHOCK_DAMAGE_COOLDOWN) {
            queueSound(prefix + "shock_damage");
            lastShockDamageTime = currentTime;
        }

        // Fix laceration detection
        Entity damageEntity = damageSource.getSource();
        if (damageEntity instanceof HostileEntity && fracturesEnabled) {
            if (damage >= 5) {
                queueSound(prefix + "major_laceration");
            } else {
                queueSound(prefix + "minor_laceration");
            }
        }
    }
// sound queue
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
                LOGGER.debug("Playing sound: " + soundName);
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