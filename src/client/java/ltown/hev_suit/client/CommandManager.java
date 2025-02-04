package ltown.hev_suit.client;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CommandManager {

    private static final Logger LOGGER = LogManager.getLogger("CommandManager");

    public static void registerToggleCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("hev")
                .then(ClientCommandManager.literal("toggle")
                    .then(ClientCommandManager.literal("health")
                        .then(ClientCommandManager.literal("health_critical2")
                            .executes(context -> {
                                if (!SettingsManager.useBlackMesaSFX) {
                                    SettingsManager.healthCritical2Enabled = !SettingsManager.healthCritical2Enabled;
                                    context.getSource().sendFeedback(Text.literal("Health Critical 2 alerts " + (SettingsManager.healthCritical2Enabled ? "enabled" : "disabled")));
                                    SettingsManager.saveSettings();
                                }
                                return 1;
                            })
                        )
                        .then(ClientCommandManager.literal("seek_medical")
                            .executes(context -> {
                                if (!SettingsManager.useBlackMesaSFX) {
                                    SettingsManager.seekMedicalEnabled = !SettingsManager.seekMedicalEnabled;
                                    context.getSource().sendFeedback(Text.literal("Seek Medical alerts " + (SettingsManager.seekMedicalEnabled ? "enabled" : "disabled")));
                                    SettingsManager.saveSettings();
                                }
                                return 1;
                            })
                        )
                        .then(ClientCommandManager.literal("health_critical")
                            .executes(context -> {
                                if (!SettingsManager.useBlackMesaSFX) {
                                    SettingsManager.healthCriticalEnabled = !SettingsManager.healthCriticalEnabled;
                                    context.getSource().sendFeedback(Text.literal("Health Critical alerts " + (SettingsManager.healthCriticalEnabled ? "enabled" : "disabled")));
                                    SettingsManager.saveSettings();
                                }
                                return 1;
                            })
                        )
                        .then(ClientCommandManager.literal("near_death")
                            .executes(context -> {
                                if (!SettingsManager.useBlackMesaSFX) {
                                    SettingsManager.nearDeathEnabled = !SettingsManager.nearDeathEnabled;
                                    context.getSource().sendFeedback(Text.literal("Near Death alerts " + (SettingsManager.nearDeathEnabled ? "enabled" : "disabled")));
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
                                context.getSource().sendFeedback(Text.literal("All alerts " + (newState ? "enabled" : "disabled")));
                                SettingsManager.saveSettings();
                            }
                            return 1;
                        })
                    )
                    .then(ClientCommandManager.literal("hud")
                        .executes(context -> {
                            if (!SettingsManager.useBlackMesaSFX) {
                                SettingsManager.hudEnabled = !SettingsManager.hudEnabled;
                                context.getSource().sendFeedback(Text.literal("HUD " + (SettingsManager.hudEnabled ? "enabled" : "disabled")));
                                SettingsManager.saveSettings();
                            }
                            return 1;
                        })
                    )
                    .then(ClientCommandManager.literal("fractures")
                        .executes(context -> {
                            if (!SettingsManager.useBlackMesaSFX) {
                                SettingsManager.fracturesEnabled = !SettingsManager.fracturesEnabled;
                                context.getSource().sendFeedback(Text.literal("Fracture alerts " + (SettingsManager.fracturesEnabled ? "enabled" : "disabled")));
                                SettingsManager.saveSettings();
                            }
                            return 1;
                        })
                    )
                    .then(ClientCommandManager.literal("heatdamage")
                        .executes(context -> {
                            if (!SettingsManager.useBlackMesaSFX) {
                                SettingsManager.heatDamageEnabled = !SettingsManager.heatDamageEnabled;
                                context.getSource().sendFeedback(Text.literal("Heat damage alerts " + (SettingsManager.heatDamageEnabled ? "enabled" : "disabled")));
                                SettingsManager.saveSettings();
                            }
                            return 1;
                        })
                    )
                    .then(ClientCommandManager.literal("bloodloss")
                        .executes(context -> {
                            if (!SettingsManager.useBlackMesaSFX) {
                                SettingsManager.bloodLossEnabled = !SettingsManager.bloodLossEnabled;
                                context.getSource().sendFeedback(Text.literal("Blood loss alerts " + (SettingsManager.bloodLossEnabled ? "enabled" : "disabled")));
                                SettingsManager.saveSettings();
                            }
                            return 1;
                        })
                    )
                    .then(ClientCommandManager.literal("shockdamage")
                        .executes(context -> {
                            if (!SettingsManager.useBlackMesaSFX) {
                                SettingsManager.shockDamageEnabled = !SettingsManager.shockDamageEnabled;
                                context.getSource().sendFeedback(Text.literal("Shock damage alerts " + (SettingsManager.shockDamageEnabled ? "enabled" : "disabled")));
                                SettingsManager.saveSettings();
                            }
                            return 1;
                        })
                    )
                    .then(ClientCommandManager.literal("chemicaldamage")
                        .executes(context -> {
                            if (!SettingsManager.useBlackMesaSFX) {
                                SettingsManager.chemicalDamageEnabled = !SettingsManager.chemicalDamageEnabled;
                                context.getSource().sendFeedback(Text.literal("Chemical damage alerts " + (SettingsManager.chemicalDamageEnabled ? "enabled" : "disabled")));
                                SettingsManager.saveSettings();
                            }
                            return 1;
                        })
                    )
                    .then(ClientCommandManager.literal("morphine")
                        .executes(context -> {
                            if (!SettingsManager.useBlackMesaSFX) {
                                SettingsManager.morphineEnabled = !SettingsManager.morphineEnabled;
                                context.getSource().sendFeedback(Text.literal("Morphine alerts " + (SettingsManager.morphineEnabled ? "enabled" : "disabled")));
                                SettingsManager.saveSettings();
                            }
                            return 1;
                        })
                    )
                    .executes(context -> {
                        if (!SettingsManager.useBlackMesaSFX) {
                            SettingsManager.hevSuitEnabled = !SettingsManager.hevSuitEnabled;
                            String status = SettingsManager.hevSuitEnabled ? "Activated" : "Deactivated";
                            context.getSource().sendFeedback(Text.literal("Voice System " + status));                
                            SettingsManager.saveSettings();
                        }
                        return 1;
                    })
                )
                .then(ClientCommandManager.literal("clearqueue")
                    .executes(context -> {
                        SoundManager.clearSoundQueue();
                        context.getSource().sendFeedback(Text.literal("Queue cleared."));
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
                                context.getSource().sendFeedback(Text.literal("PVP mode activated: Only HUD and health alerts enabled."));
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
                                context.getSource().sendFeedback(Text.literal("PVP mode deactivated: All features re-enabled."));
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
                        SettingsManager.useBlackMesaSFX = !SettingsManager.useBlackMesaSFX;
                        LOGGER.debug("Toggled Black Mesa SFX: " + SettingsManager.useBlackMesaSFX);
                        // Remove the feature toggling, just switch sound sets
                        context.getSource().sendFeedback(Text.literal(SettingsManager.useBlackMesaSFX ? 
                            "Black Mesa SFX enabled" : 
                            "Half-Life 1 SFX enabled"));
                        SettingsManager.saveSettings();
                        return 1;
                    })
                )
            );
        });
    }
}
