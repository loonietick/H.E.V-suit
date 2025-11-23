package ltown.hev_suit.client.managers;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import ltown.hev_suit.client.screen.HevSuitConfigScreen;

public class CommandManager {

    private static final Logger LOGGER = LogManager.getLogger("CommandManager");
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("^#?([0-9A-Fa-f]{6})$");

    public static void registerToggleCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            LiteralArgumentBuilder<FabricClientCommandSource> root = ClientCommandManager.literal("hev");
            root.then(buildConfigCommand());
            root.then(buildToggleCommands());
            root.then(buildEditCommands());
            root.then(buildListQueueCommand());
            root.then(buildClearQueueCommand());
            root.then(buildQueueSoundCommand());
            dispatcher.register(root);
        });
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> buildConfigCommand() {
        return ClientCommandManager.literal("config")
                .executes(context -> {
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client != null) {
                        client.execute(() -> {
                            if (client.currentScreen instanceof HevSuitConfigScreen configScreen) {
                                configScreen.close();
                            } else {
                                client.setScreen(new HevSuitConfigScreen(client.currentScreen));
                            }
                        });
                        sendPrimaryFeedback(context.getSource(), "Opening HEV Suit configuration UI.");
                    }
                    return 1;
                });
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> buildToggleCommands() {
        LiteralArgumentBuilder<FabricClientCommandSource> toggle = ClientCommandManager.literal("toggle");
        toggle.then(buildMainToggleSection());
        toggle.then(buildHudToggleSection());
        toggle.then(buildAudibleToggleSection());
        toggle.then(ClientCommandManager.literal("all").executes(CommandManager::toggleAllFeatures));
        return toggle;
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> buildMainToggleSection() {
        LiteralArgumentBuilder<FabricClientCommandSource> main = ClientCommandManager.literal("main");
        main.then(ClientCommandManager.literal("enabled")
                .executes(ctx -> toggleOption(ctx, "HEV Suit", () -> SettingsManager.hevSuitEnabled, value -> SettingsManager.hevSuitEnabled = value)));
        main.then(ClientCommandManager.literal("pvp").executes(CommandManager::togglePvpMode));
        main.then(ClientCommandManager.literal("captions").executes(CommandManager::toggleCaptions));
        return main;
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> buildHudToggleSection() {
        LiteralArgumentBuilder<FabricClientCommandSource> hud = ClientCommandManager.literal("hud");
        hud.then(ClientCommandManager.literal("enabled").executes(CommandManager::toggleHudEnabled));
        hud.then(ClientCommandManager.literal("health")
                .executes(ctx -> toggleOption(ctx, "HUD Health", () -> SettingsManager.hudHealthEnabled, value -> SettingsManager.hudHealthEnabled = value)));
        hud.then(ClientCommandManager.literal("armor")
                .executes(ctx -> toggleOption(ctx, "HUD Armor", () -> SettingsManager.hudArmorEnabled, value -> SettingsManager.hudArmorEnabled = value)));
        hud.then(ClientCommandManager.literal("ammo")
                .executes(ctx -> toggleOption(ctx, "HUD Ammo", () -> SettingsManager.hudAmmoEnabled, value -> SettingsManager.hudAmmoEnabled = value)));
        hud.then(ClientCommandManager.literal("damage_indicators")
                .executes(ctx -> toggleOption(ctx, "Damage indicators", () -> SettingsManager.damageIndicatorsEnabled, value -> SettingsManager.damageIndicatorsEnabled = value)));
        hud.then(ClientCommandManager.literal("threat_indicators")
                .executes(ctx -> toggleOption(ctx, "Threat indicators", () -> SettingsManager.threatIndicatorsEnabled, value -> SettingsManager.threatIndicatorsEnabled = value)));
        hud.then(ClientCommandManager.literal("align")
                .executes(ctx -> toggleOption(ctx, "HUD alignment mode", () -> SettingsManager.hudAlignmentMode, value -> SettingsManager.hudAlignmentMode = value)));
        return hud;
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> buildAudibleToggleSection() {
        LiteralArgumentBuilder<FabricClientCommandSource> audible = ClientCommandManager.literal("audible");
        audible.then(buildAudibleGeneralSection());
        audible.then(buildAudibleHealthSection());
        audible.then(buildAudibleWeaponSection());
        return audible;
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> buildAudibleGeneralSection() {
        LiteralArgumentBuilder<FabricClientCommandSource> general = ClientCommandManager.literal("general");
        addToggleCommands(general,
                new ToggleDescriptor("fractures", "Fracture alerts", () -> SettingsManager.fracturesEnabled, value -> SettingsManager.fracturesEnabled = value),
                new ToggleDescriptor("blood_loss", "Blood loss alerts", () -> SettingsManager.bloodLossEnabled, value -> SettingsManager.bloodLossEnabled = value),
                new ToggleDescriptor("morphine", "Morphine announcements", () -> SettingsManager.morphineEnabled, value -> SettingsManager.morphineEnabled = value),
                new ToggleDescriptor("armor_durability", "Armor durability alerts", () -> SettingsManager.armorDurabilityEnabled, value -> SettingsManager.armorDurabilityEnabled = value),
                new ToggleDescriptor("heat_damage", "Heat damage alerts", () -> SettingsManager.heatDamageEnabled, value -> SettingsManager.heatDamageEnabled = value),
                new ToggleDescriptor("shock_damage", "Shock damage alerts", () -> SettingsManager.shockDamageEnabled, value -> SettingsManager.shockDamageEnabled = value),
                new ToggleDescriptor("chemical_damage", "Chemical damage alerts", () -> SettingsManager.chemicalDamageEnabled, value -> SettingsManager.chemicalDamageEnabled = value),
                new ToggleDescriptor("hev_damage", "HEV damage alerts", () -> SettingsManager.hevDamageEnabled, value -> SettingsManager.hevDamageEnabled = value),
                new ToggleDescriptor("power_overload", "Power armor overload alerts", () -> SettingsManager.powerArmorOverloadEnabled, value -> SettingsManager.powerArmorOverloadEnabled = value),
                new ToggleDescriptor("hev_logon", "HEV logon sound", () -> SettingsManager.hevLogonEnabled, value -> SettingsManager.hevLogonEnabled = value),
                new ToggleDescriptor("elytra_equip", "Elytra equip SFX", () -> SettingsManager.elytraEquipSfxEnabled, value -> SettingsManager.elytraEquipSfxEnabled = value),
                new ToggleDescriptor("radiation", "Radiation alerts", () -> SettingsManager.radiationSfxEnabled, value -> SettingsManager.radiationSfxEnabled = value)
        );
        return general;
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> buildAudibleHealthSection() {
        LiteralArgumentBuilder<FabricClientCommandSource> health = ClientCommandManager.literal("health");
        addToggleCommands(health,
                new ToggleDescriptor("alerts", "Health alerts", () -> SettingsManager.healthAlertsEnabled, value -> SettingsManager.healthAlertsEnabled = value),
                new ToggleDescriptor("health_critical_2", "Health critical 2 alerts", () -> SettingsManager.healthCritical2Enabled, value -> SettingsManager.healthCritical2Enabled = value),
                new ToggleDescriptor("seek_medical", "Seek medical alerts", () -> SettingsManager.seekMedicalEnabled, value -> SettingsManager.seekMedicalEnabled = value),
                new ToggleDescriptor("health_critical", "Health critical alerts", () -> SettingsManager.healthCriticalEnabled, value -> SettingsManager.healthCriticalEnabled = value),
                new ToggleDescriptor("near_death", "Near death alerts", () -> SettingsManager.nearDeathEnabled, value -> SettingsManager.nearDeathEnabled = value),
                new ToggleDescriptor("insufficient_medical", "Insufficient medical warnings", () -> SettingsManager.insufficientMedicalEnabled, value -> SettingsManager.insufficientMedicalEnabled = value),
                new ToggleDescriptor("administering_medical", "Administering medical alerts", () -> SettingsManager.administeringMedicalEnabled, value -> SettingsManager.administeringMedicalEnabled = value),
                new ToggleDescriptor("death_sfx", "Death flatline SFX", () -> SettingsManager.deathSfxEnabled, value -> SettingsManager.deathSfxEnabled = value),
                new ToggleDescriptor("internal_bleeding", "Internal bleeding alerts", () -> SettingsManager.internalBleedingEnabled, value -> SettingsManager.internalBleedingEnabled = value)
        );
        return health;
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> buildAudibleWeaponSection() {
        LiteralArgumentBuilder<FabricClientCommandSource> weapons = ClientCommandManager.literal("weapons");
        addToggleCommands(weapons,
                new ToggleDescriptor("acquisition", "Weapon acquisition alerts", () -> SettingsManager.weaponPickupEnabled, value -> SettingsManager.weaponPickupEnabled = value),
                new ToggleDescriptor("ammunition", "Ammunition depletion alerts", () -> SettingsManager.ammoDepletedEnabled, value -> SettingsManager.ammoDepletedEnabled = value)
        );
        return weapons;
    }

    private static void addToggleCommands(LiteralArgumentBuilder<FabricClientCommandSource> parent, ToggleDescriptor... toggles) {
        for (ToggleDescriptor toggle : toggles) {
            parent.then(ClientCommandManager.literal(toggle.command())
                    .executes(ctx -> toggleOption(ctx, toggle.label(), toggle.getter(), toggle.setter())));
        }
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> buildEditCommands() {
        LiteralArgumentBuilder<FabricClientCommandSource> edit = ClientCommandManager.literal("edit");
        edit.then(buildHudColorCommands());
        edit.then(buildWeaponKeywordCommands());
        return edit;
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> buildHudColorCommands() {
        LiteralArgumentBuilder<FabricClientCommandSource> hud = ClientCommandManager.literal("hud");
        LiteralArgumentBuilder<FabricClientCommandSource> color = ClientCommandManager.literal("color");
        color.then(ClientCommandManager.argument("hexcolor", StringArgumentType.word())
                .executes(context -> setBothHudColors(context, StringArgumentType.getString(context, "hexcolor"))));
        color.then(ClientCommandManager.literal("reset").executes(CommandManager::resetHudColors));
        color.then(ClientCommandManager.literal("primary")
                .then(ClientCommandManager.argument("hexcolor", StringArgumentType.word())
                        .executes(context -> setPrimaryColor(context, StringArgumentType.getString(context, "hexcolor")))));
        color.then(ClientCommandManager.literal("secondary")
                .then(ClientCommandManager.argument("hexcolor", StringArgumentType.word())
                        .executes(context -> setSecondaryColor(context, StringArgumentType.getString(context, "hexcolor")))));
        color.then(ClientCommandManager.literal("setall")
                .then(ClientCommandManager.argument("hexcolor", StringArgumentType.word())
                        .executes(context -> setBothHudColors(context, StringArgumentType.getString(context, "hexcolor")))));
        hud.then(color);
        return hud;
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> buildWeaponKeywordCommands() {
        LiteralArgumentBuilder<FabricClientCommandSource> weapons = ClientCommandManager.literal("weapons");
        LiteralArgumentBuilder<FabricClientCommandSource> keywords = ClientCommandManager.literal("keywords");
        keywords.then(ClientCommandManager.literal("list").executes(CommandManager::listWeaponKeywords));
        keywords.then(ClientCommandManager.literal("set")
                .then(ClientCommandManager.argument("keywords", StringArgumentType.greedyString())
                        .executes(context -> setWeaponKeywords(context, StringArgumentType.getString(context, "keywords")))));
        keywords.then(ClientCommandManager.literal("add")
                .then(ClientCommandManager.argument("keyword", StringArgumentType.word())
                        .executes(context -> addWeaponKeyword(context, StringArgumentType.getString(context, "keyword")))));
        keywords.then(ClientCommandManager.literal("remove")
                .then(ClientCommandManager.argument("keyword", StringArgumentType.word())
                        .executes(context -> removeWeaponKeyword(context, StringArgumentType.getString(context, "keyword")))));
        weapons.then(keywords);
        return weapons;
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> buildListQueueCommand() {
        return ClientCommandManager.literal("listqueue").executes(context -> {
            List<String> queuedSounds = SoundManager.getQueuedSounds();
            if (queuedSounds.isEmpty()) {
                sendPrimaryFeedback(context.getSource(), "Sound queue is empty");
            } else {
                sendPrimaryFeedback(context.getSource(), "Queued sounds:");
                for (String sound : queuedSounds) {
                    sendPrimaryFeedback(context.getSource(), " • " + sound);
                }
            }
            return 1;
        });
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> buildClearQueueCommand() {
        return ClientCommandManager.literal("clearqueue").executes(context -> {
            SoundManager.clearSoundQueue();
            sendPrimaryFeedback(context.getSource(), "Queue cleared.");
            return 1;
        });
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> buildQueueSoundCommand() {
        return ClientCommandManager.literal("queuesound")
                .then(ClientCommandManager.argument("sound", StringArgumentType.string())
                        .executes(context -> {
                            String soundName = StringArgumentType.getString(context, "sound");
                            if (SoundManager.SOUND_EVENTS.containsKey(soundName)) {
                                SoundManager.queueSound(soundName);
                                sendPrimaryFeedback(context.getSource(), "Queuing sound: " + soundName);
                            } else {
                                sendPrimaryFeedback(context.getSource(), "Sound not found: " + soundName);
                            }
                            return 1;
                        }));
    }

    private static int toggleHudEnabled(CommandContext<FabricClientCommandSource> context) {
        boolean newValue = !SettingsManager.hudEnabled;
        SettingsManager.hudEnabled = newValue;
        sendPrimaryFeedback(context.getSource(), "HUD " + (newValue ? "enabled" : "disabled"));
        if (newValue) {
            context.getSource().sendFeedback(
                    Text.literal("Notice: Enable the resource pack that hides vanilla hearts/armor if desired.")
                            .setStyle(Style.EMPTY.withColor(Formatting.RED)));
        } else {
            context.getSource().sendFeedback(
                    Text.literal("Notice: Disable the resource pack hiding vanilla hearts/armor.")
                            .setStyle(Style.EMPTY.withColor(Formatting.RED)));
        }
        SettingsManager.saveSettings();
        return 1;
    }

    private static int togglePvpMode(CommandContext<FabricClientCommandSource> context) {
        SettingsManager.pvpModeEnabled = !SettingsManager.pvpModeEnabled;
        if (SettingsManager.pvpModeEnabled) {
            SettingsManager.healthAlertsEnabled = true;
            SettingsManager.hudEnabled = true;
            SettingsManager.fracturesEnabled = false;
            SettingsManager.heatDamageEnabled = false;
            SettingsManager.bloodLossEnabled = false;
            SettingsManager.shockDamageEnabled = false;
            SettingsManager.chemicalDamageEnabled = false;
            SettingsManager.morphineEnabled = false;
            SettingsManager.hevSuitEnabled = false;
            SettingsManager.healthCritical2Enabled = false;
            SettingsManager.armorDurabilityEnabled = true;
            sendPrimaryFeedback(context.getSource(), "PvP mode enabled: limited alerts remain active.");
        } else {
            SettingsManager.healthAlertsEnabled = true;
            SettingsManager.hudEnabled = true;
            SettingsManager.fracturesEnabled = true;
            SettingsManager.heatDamageEnabled = true;
            SettingsManager.bloodLossEnabled = true;
            SettingsManager.shockDamageEnabled = true;
            SettingsManager.chemicalDamageEnabled = true;
            SettingsManager.morphineEnabled = true;
            SettingsManager.hevSuitEnabled = true;
            SettingsManager.healthCritical2Enabled = true;
            sendPrimaryFeedback(context.getSource(), "PvP mode disabled: all features restored.");
        }
        SettingsManager.saveSettings();
        return 1;
    }

    private static int toggleCaptions(CommandContext<FabricClientCommandSource> context) {
        SubtitleManager.toggleCaptions();
        SettingsManager.captionsEnabled = SubtitleManager.areCaptionsEnabled();
        sendPrimaryFeedback(context.getSource(), "Captions " + (SettingsManager.captionsEnabled ? "enabled" : "disabled"));
        SettingsManager.saveSettings();
        return 1;
    }

    private static int toggleAllFeatures(CommandContext<FabricClientCommandSource> context) {
        boolean newState = !(SettingsManager.hudEnabled && SettingsManager.hevSuitEnabled);
        SettingsManager.hudEnabled = newState;
        SettingsManager.hudHealthEnabled = newState;
        SettingsManager.hudArmorEnabled = newState;
        SettingsManager.hudAmmoEnabled = newState;
        SettingsManager.damageIndicatorsEnabled = newState;
        SettingsManager.threatIndicatorsEnabled = newState;
        SettingsManager.hevSuitEnabled = newState;
        SettingsManager.morphineEnabled = newState;
        SettingsManager.armorDurabilityEnabled = newState;
        SettingsManager.fracturesEnabled = newState;
        SettingsManager.heatDamageEnabled = newState;
        SettingsManager.bloodLossEnabled = newState;
        SettingsManager.shockDamageEnabled = newState;
        SettingsManager.chemicalDamageEnabled = newState;
        SettingsManager.healthCritical2Enabled = newState;
        SettingsManager.seekMedicalEnabled = newState;
        SettingsManager.healthCriticalEnabled = newState;
        SettingsManager.nearDeathEnabled = newState;
        SettingsManager.insufficientMedicalEnabled = newState;
        SettingsManager.administeringMedicalEnabled = newState;
        SettingsManager.deathSfxEnabled = newState;
        SettingsManager.internalBleedingEnabled = newState;
        SettingsManager.hevDamageEnabled = newState;
        SettingsManager.powerArmorOverloadEnabled = newState;
        SettingsManager.hevLogonEnabled = newState;
        SettingsManager.elytraEquipSfxEnabled = newState;
        SettingsManager.weaponPickupEnabled = newState;
        SettingsManager.ammoDepletedEnabled = newState;
        SettingsManager.radiationSfxEnabled = newState;
        sendPrimaryFeedback(context.getSource(), "All features " + (newState ? "enabled" : "disabled"));
        SettingsManager.saveSettings();
        return 1;
    }

    private static int toggleOption(CommandContext<FabricClientCommandSource> context, String label, BooleanSupplier getter, Consumer<Boolean> setter) {
        boolean newValue = !getter.getAsBoolean();
        setter.accept(newValue);
        SettingsManager.saveSettings();
        sendPrimaryFeedback(context.getSource(), label + " " + (newValue ? "enabled" : "disabled"));
        return 1;
    }

    private static int setPrimaryColor(CommandContext<FabricClientCommandSource> context, String hex) {
        Integer color = parseColor(hex);
        if (color == null) {
            sendInvalidColor(context);
            return 0;
        }
        SettingsManager.hudPrimaryColor = color;
        SettingsManager.saveSettings();
        sendPrimaryFeedback(context.getSource(), "Primary HUD color updated.");
        return 1;
    }

    private static int setSecondaryColor(CommandContext<FabricClientCommandSource> context, String hex) {
        Integer color = parseColor(hex);
        if (color == null) {
            sendInvalidColor(context);
            return 0;
        }
        SettingsManager.hudSecondaryColor = color;
        SettingsManager.saveSettings();
        sendPrimaryFeedback(context.getSource(), "Secondary HUD color updated.");
        return 1;
    }

    private static int setBothHudColors(CommandContext<FabricClientCommandSource> context, String hex) {
        Integer color = parseColor(hex);
        if (color == null) {
            sendInvalidColor(context);
            return 0;
        }
        int secondary = SettingsManager.calculateDarkerShade(color);
        SettingsManager.hudPrimaryColor = color;
        SettingsManager.hudSecondaryColor = secondary;
        SettingsManager.saveSettings();
        sendPrimaryFeedback(context.getSource(), "HUD colors updated.");
        context.getSource().sendFeedback(colorPreview("Primary", color));
        context.getSource().sendFeedback(colorPreview("Secondary", secondary));
        return 1;
    }

    private static int resetHudColors(CommandContext<FabricClientCommandSource> context) {
        SettingsManager.hudPrimaryColor = 0xFFFFAE00;
        SettingsManager.hudSecondaryColor = 0xFF8B5E00;
        SettingsManager.saveSettings();
        sendPrimaryFeedback(context.getSource(), "HUD colors reset to default.");
        context.getSource().sendFeedback(colorPreview("Primary", SettingsManager.hudPrimaryColor));
        context.getSource().sendFeedback(colorPreview("Secondary", SettingsManager.hudSecondaryColor));
        return 1;
    }

    private static int listWeaponKeywords(CommandContext<FabricClientCommandSource> context) {
        List<String> keywords = SettingsManager.weaponKeywords;
        if (keywords.isEmpty()) {
            sendPrimaryFeedback(context.getSource(), "No weapon keywords configured.");
        } else {
            sendPrimaryFeedback(context.getSource(), "Weapon keywords: " + String.join(", ", keywords));
        }
        return 1;
    }

    private static int setWeaponKeywords(CommandContext<FabricClientCommandSource> context, String raw) {
        List<String> parsed = parseKeywords(raw);
        SettingsManager.weaponKeywords = parsed;
        SettingsManager.saveSettings();
        sendPrimaryFeedback(context.getSource(), "Weapon keywords updated (" + parsed.size() + ").");
        return 1;
    }

    private static int addWeaponKeyword(CommandContext<FabricClientCommandSource> context, String keyword) {
        String cleaned = keyword.trim().toLowerCase();
        if (cleaned.isEmpty()) {
            sendPrimaryFeedback(context.getSource(), "Keyword cannot be empty.");
            return 0;
        }
        if (!SettingsManager.weaponKeywords.contains(cleaned)) {
            SettingsManager.weaponKeywords.add(cleaned);
            SettingsManager.saveSettings();
            sendPrimaryFeedback(context.getSource(), "Added weapon keyword: " + cleaned);
        } else {
            sendPrimaryFeedback(context.getSource(), "Keyword already present: " + cleaned);
        }
        return 1;
    }

    private static int removeWeaponKeyword(CommandContext<FabricClientCommandSource> context, String keyword) {
        String cleaned = keyword.trim().toLowerCase();
        if (SettingsManager.weaponKeywords.remove(cleaned)) {
            SettingsManager.saveSettings();
            sendPrimaryFeedback(context.getSource(), "Removed weapon keyword: " + cleaned);
        } else {
            sendPrimaryFeedback(context.getSource(), "Keyword not found: " + cleaned);
        }
        return 1;
    }

    private static Integer parseColor(String hexColor) {
        if (!isValidHexColor(hexColor)) {
            return null;
        }
        String digits = hexColor.startsWith("#") ? hexColor.substring(1) : hexColor;
        try {
            return 0xFF000000 | Integer.parseInt(digits, 16);
        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid color value supplied: {}", hexColor, e);
            return null;
        }
    }

    private static List<String> parseKeywords(String raw) {
        Set<String> unique = new LinkedHashSet<>();
        if (raw != null) {
            String[] parts = raw.split(",");
            for (String part : parts) {
                String value = part.trim().toLowerCase();
                if (!value.isEmpty()) {
                    unique.add(value);
                }
            }
        }
        return new ArrayList<>(unique);
    }

    private static void sendInvalidColor(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(
                Text.literal("Invalid hex color. Use format: #RRGGBB or RRGGBB")
                        .setStyle(Style.EMPTY.withColor(Formatting.RED)));
    }

    private static Text colorPreview(String label, int color) {
        return Text.literal(label + ": ")
                .append(Text.literal("■■■").setStyle(Style.EMPTY.withColor(color & 0xFFFFFF)));
    }

    private static void sendPrimaryFeedback(FabricClientCommandSource source, String message) {
        source.sendFeedback(Text.literal(message).setStyle(Style.EMPTY.withColor(SettingsManager.hudPrimaryColor & 0xFFFFFF)));
    }

    private static boolean isValidHexColor(String hexColor) {
        return HEX_COLOR_PATTERN.matcher(hexColor).matches();
    }

    private record ToggleDescriptor(String command, String label, BooleanSupplier getter, Consumer<Boolean> setter) { }
}
