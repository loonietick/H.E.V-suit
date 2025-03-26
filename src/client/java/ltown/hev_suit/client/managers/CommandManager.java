package ltown.hev_suit.client.managers;

import com.mojang.brigadier.arguments.StringArgumentType;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.text.Style;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.regex.Pattern;

public class CommandManager {

    private static final Logger LOGGER = LogManager.getLogger("CommandManager");
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("^#?([0-9A-Fa-f]{6})$");

    public static void registerToggleCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("hev")
                .then(ClientCommandManager.literal("toggle")
                    .then(ClientCommandManager.literal("hud")
                        .then(ClientCommandManager.literal("all")
                            .executes(context -> {
                                SettingsManager.hudEnabled = !SettingsManager.hudEnabled;
                                context.getSource().sendFeedback(
                                    Text.literal("HUD " + (SettingsManager.hudEnabled ? "enabled" : "disabled"))
                                        .setStyle(Style.EMPTY.withColor(SettingsManager.hudPrimaryColor & 0xFFFFFF))
                                );
                                if (SettingsManager.hudEnabled) {
                                    context.getSource().sendFeedback(
                                        Text.literal("Notice: If you want to hide the vanilla minecraft hearts and armor icons make sure you enable the resourcepack that does so.")
                                            .setStyle(Style.EMPTY.withColor(Formatting.RED))
                                    );
                                } else {
                                    context.getSource().sendFeedback(
                                        Text.literal("Notice: Please go into the Minecraft settings to disable the resourcepack that hides the hearts and armor icons.")
                                            .setStyle(Style.EMPTY.withColor(Formatting.RED))
                                    );
                                }
                                SettingsManager.saveSettings();
                                return 1;
                            })
                        )
                        .then(ClientCommandManager.literal("health")
                            .executes(context -> {
                                SettingsManager.hudHealthEnabled = !SettingsManager.hudHealthEnabled;
                                context.getSource().sendFeedback(
                                    Text.literal("HUD Health " + (SettingsManager.hudHealthEnabled ? "enabled" : "disabled"))
                                        .setStyle(Style.EMPTY.withColor(SettingsManager.hudPrimaryColor & 0xFFFFFF))
                                );
                                SettingsManager.saveSettings();
                                return 1;
                            })
                        )
                        .then(ClientCommandManager.literal("armor")
                            .executes(context -> {
                                SettingsManager.hudArmorEnabled = !SettingsManager.hudArmorEnabled;
                                context.getSource().sendFeedback(
                                    Text.literal("HUD Armor " + (SettingsManager.hudArmorEnabled ? "enabled" : "disabled"))
                                        .setStyle(Style.EMPTY.withColor(SettingsManager.hudPrimaryColor & 0xFFFFFF))
                                );
                                SettingsManager.saveSettings();
                                return 1;
                            })
                        )
                        .then(ClientCommandManager.literal("ammo")
                            .executes(context -> {
                                SettingsManager.hudAmmoEnabled = !SettingsManager.hudAmmoEnabled;
                                context.getSource().sendFeedback(
                                    Text.literal("HUD Ammo " + (SettingsManager.hudAmmoEnabled ? "enabled" : "disabled"))
                                        .setStyle(Style.EMPTY.withColor(SettingsManager.hudPrimaryColor & 0xFFFFFF))
                                );
                                SettingsManager.saveSettings();
                                return 1;
                            })
                        )
                        .then(ClientCommandManager.literal("damageindicators")
                            .executes(context -> {
                                SettingsManager.damageIndicatorsEnabled = !SettingsManager.damageIndicatorsEnabled;
                                context.getSource().sendFeedback(
                                    Text.literal("Damage indicators " + (SettingsManager.damageIndicatorsEnabled ? "enabled" : "disabled"))
                                        .setStyle(Style.EMPTY.withColor(SettingsManager.hudPrimaryColor & 0xFFFFFF))
                                );
                                SettingsManager.saveSettings();
                                return 1;
                            })
                        )
                        .then(ClientCommandManager.literal("mobindicators")
                            .executes(context -> {
                                SettingsManager.threatIndicatorsEnabled = !SettingsManager.threatIndicatorsEnabled;
                                context.getSource().sendFeedback(
                                    Text.literal("Hostile MOB indicators " + (SettingsManager.threatIndicatorsEnabled ? "enabled" : "disabled"))
                                        .setStyle(Style.EMPTY.withColor(SettingsManager.hudPrimaryColor & 0xFFFFFF))
                                );
                                SettingsManager.saveSettings();
                                return 1;
                            })
                        )
                    )
                    .then(ClientCommandManager.literal("voice")
                        .then(ClientCommandManager.literal("all")
                            .executes(context -> {
                                SettingsManager.hevSuitEnabled = !SettingsManager.hevSuitEnabled;
                                String status = SettingsManager.hevSuitEnabled ? "Enabled" : "Disabled";
                                context.getSource().sendFeedback(
                                    Text.literal("The HEV Suit is now: " + status)
                                        .setStyle(Style.EMPTY.withColor(SettingsManager.hudPrimaryColor & 0xFFFFFF))
                                );
                                SettingsManager.saveSettings();
                                return 1;
                            })
                        )
                        .then(ClientCommandManager.literal("morphine")
                            .executes(context -> {
                                SettingsManager.morphineEnabled = !SettingsManager.morphineEnabled;
                                context.getSource().sendFeedback(
                                    Text.literal("Morphine sound effects " + (SettingsManager.morphineEnabled ? "enabled" : "disabled"))
                                        .setStyle(Style.EMPTY.withColor(SettingsManager.hudPrimaryColor & 0xFFFFFF))
                                );
                                SettingsManager.saveSettings();
                                return 1;
                            })
                        )
                        .then(ClientCommandManager.literal("armordurability")
                            .executes(context -> {
                                SettingsManager.armorDurabilityEnabled = !SettingsManager.armorDurabilityEnabled;
                                context.getSource().sendFeedback(
                                    Text.literal("Armor durability sound effects " + 
                                        (SettingsManager.armorDurabilityEnabled ? "enabled" : "disabled"))
                                        .setStyle(Style.EMPTY.withColor(SettingsManager.hudPrimaryColor & 0xFFFFFF))
                                );
                                SettingsManager.saveSettings();
                                return 1;
                            })
                        )
                        .then(ClientCommandManager.literal("fractures")
                            .executes(context -> {
                                SettingsManager.fracturesEnabled = !SettingsManager.fracturesEnabled;
                                context.getSource().sendFeedback(
                                    Text.literal("Fracture alerts " + (SettingsManager.fracturesEnabled ? "enabled" : "disabled"))
                                    .setStyle(Style.EMPTY.withColor(SettingsManager.hudPrimaryColor & 0xFFFFFF))
                                );
                                SettingsManager.saveSettings();
                                return 1;
                            })
                        )
                        .then(ClientCommandManager.literal("heatdamage")
                            .executes(context -> {
                                SettingsManager.heatDamageEnabled = !SettingsManager.heatDamageEnabled;
                                context.getSource().sendFeedback(
                                    Text.literal("Heat Damage alerts " + (SettingsManager.heatDamageEnabled ? "enabled" : "disabled"))
                                    .setStyle(Style.EMPTY.withColor(SettingsManager.hudPrimaryColor & 0xFFFFFF))
                                );
                                SettingsManager.saveSettings();
                                return 1;
                            })
                        )
                        .then(ClientCommandManager.literal("bloodloss")
                            .executes(context -> {
                                SettingsManager.bloodLossEnabled = !SettingsManager.bloodLossEnabled;
                                context.getSource().sendFeedback(
                                    Text.literal("Blood Loss alerts " + (SettingsManager.bloodLossEnabled ? "enabled" : "disabled"))
                                    .setStyle(Style.EMPTY.withColor(SettingsManager.hudPrimaryColor & 0xFFFFFF))
                                );
                                SettingsManager.saveSettings();
                                return 1;
                            })
                        )
                        .then(ClientCommandManager.literal("shockdamage")
                            .executes(context -> {
                                SettingsManager.shockDamageEnabled = !SettingsManager.shockDamageEnabled;
                                context.getSource().sendFeedback(
                                    Text.literal("Shock Damage alerts " + (SettingsManager.shockDamageEnabled ? "enabled" : "disabled"))
                                    .setStyle(Style.EMPTY.withColor(SettingsManager.hudPrimaryColor & 0xFFFFFF))
                                );
                                SettingsManager.saveSettings();
                                return 1;
                            })
                        )
                        .then(ClientCommandManager.literal("chemicaldamage")
                            .executes(context -> {
                                SettingsManager.chemicalDamageEnabled = !SettingsManager.chemicalDamageEnabled;
                                context.getSource().sendFeedback(
                                    Text.literal("Chemical Damage alerts " + (SettingsManager.chemicalDamageEnabled ? "enabled" : "disabled"))
                                    .setStyle(Style.EMPTY.withColor(SettingsManager.hudPrimaryColor & 0xFFFFFF))
                                );
                                SettingsManager.saveSettings();
                                return 1;
                            })
                        )
                        .then(ClientCommandManager.literal("health_critical2")
                            .executes(context -> {
                                SettingsManager.healthCritical2Enabled = !SettingsManager.healthCritical2Enabled;
                                context.getSource().sendFeedback(
                                    Text.literal("Health Critical 2 alerts " + (SettingsManager.healthCritical2Enabled ? "enabled" : "disabled"))
                                    .setStyle(Style.EMPTY.withColor(SettingsManager.hudPrimaryColor & 0xFFFFFF))
                                );
                                SettingsManager.saveSettings();
                                return 1;
                            })
                        )
                        .then(ClientCommandManager.literal("seek_medical")
                            .executes(context -> {
                                SettingsManager.seekMedicalEnabled = !SettingsManager.seekMedicalEnabled;
                                context.getSource().sendFeedback(
                                    Text.literal("Seek Medical alerts " + (SettingsManager.seekMedicalEnabled ? "enabled" : "disabled"))
                                    .setStyle(Style.EMPTY.withColor(SettingsManager.hudPrimaryColor & 0xFFFFFF))
                                );
                                SettingsManager.saveSettings();
                                return 1;
                            })
                        )
                        .then(ClientCommandManager.literal("health_critical")
                            .executes(context -> {
                                SettingsManager.healthCriticalEnabled = !SettingsManager.healthCriticalEnabled;
                                context.getSource().sendFeedback(
                                    Text.literal("Health Critical alerts " + (SettingsManager.healthCriticalEnabled ? "enabled" : "disabled"))
                                    .setStyle(Style.EMPTY.withColor(SettingsManager.hudPrimaryColor & 0xFFFFFF))
                                );
                                SettingsManager.saveSettings();
                                return 1;
                            })
                        )
                        .then(ClientCommandManager.literal("near_death")
                            .executes(context -> {
                                SettingsManager.nearDeathEnabled = !SettingsManager.nearDeathEnabled;
                                context.getSource().sendFeedback(
                                    Text.literal("Near Death alerts " + (SettingsManager.nearDeathEnabled ? "enabled" : "disabled"))
                                    .setStyle(Style.EMPTY.withColor(SettingsManager.hudPrimaryColor & 0xFFFFFF))
                                );
                                SettingsManager.saveSettings();
                                return 1;
                            })
                        )
                    )
                    .then(ClientCommandManager.literal("systems")
                        .then(ClientCommandManager.literal("pvp")
                            .executes(context -> {
                                if (!SettingsManager.useBlackMesaSFX) {
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
                                        context.getSource().sendFeedback(Text.literal("PvP mode has been enabled, All hev suit features besides the hud, health alerts and armor durability tracking are disabled.").setStyle(Style.EMPTY.withColor(SettingsManager.hudPrimaryColor & 0xFFFFFF)));
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
                                        context.getSource().sendFeedback(Text.literal("PVP mode deactivated: All features re-enabled.").setStyle(Style.EMPTY.withColor(SettingsManager.hudPrimaryColor & 0xFFFFFF)));
                                    }
                                    SettingsManager.saveSettings();
                                } else {
                                    SettingsManager.pvpModeEnabled = !SettingsManager.pvpModeEnabled;
                                    if (SettingsManager.pvpModeEnabled) {
                                        SettingsManager.fracturesEnabled = false;
                                        SettingsManager.bloodLossEnabled = false;
                                        SettingsManager.chemicalDamageEnabled = false;
                                        SettingsManager.morphineEnabled = false;
                                        SettingsManager.healthCritical2Enabled = false;
                                        SettingsManager.nearDeathEnabled = false;
                                        SettingsManager.hevSuitEnabled = false;
                                        SettingsManager.healthAlertsEnabled = true;
                                        SettingsManager.hudEnabled = true;
                                        SettingsManager.armorDurabilityEnabled = true;
                                        context.getSource().sendFeedback(
                                            Text.literal("PvP mode has been enabled, All hev suit features besides the hud and health alerts are disabled.")
                                                .setStyle(Style.EMPTY.withColor(SettingsManager.hudPrimaryColor & 0xFFFFFF))
                                        );
                                    } else {
                                        SettingsManager.fracturesEnabled = true;
                                        SettingsManager.bloodLossEnabled = true;
                                        SettingsManager.chemicalDamageEnabled = true;
                                        SettingsManager.morphineEnabled = true;
                                        SettingsManager.healthCritical2Enabled = true;
                                        SettingsManager.nearDeathEnabled = true;
                                        SettingsManager.hevSuitEnabled = true;
                                        SettingsManager.healthAlertsEnabled = true;
                                        SettingsManager.hudEnabled = true;
                                        context.getSource().sendFeedback(
                                            Text.literal("PVP mode deactivated: All features re-enabled.")
                                                .setStyle(Style.EMPTY.withColor(SettingsManager.hudPrimaryColor & 0xFFFFFF))
                                        );
                                    }
                                    SettingsManager.saveSettings();
                                }
                                return 1;
                            })
                        )
                        .then(ClientCommandManager.literal("blackmesasfx")
                            .executes(context -> {
                                SettingsManager.useBlackMesaSFX = !SettingsManager.useBlackMesaSFX;
                                LOGGER.debug("Toggled Black Mesa SFX: " + SettingsManager.useBlackMesaSFX);
                                context.getSource().sendFeedback(
                                    Text.literal(SettingsManager.useBlackMesaSFX ? 
                                        "Black Mesa SFX enabled" : 
                                        "Half-Life 1 SFX enabled")
                                    .setStyle(Style.EMPTY.withColor(SettingsManager.hudPrimaryColor & 0xFFFFFF))
                                );
                                SettingsManager.saveSettings();
                                return 1;
                            })
                        )
                        .then(ClientCommandManager.literal("captions")
                            .executes(context -> {
                                SubtitleManager.toggleCaptions();
                                context.getSource().sendFeedback(
                                    Text.literal("Captions " + (SubtitleManager.areCaptionsEnabled() ? "enabled" : "disabled"))
                                .setStyle(Style.EMPTY.withColor(SettingsManager.hudPrimaryColor & 0xFFFFFF))
                            );
                            return 1;
                        })
                    )
                        .then(ClientCommandManager.literal("all")
                            .executes(context -> {
                                boolean newState = !SettingsManager.hudEnabled;

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

                                context.getSource().sendFeedback(
                                    Text.literal("Toggled all features " + (newState ? "enabled" : "disabled"))
                                        .setStyle(Style.EMPTY.withColor(SettingsManager.hudPrimaryColor & 0xFFFFFF))
                                );
                                SettingsManager.saveSettings();
                                return 1;
                            })
                        )
                    )
                )
                // Add queue list command
                .then(ClientCommandManager.literal("listqueue")
                    .executes(context -> {
                        List<String> queuedSounds = SoundManager.getQueuedSounds();
                        if (queuedSounds.isEmpty()) {
                            context.getSource().sendFeedback(
                                Text.literal("Sound queue is empty")
                                .setStyle(Style.EMPTY.withColor(SettingsManager.hudPrimaryColor & 0xFFFFFF))
                            );
                        } else {
                            context.getSource().sendFeedback(
                                Text.literal("Queued sounds:")
                                .setStyle(Style.EMPTY.withColor(SettingsManager.hudPrimaryColor & 0xFFFFFF))
                            );
                            for (int i = 0; i < queuedSounds.size(); i++) {
                                context.getSource().sendFeedback(
                                    Text.literal(" • " + queuedSounds.get(i))
                                    .setStyle(Style.EMPTY.withColor(SettingsManager.hudPrimaryColor & 0xFFFFFF))
                                );
                            }
                        }
                        return 1;
                    })
                )
                // Update existing command feedbacks with gold color
                .then(ClientCommandManager.literal("clearqueue")
                    .executes(context -> {
                        SoundManager.clearSoundQueue();
                        context.getSource().sendFeedback(
                            Text.literal("Queue cleared.")
                            .setStyle(Style.EMPTY.withColor(SettingsManager.hudPrimaryColor & 0xFFFFFF))
                        );
                        return 1;
                    })
                )
                .then(ClientCommandManager.literal("queuesound")
                    .then(ClientCommandManager.argument("sound", StringArgumentType.string())
                        .executes(context -> {
                            String soundName = StringArgumentType.getString(context, "sound");
                            if (SoundManager.SOUND_EVENTS.containsKey(soundName)) {
                                SoundManager.queueSound(soundName);
                                context.getSource().sendFeedback(Text.literal("Queuing sound: " + soundName).setStyle(Style.EMPTY.withColor(SettingsManager.hudPrimaryColor & 0xFFFFFF)));
                            } else {
                                context.getSource().sendFeedback(Text.literal("Sound not found: " + soundName).setStyle(Style.EMPTY.withColor(SettingsManager.hudPrimaryColor & 0xFFFFFF)));
                            }
                            return 1;
                        })
                    )
                )
                // Add new edit command
                .then(ClientCommandManager.literal("edit")
                    .then(ClientCommandManager.literal("hud")
                        .then(ClientCommandManager.literal("color")
                            .then(ClientCommandManager.argument("hexcolor", StringArgumentType.word())
                                .executes(context -> {
                                    String hexColor = StringArgumentType.getString(context, "hexcolor");
                                    
                                    // Validate hex color format
                                    if (!isValidHexColor(hexColor)) {
                                        context.getSource().sendFeedback(
                                            Text.literal("Invalid hex color format. Use format: #RRGGBB or RRGGBB")
                                                .setStyle(Style.EMPTY.withColor(Formatting.RED))
                                        );
                                        return 0;
                                    }
                                    
                                    // Remove # if present
                                    if (hexColor.startsWith("#")) {
                                        hexColor = hexColor.substring(1);
                                    }
                                    
                                    try {
                                        // Parse the hex color to an integer with alpha
                                        int primaryColor = 0xFF000000 | Integer.parseInt(hexColor, 16);
                                        
                                        // Calculate a darker shade for secondary elements
                                        int secondaryColor = SettingsManager.calculateDarkerShade(primaryColor);
                                        
                                        // Save the colors
                                        SettingsManager.hudPrimaryColor = primaryColor;
                                        SettingsManager.hudSecondaryColor = secondaryColor;
                                        SettingsManager.saveSettings();
                                        
                                        // Display a color preview in chat
                                        context.getSource().sendFeedback(
                                            Text.literal("HUD colors updated.")
                                                .setStyle(Style.EMPTY.withColor(SettingsManager.hudPrimaryColor & 0xFFFFFF))
                                        );
                                        
                                        // Show color preview
                                        context.getSource().sendFeedback(
                                            Text.literal("Primary color: ")
                                                .append(Text.literal("■■■")
                                                    .setStyle(Style.EMPTY.withColor(primaryColor & 0xFFFFFF)))
                                        );
                                        
                                        context.getSource().sendFeedback(
                                            Text.literal("Secondary color: ")
                                                .append(Text.literal("■■■")
                                                    .setStyle(Style.EMPTY.withColor(secondaryColor & 0xFFFFFF)))
                                        );
                                        
                                        // Show reset instructions
                                        context.getSource().sendFeedback(
                                            Text.literal("Use '/hev edit hud color reset' to restore default colors.")
                                                .setStyle(Style.EMPTY.withColor(Formatting.GRAY))
                                        );
                                        
                                        return 1;
                                    } catch (NumberFormatException e) {
                                        context.getSource().sendFeedback(
                                            Text.literal("Error parsing hex color.")
                                                .setStyle(Style.EMPTY.withColor(Formatting.RED))
                                        );
                                        return 0;
                                    }
                                })
                            )
                            .then(ClientCommandManager.literal("reset")
                                .executes(context -> {
                                    // Reset to default Half-Life amber color
                                    SettingsManager.hudPrimaryColor = 0xFFFFAE00;   // Default amber color
                                    SettingsManager.hudSecondaryColor = 0xFF8B5E00; // Default dark amber color
                                    SettingsManager.saveSettings();
                                    
                                    context.getSource().sendFeedback(
                                        Text.literal("HUD colors reset to default.")
                                            .setStyle(Style.EMPTY.withColor(SettingsManager.hudPrimaryColor & 0xFFFFFF))
                                    );
                                    
                                    // Show color preview
                                    context.getSource().sendFeedback(
                                        Text.literal("Primary color: ")
                                            .append(Text.literal("■■■")
                                                .setStyle(Style.EMPTY.withColor(SettingsManager.hudPrimaryColor & 0xFFFFFF)))
                                    );
                                    
                                    context.getSource().sendFeedback(
                                        Text.literal("Secondary color: ")
                                            .append(Text.literal("■■■")
                                                .setStyle(Style.EMPTY.withColor(SettingsManager.hudSecondaryColor & 0xFFFFFF)))
                                    );
                                    
                                    return 1;
                                })
                            )
                            .then(ClientCommandManager.literal("primary")
                                .then(ClientCommandManager.argument("hexcolor", StringArgumentType.word())
                                    .executes(context -> {
                                        String hexColor = StringArgumentType.getString(context, "hexcolor");
                                        if (!isValidHexColor(hexColor)) {
                                            context.getSource().sendFeedback(
                                                Text.literal("Invalid hex color format. Use format: #RRGGBB or RRGGBB")
                                                    .setStyle(Style.EMPTY.withColor(Formatting.RED))
                                            );
                                            return 0;
                                        }
                                        
                                        if (hexColor.startsWith("#")) hexColor = hexColor.substring(1);
                                        try {
                                            int primaryColor = 0xFF000000 | Integer.parseInt(hexColor, 16);
                                            SettingsManager.hudPrimaryColor = primaryColor;
                                            SettingsManager.saveSettings();
                                            
                                            context.getSource().sendFeedback(
                                                Text.literal("Primary HUD color updated.")
                                                    .setStyle(Style.EMPTY.withColor(primaryColor & 0xFFFFFF))
                                            );
                                            return 1;
                                        } catch (NumberFormatException e) {
                                            context.getSource().sendFeedback(
                                                Text.literal("Error parsing hex color.")
                                                    .setStyle(Style.EMPTY.withColor(Formatting.RED))
                                            );
                                            return 0;
                                        }
                                    })
                                )
                            )
                            .then(ClientCommandManager.literal("secondary")
                                .then(ClientCommandManager.argument("hexcolor", StringArgumentType.word())
                                    .executes(context -> {
                                        String hexColor = StringArgumentType.getString(context, "hexcolor");
                                        if (!isValidHexColor(hexColor)) {
                                            context.getSource().sendFeedback(
                                                Text.literal("Invalid hex color format. Use format: #RRGGBB or RRGGBB")
                                                    .setStyle(Style.EMPTY.withColor(Formatting.RED))
                                            );
                                            return 0;
                                        }
                                        
                                        if (hexColor.startsWith("#")) hexColor = hexColor.substring(1);
                                        try {
                                            int secondaryColor = 0xFF000000 | Integer.parseInt(hexColor, 16);
                                            SettingsManager.hudSecondaryColor = secondaryColor;
                                            SettingsManager.saveSettings();
                                            
                                            context.getSource().sendFeedback(
                                                Text.literal("Secondary HUD color updated.")
                                                    .setStyle(Style.EMPTY.withColor(secondaryColor & 0xFFFFFF))
                                            );
                                            return 1;
                                        } catch (NumberFormatException e) {
                                            context.getSource().sendFeedback(
                                                Text.literal("Error parsing hex color.")
                                                    .setStyle(Style.EMPTY.withColor(Formatting.RED))
                                            );
                                            return 0;
                                        }
                                    })
                                )
                            )
                            .then(ClientCommandManager.literal("setall")
                                .then(ClientCommandManager.argument("hexcolor", StringArgumentType.word())
                                    .executes(context -> {
                                        String hexColor = StringArgumentType.getString(context, "hexcolor");
                                        if (!isValidHexColor(hexColor)) {
                                            context.getSource().sendFeedback(
                                                Text.literal("Invalid hex color format. Use format: #RRGGBB or RRGGBB")
                                                    .setStyle(Style.EMPTY.withColor(Formatting.RED))
                                            );
                                            return 0;
                                        }
                                        
                                        if (hexColor.startsWith("#")) hexColor = hexColor.substring(1);
                                        try {
                                            int baseColor = 0xFF000000 | Integer.parseInt(hexColor, 16);
                                            
                                            // Only set primary and secondary colors
                                            int primaryColor = baseColor;
                                            int secondaryColor = SettingsManager.calculateDarkerShade(baseColor);
                                            
                                            SettingsManager.hudPrimaryColor = primaryColor;
                                            SettingsManager.hudSecondaryColor = secondaryColor;
                                            SettingsManager.saveSettings();
                                            
                                            context.getSource().sendFeedback(
                                                Text.literal("HUD colors updated.")
                                                    .setStyle(Style.EMPTY.withColor(primaryColor & 0xFFFFFF))
                                            );
                                            
                                            context.getSource().sendFeedback(
                                                Text.literal("Primary: ")
                                                    .append(Text.literal("■■■")
                                                    .setStyle(Style.EMPTY.withColor(primaryColor & 0xFFFFFF)))
                                            );
                                            context.getSource().sendFeedback(
                                                Text.literal("Secondary: ")
                                                    .append(Text.literal("■■■")
                                                    .setStyle(Style.EMPTY.withColor(secondaryColor & 0xFFFFFF)))
                                            );
                                            
                                            return 1;
                                        } catch (NumberFormatException e) {
                                            context.getSource().sendFeedback(
                                                Text.literal("Error parsing hex color.")
                                                    .setStyle(Style.EMPTY.withColor(Formatting.RED))
                                            );
                                            return 0;
                                        }
                                    })
                                )
                            )
                        )
                    )
                )
            );
        });
    }
    
    private static boolean isValidHexColor(String hexColor) {
        return HEX_COLOR_PATTERN.matcher(hexColor).matches();
    }

 
}
