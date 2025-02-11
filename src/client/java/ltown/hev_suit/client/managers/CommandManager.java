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

public class CommandManager {

    private static final Logger LOGGER = LogManager.getLogger("CommandManager");

    public static void registerToggleCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("hev")
                .then(ClientCommandManager.literal("toggle")
                    // Add captions toggle here
                    .then(ClientCommandManager.literal("captions")
                        .executes(context -> {
                            SubtitleManager.toggleCaptions();
                            context.getSource().sendFeedback(
                                Text.literal("HEV Suit captions " + 
                                (SubtitleManager.areCaptionsEnabled() ? "enabled" : "disabled"))
                                .setStyle(Style.EMPTY.withColor(Formatting.GOLD))
                            );
                            return 1;
                        })
                    )
                    // Add Black Mesa SFX toggle here
                    .then(ClientCommandManager.literal("blackmesasfx")
                        .executes(context -> {
                            SettingsManager.useBlackMesaSFX = !SettingsManager.useBlackMesaSFX;
                            LOGGER.debug("Toggled Black Mesa SFX: " + SettingsManager.useBlackMesaSFX);
                            context.getSource().sendFeedback(
                                Text.literal(SettingsManager.useBlackMesaSFX ? 
                                    "Black Mesa SFX enabled" : 
                                    "Half-Life 1 SFX enabled")
                                .setStyle(Style.EMPTY.withColor(Formatting.GOLD))
                            );
                            SettingsManager.saveSettings();
                            return 1;
                        })
                    )
                    .then(ClientCommandManager.literal("health")
                        .then(ClientCommandManager.literal("health_critical2")
                            .executes(context -> {
                                if (!SettingsManager.useBlackMesaSFX) {
                                    SettingsManager.healthCritical2Enabled = !SettingsManager.healthCritical2Enabled;
                                    context.getSource().sendFeedback(Text.literal("Health Critical 2 alerts " + (SettingsManager.healthCritical2Enabled ? "enabled" : "disabled")).setStyle(Style.EMPTY.withColor(Formatting.GOLD)));
                                    SettingsManager.saveSettings();
                                }
                                return 1;
                            })
                        )
                        .then(ClientCommandManager.literal("seek_medical")
                            .executes(context -> {
                                if (!SettingsManager.useBlackMesaSFX) {
                                    SettingsManager.seekMedicalEnabled = !SettingsManager.seekMedicalEnabled;
                                    context.getSource().sendFeedback(Text.literal("Seek Medical alerts " + (SettingsManager.seekMedicalEnabled ? "enabled" : "disabled")).setStyle(Style.EMPTY.withColor(Formatting.GOLD)));
                                    SettingsManager.saveSettings();
                                }
                                return 1;
                            })
                        )
                        .then(ClientCommandManager.literal("health_critical")
                            .executes(context -> {
                                if (!SettingsManager.useBlackMesaSFX) {
                                    SettingsManager.healthCriticalEnabled = !SettingsManager.healthCriticalEnabled;
                                    context.getSource().sendFeedback(Text.literal("Health Critical alerts " + (SettingsManager.healthCriticalEnabled ? "enabled" : "disabled")).setStyle(Style.EMPTY.withColor(Formatting.GOLD)));
                                    SettingsManager.saveSettings();
                                }
                                return 1;
                            })
                        )
                        .then(ClientCommandManager.literal("near_death")
                            .executes(context -> {
                                if (!SettingsManager.useBlackMesaSFX) {
                                    SettingsManager.nearDeathEnabled = !SettingsManager.nearDeathEnabled;
                                    context.getSource().sendFeedback(Text.literal("Near Death alerts " + (SettingsManager.nearDeathEnabled ? "enabled" : "disabled")).setStyle(Style.EMPTY.withColor(Formatting.GOLD)));
                                    SettingsManager.saveSettings();
                                }
                                return 1;
                            })
                        )
                    )
                    .then(ClientCommandManager.literal("all")
                        .executes(context -> {
                            if (!SettingsManager.useBlackMesaSFX) {
                                boolean newState = !(SettingsManager.healthAlertsEnabled && SettingsManager.hudEnabled && SettingsManager.fracturesEnabled && SettingsManager.heatDamageEnabled && SettingsManager.bloodLossEnabled && SettingsManager.shockDamageEnabled && SettingsManager.chemicalDamageEnabled && SettingsManager.morphineEnabled && SettingsManager.healthCritical2Enabled && SettingsManager.seekMedicalEnabled && SettingsManager.healthCriticalEnabled && SettingsManager.nearDeathEnabled);
                                SettingsManager.healthAlertsEnabled = newState;
                                SettingsManager.hudEnabled = newState;
                                SettingsManager.fracturesEnabled = newState;
                                SettingsManager.heatDamageEnabled = newState;
                                SettingsManager.bloodLossEnabled = newState;
                                SettingsManager.shockDamageEnabled = newState;
                                SettingsManager.chemicalDamageEnabled = newState;
                                SettingsManager.morphineEnabled = newState;
                                SettingsManager.healthCritical2Enabled = newState;
                                SettingsManager.seekMedicalEnabled = newState;
                                SettingsManager.healthCriticalEnabled = newState;
                                SettingsManager.nearDeathEnabled = newState;
                                context.getSource().sendFeedback(Text.literal("All alerts " + (newState ? "enabled" : "disabled")).setStyle(Style.EMPTY.withColor(Formatting.GOLD)));
                                SettingsManager.saveSettings();
                            }
                            return 1;
                        })
                    )
                    .then(ClientCommandManager.literal("hud")
                        .executes(context -> {
                            if (!SettingsManager.useBlackMesaSFX) {
                                SettingsManager.hudEnabled = !SettingsManager.hudEnabled;
                                context.getSource().sendFeedback(Text.literal("HUD " + (SettingsManager.hudEnabled ? "enabled" : "disabled")).setStyle(Style.EMPTY.withColor(Formatting.GOLD)));
                                SettingsManager.saveSettings();
                            }
                            return 1;
                        })
                    )
                    .then(ClientCommandManager.literal("fractures")
                        .executes(context -> {
                            if (!SettingsManager.useBlackMesaSFX) {
                                SettingsManager.fracturesEnabled = !SettingsManager.fracturesEnabled;
                                context.getSource().sendFeedback(Text.literal("Fracture alerts " + (SettingsManager.fracturesEnabled ? "enabled" : "disabled")).setStyle(Style.EMPTY.withColor(Formatting.GOLD)));
                                SettingsManager.saveSettings();
                            }
                            return 1;
                        })
                    )
                    .then(ClientCommandManager.literal("heatdamage")
                        .executes(context -> {
                            if (!SettingsManager.useBlackMesaSFX) {
                                SettingsManager.heatDamageEnabled = !SettingsManager.heatDamageEnabled;
                                context.getSource().sendFeedback(Text.literal("Heat damage alerts " + (SettingsManager.heatDamageEnabled ? "enabled" : "disabled")).setStyle(Style.EMPTY.withColor(Formatting.GOLD)));
                                SettingsManager.saveSettings();
                            }
                            return 1;
                        })
                    )
                    .then(ClientCommandManager.literal("bloodloss")
                        .executes(context -> {
                            if (!SettingsManager.useBlackMesaSFX) {
                                SettingsManager.bloodLossEnabled = !SettingsManager.bloodLossEnabled;
                                context.getSource().sendFeedback(Text.literal("Blood loss alerts " + (SettingsManager.bloodLossEnabled ? "enabled" : "disabled")).setStyle(Style.EMPTY.withColor(Formatting.GOLD)));
                                SettingsManager.saveSettings();
                            }
                            return 1;
                        })
                    )
                    .then(ClientCommandManager.literal("shockdamage")
                        .executes(context -> {
                            if (!SettingsManager.useBlackMesaSFX) {
                                SettingsManager.shockDamageEnabled = !SettingsManager.shockDamageEnabled;
                                context.getSource().sendFeedback(Text.literal("Shock damage alerts " + (SettingsManager.shockDamageEnabled ? "enabled" : "disabled")).setStyle(Style.EMPTY.withColor(Formatting.GOLD)));
                                SettingsManager.saveSettings();
                            }
                            return 1;
                        })
                    )
                    .then(ClientCommandManager.literal("chemicaldamage")
                        .executes(context -> {
                            if (!SettingsManager.useBlackMesaSFX) {
                                SettingsManager.chemicalDamageEnabled = !SettingsManager.chemicalDamageEnabled;
                                context.getSource().sendFeedback(Text.literal("Chemical damage alerts " + (SettingsManager.chemicalDamageEnabled ? "enabled" : "disabled")).setStyle(Style.EMPTY.withColor(Formatting.GOLD)));
                                SettingsManager.saveSettings();
                            }
                            return 1;
                        })
                    )
                    .then(ClientCommandManager.literal("morphine")
                        .executes(context -> {
                            if (!SettingsManager.useBlackMesaSFX) {
                                SettingsManager.morphineEnabled = !SettingsManager.morphineEnabled;
                                context.getSource().sendFeedback(Text.literal("Morphine alerts " + (SettingsManager.morphineEnabled ? "enabled" : "disabled")).setStyle(Style.EMPTY.withColor(Formatting.GOLD)));
                                SettingsManager.saveSettings();
                            }
                            return 1;
                        })
                    )
                    // Add armordurability as part of toggle commands
                    .then(ClientCommandManager.literal("armordurability")
                        .executes(context -> {
                            if (!SettingsManager.useBlackMesaSFX) {
                                SettingsManager.armorDurabilityEnabled = !SettingsManager.armorDurabilityEnabled;
                                context.getSource().sendFeedback(Text.literal("Armor durability monitoring " + 
                                    (SettingsManager.armorDurabilityEnabled ? "enabled" : "disabled")).setStyle(Style.EMPTY.withColor(Formatting.GOLD)));
                                SettingsManager.saveSettings();
                            }
                            return 1;
                        })
                    )
                    // Add damageindicators as part of toggle commands
                    .then(ClientCommandManager.literal("damageindicators")
                        .executes(context -> {
                            if (!SettingsManager.useBlackMesaSFX) {
                                SettingsManager.damageIndicatorsEnabled = !SettingsManager.damageIndicatorsEnabled;
                                context.getSource().sendFeedback(
                                    Text.literal("Damage indicators " + (SettingsManager.damageIndicatorsEnabled ? "enabled" : "disabled"))
                                        .setStyle(Style.EMPTY.withColor(Formatting.GOLD))
                                );
                                SettingsManager.saveSettings();
                            }
                            return 1;
                        })
                    )
                    .executes(context -> {
                        if (!SettingsManager.useBlackMesaSFX) {
                            SettingsManager.hevSuitEnabled = !SettingsManager.hevSuitEnabled;
                            String status = SettingsManager.hevSuitEnabled ? "Activated" : "Deactivated";
                            context.getSource().sendFeedback(
                                Text.literal("Voice System " + status)
                                .setStyle(Style.EMPTY.withColor(Formatting.GOLD))
                            );
                            SettingsManager.saveSettings();
                        }
                        return 1;
                    })
                )
                // Add queue list command
                .then(ClientCommandManager.literal("listqueue")
                    .executes(context -> {
                        List<String> queuedSounds = SoundManager.getQueuedSounds();
                        if (queuedSounds.isEmpty()) {
                            context.getSource().sendFeedback(
                                Text.literal("Sound queue is empty")
                                .setStyle(Style.EMPTY.withColor(Formatting.GOLD))
                            );
                        } else {
                            context.getSource().sendFeedback(
                                Text.literal("Queued sounds:")
                                .setStyle(Style.EMPTY.withColor(Formatting.GOLD))
                            );
                            for (int i = 0; i < queuedSounds.size(); i++) {
                                context.getSource().sendFeedback(
                                    Text.literal(" • " + queuedSounds.get(i))
                                    .setStyle(Style.EMPTY.withColor(Formatting.GOLD))
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
                            .setStyle(Style.EMPTY.withColor(Formatting.GOLD))
                        );
                        return 1;
                    })
                )
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
                                SettingsManager.armorDurabilityEnabled = true;  // Enable for PVP mode
                                context.getSource().sendFeedback(Text.literal("PVP mode activated: Only HUD and health alerts enabled.").setStyle(Style.EMPTY.withColor(Formatting.GOLD)));
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
                                context.getSource().sendFeedback(Text.literal("PVP mode deactivated: All features re-enabled.").setStyle(Style.EMPTY.withColor(Formatting.GOLD)));
                            }
                            SettingsManager.saveSettings();
                        }
                        return 1;
                    })
                )
                .then(ClientCommandManager.literal("queuesound")
                    .then(ClientCommandManager.argument("sound", StringArgumentType.string())
                        .executes(context -> {
                            String soundName = StringArgumentType.getString(context, "sound");
                            if (SoundManager.SOUND_EVENTS.containsKey(soundName)) {
                                SoundManager.queueSound(soundName);
                                context.getSource().sendFeedback(Text.literal("Queuing sound: " + soundName).setStyle(Style.EMPTY.withColor(Formatting.GOLD)));
                            } else {
                                context.getSource().sendFeedback(Text.literal("Sound not found: " + soundName).setStyle(Style.EMPTY.withColor(Formatting.GOLD)));
                            }
                            return 1;
                        })
                    )
                )
            );
        });
    }
}
