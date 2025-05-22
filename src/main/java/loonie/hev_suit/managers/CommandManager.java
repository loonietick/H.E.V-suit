package loonie.hev_suit.managers;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandManager extends CommandBase {
    private static final Logger LOGGER = LogManager.getLogger("CommandManager");

    @Override
    public String getName() {
        return "hev";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "commands.hev.usage";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 1) {
            sendMessage(sender, "Usage: /hev <toggle/voice/systems/clearqueue/listqueue/queuesound> [options]");
            return;
        }
        String cmd = args[0];
        switch (cmd) {
            case "toggle":
                if (args.length < 3) {
                    sendMessage(sender, "Usage: /hev toggle hud <all/health/armor/ammo/damageindicators/mobindicators>");
                } else if ("hud".equals(args[1])) {
                    boolean newState;
                    switch (args[2]) {
                        case "all":
                            SettingsManager.hudEnabled = !(SettingsManager.hudEnabled && SettingsManager.hudHealthEnabled && SettingsManager.hudArmorEnabled && SettingsManager.hudAmmoEnabled && SettingsManager.damageIndicatorsEnabled && SettingsManager.threatIndicatorsEnabled);
                            SettingsManager.hudHealthEnabled = SettingsManager.hudArmorEnabled = SettingsManager.hudAmmoEnabled = SettingsManager.damageIndicatorsEnabled = SettingsManager.threatIndicatorsEnabled = SettingsManager.hudEnabled;
                            sendMessage(sender, "HUD all " + (SettingsManager.hudEnabled ? "enabled" : "disabled"));
                            break;
                        case "health":
                            SettingsManager.hudHealthEnabled = !SettingsManager.hudHealthEnabled;
                            sendMessage(sender, "HUD Health " + (SettingsManager.hudHealthEnabled ? "enabled" : "disabled"));
                            break;
                        case "armor":
                            SettingsManager.hudArmorEnabled = !SettingsManager.hudArmorEnabled;
                            sendMessage(sender, "HUD Armor " + (SettingsManager.hudArmorEnabled ? "enabled" : "disabled"));
                            break;
                        case "ammo":
                            SettingsManager.hudAmmoEnabled = !SettingsManager.hudAmmoEnabled;
                            sendMessage(sender, "HUD Ammo " + (SettingsManager.hudAmmoEnabled ? "enabled" : "disabled"));
                            break;
                        case "damageindicators":
                            SettingsManager.damageIndicatorsEnabled = !SettingsManager.damageIndicatorsEnabled;
                            sendMessage(sender, "Damage indicators " + (SettingsManager.damageIndicatorsEnabled ? "enabled" : "disabled"));
                            break;
                        case "mobindicators":
                            SettingsManager.threatIndicatorsEnabled = !SettingsManager.threatIndicatorsEnabled;
                            sendMessage(sender, "Hostile MOB indicators " + (SettingsManager.threatIndicatorsEnabled ? "enabled" : "disabled"));
                            break;
                        default:
                            sendMessage(sender, "Unknown HUD toggle: " + args[2]);
                    }
                    SettingsManager.saveSettings();
                } else {
                    sendMessage(sender, "Unknown toggle category: " + args[1]);
                }
                break;
            case "voice":
                if (args.length < 2) {
                    sendMessage(sender, "Usage: /hev voice <all/morphine/armordurability/fractures/heatdamage/bloodloss/shockdamage/chemicaldamage/vitalsignsdropping/seek_medical/health_critical/near_death>");
                } else {
                    boolean state;
                    switch (args[1]) {
                        case "all":
                            state = !SettingsManager.hevSuitEnabled;
                            SettingsManager.hevSuitEnabled = SettingsManager.morphineEnabled = SettingsManager.armorDurabilityEnabled = SettingsManager.fracturesEnabled = SettingsManager.heatDamageEnabled = SettingsManager.bloodLossEnabled = SettingsManager.shockDamageEnabled = SettingsManager.chemicalDamageEnabled = SettingsManager.healthCritical2Enabled = SettingsManager.healthCriticalEnabled = SettingsManager.seekMedicalEnabled = SettingsManager.nearDeathEnabled = state;
                            sendMessage(sender, "The HEV Suit is now: " + (state ? "Enabled" : "Disabled"));
                            break;
                        case "morphine":
                            SettingsManager.morphineEnabled = !SettingsManager.morphineEnabled;
                            sendMessage(sender, "Morphine sound effects " + (SettingsManager.morphineEnabled ? "enabled" : "disabled"));
                            break;
                        case "armordurability":
                            SettingsManager.armorDurabilityEnabled = !SettingsManager.armorDurabilityEnabled;
                            sendMessage(sender, "Armor durability sound effects " + (SettingsManager.armorDurabilityEnabled ? "enabled" : "disabled"));
                            break;
                        case "fractures":
                            SettingsManager.fracturesEnabled = !SettingsManager.fracturesEnabled;
                            sendMessage(sender, "Fracture alerts " + (SettingsManager.fracturesEnabled ? "enabled" : "disabled"));
                            break;
                        case "heatdamage":
                            SettingsManager.heatDamageEnabled = !SettingsManager.heatDamageEnabled;
                            sendMessage(sender, "Heat Damage alerts " + (SettingsManager.heatDamageEnabled ? "enabled" : "disabled"));
                            break;
                        case "bloodloss":
                            SettingsManager.bloodLossEnabled = !SettingsManager.bloodLossEnabled;
                            sendMessage(sender, "Blood Loss alerts " + (SettingsManager.bloodLossEnabled ? "enabled" : "disabled"));
                            break;
                        case "shockdamage":
                            SettingsManager.shockDamageEnabled = !SettingsManager.shockDamageEnabled;
                            sendMessage(sender, "Shock Damage alerts " + (SettingsManager.shockDamageEnabled ? "enabled" : "disabled"));
                            break;
                        case "chemicaldamage":
                            SettingsManager.chemicalDamageEnabled = !SettingsManager.chemicalDamageEnabled;
                            sendMessage(sender, "Chemical Damage alerts " + (SettingsManager.chemicalDamageEnabled ? "enabled" : "disabled"));
                            break;
                        case "vitalsignsdropping":
                            SettingsManager.healthCritical2Enabled = !SettingsManager.healthCritical2Enabled;
                            sendMessage(sender, "Vital Signs Dropping alerts " + (SettingsManager.healthCritical2Enabled ? "enabled" : "disabled"));
                            break;
                        case "seek_medical":
                            SettingsManager.seekMedicalEnabled = !SettingsManager.seekMedicalEnabled;
                            sendMessage(sender, "Seek Medical alerts " + (SettingsManager.seekMedicalEnabled ? "enabled" : "disabled"));
                            break;
                        case "health_critical":
                            SettingsManager.healthCriticalEnabled = !SettingsManager.healthCriticalEnabled;
                            sendMessage(sender, "Health Critical alerts " + (SettingsManager.healthCriticalEnabled ? "enabled" : "disabled"));
                            break;
                        case "near_death":
                            SettingsManager.nearDeathEnabled = !SettingsManager.nearDeathEnabled;
                            sendMessage(sender, "Near Death alerts " + (SettingsManager.nearDeathEnabled ? "enabled" : "disabled"));
                            break;
                        default:
                            sendMessage(sender, "Unknown voice category: " + args[1]);
                    }
                    SettingsManager.saveSettings();
                }
                break;
            case "systems":
                if (args.length < 2) {
                    sendMessage(sender, "Usage: /hev systems <pvp/blackmesasfx/captions/all>");
                } else {
                    switch (args[1]) {
                        case "pvp": handlePvPMode(sender); break;
                        case "blackmesasfx":
                            SettingsManager.useBlackMesaSFX = !SettingsManager.useBlackMesaSFX;
                            sendMessage(sender, SettingsManager.useBlackMesaSFX ? "Black Mesa SFX enabled" : "Half-Life 1 SFX enabled");
                            SettingsManager.saveSettings();
                            break;
                        case "captions":
                            SubtitleManager.toggleCaptions();
                            sendMessage(sender, "Captions " + (SubtitleManager.areCaptionsEnabled() ? "enabled" : "disabled"));
                            break;
                        case "all":
                            boolean newState = !SettingsManager.hudEnabled;
                            // toggle all features
                            SettingsManager.hudEnabled = SettingsManager.hudHealthEnabled = SettingsManager.hudArmorEnabled = SettingsManager.hudAmmoEnabled = SettingsManager.damageIndicatorsEnabled = SettingsManager.threatIndicatorsEnabled = SettingsManager.hevSuitEnabled = SettingsManager.morphineEnabled = SettingsManager.armorDurabilityEnabled = SettingsManager.fracturesEnabled = SettingsManager.heatDamageEnabled = SettingsManager.bloodLossEnabled = SettingsManager.shockDamageEnabled = SettingsManager.chemicalDamageEnabled = SettingsManager.healthCritical2Enabled = SettingsManager.seekMedicalEnabled = SettingsManager.healthCriticalEnabled = SettingsManager.nearDeathEnabled = newState;
                            sendMessage(sender, "Toggled all features " + (newState ? "enabled" : "disabled"));
                            SettingsManager.saveSettings();
                            break;
                        default:
                            sendMessage(sender, "Unknown systems command: " + args[1]);
                    }
                }
                break;
            case "clearqueue":
                SoundManager.clearSoundQueue();
                sendMessage(sender, "Queue cleared.");
                break;
            case "listqueue":
                List<String> queued = SoundManager.getQueuedSounds();
                if (queued.isEmpty()) sendMessage(sender, "Sound queue is empty");
                else {
                    sendMessage(sender, "Queued sounds:");
                    for (String s : queued) sendMessage(sender, " • " + s);
                }
                break;
            case "queuesound":
                if (args.length > 1) {
                    String sound = args[1];
                    if (SoundManager.SOUND_EVENTS.containsKey(sound)) {
                        SoundManager.queueSound(sound);
                        sendMessage(sender, "Queuing sound: " + sound);
                    } else sendMessage(sender, "Sound not found: " + sound);
                }
                break;
            default:
                sendMessage(sender, "Unknown subcommand: " + cmd);
        }
    }

    private void handleToggle(ICommandSender sender, String[] args) {
        if (args.length < 1) {
            sendMessage(sender, "Usage: /hev toggle <feature>");
            return;
        }

        String feature = args[0];
        switch (feature) {
            case "captions":
                SubtitleManager.toggleCaptions();
                sendMessage(sender, "HEV Suit captions " + 
                    (SubtitleManager.areCaptionsEnabled() ? "enabled" : "disabled"));
                break;
            case "blackmesasfx":
                SettingsManager.useBlackMesaSFX = !SettingsManager.useBlackMesaSFX;
                sendMessage(sender, SettingsManager.useBlackMesaSFX ? 
                    "Black Mesa SFX enabled" : "Half-Life 1 SFX enabled");
                SettingsManager.saveSettings();
                break;
            // Add more toggle cases here for other features
            default:
                sendMessage(sender, "Unknown feature: " + feature);
        }
    }

    private void handleListQueue(ICommandSender sender) {
        List<String> queuedSounds = SoundManager.getQueuedSounds();
        if (queuedSounds.isEmpty()) {
            sendMessage(sender, "Sound queue is empty");
        } else {
            sendMessage(sender, "Queued sounds:");
            for (String sound : queuedSounds) {
                sendMessage(sender, " • " + sound);
            }
        }
    }

    private void handlePvPMode(ICommandSender sender) {
        SettingsManager.pvpModeEnabled = !SettingsManager.pvpModeEnabled;
        if (!SettingsManager.useBlackMesaSFX) {
            if (SettingsManager.pvpModeEnabled) {
                enablePvPMode();
                sendMessage(sender, "PvP mode has been enabled, All hev suit features besides the hud, health alerts and armor durability tracking are disabled.");
            } else {
                disablePvPMode();
                sendMessage(sender, "PVP mode deactivated: All features re-enabled.");
            }
        } else {
            if (SettingsManager.pvpModeEnabled) {
                enableBlackMesaPvPMode();
                sendMessage(sender, "PvP mode has been enabled, All hev suit features besides the hud and health alerts are disabled.");
            } else {
                disableBlackMesaPvPMode();
                sendMessage(sender, "PVP mode deactivated: All features re-enabled.");
            }
        }
        SettingsManager.saveSettings();
    }

    private void enablePvPMode() {
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
    }

    private void disablePvPMode() {
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
    }

    private void enableBlackMesaPvPMode() {
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
    }

    private void disableBlackMesaPvPMode() {
        SettingsManager.fracturesEnabled = true;
        SettingsManager.bloodLossEnabled = true;
        SettingsManager.chemicalDamageEnabled = true;
        SettingsManager.morphineEnabled = true;
        SettingsManager.healthCritical2Enabled = true;
        SettingsManager.nearDeathEnabled = true;
        SettingsManager.hevSuitEnabled = true;
        SettingsManager.healthAlertsEnabled = true;
        SettingsManager.hudEnabled = true;
    }

    private void sendMessage(ICommandSender sender, String message) {
        TextComponentString text = new TextComponentString(message);
        text.getStyle().setColor(TextFormatting.GOLD);
        sender.sendMessage(text);
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, net.minecraft.util.math.BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "toggle", "clearqueue", "listqueue", "pvp", "queuesound");
        }
        if (args.length == 2 && args[0].equals("toggle")) {
            return getListOfStringsMatchingLastWord(args, "captions", "blackmesasfx", "health", "fractures", 
                "heatdamage", "bloodloss", "shockdamage", "chemicaldamage", "morphine", "armordurability", 
                "damageindicators", "hud");
        }
        return new ArrayList<>();
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return true;
    }
}