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
            sendMessage(sender, "Usage: /hev <toggle/clearqueue/listqueue/pvp/queuesound> [options]");
            return;
        }

        String subCommand = args[0];
        switch (subCommand) {
            case "toggle":
                handleToggle(sender, Arrays.copyOfRange(args, 1, args.length));
                break;
            case "clearqueue":
                SoundManager.clearSoundQueue();
                sendMessage(sender, "Queue cleared.");
                break;
            case "listqueue":
                handleListQueue(sender);
                break;
            case "pvp":
                handlePvPMode(sender);
                break;
            case "queuesound":
                if (args.length > 1) {
                    String soundName = args[1];
                    if (SoundManager.SOUND_EVENTS.containsKey(soundName)) {
                        SoundManager.queueSound(soundName);
                        sendMessage(sender, "Queuing sound: " + soundName);
                    } else {
                        sendMessage(sender, "Sound not found: " + soundName);
                    }
                }
                break;
            default:
                sendMessage(sender, "Unknown subcommand: " + subCommand);
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