package ltown.hev_suit.client;

import net.fabricmc.api.ClientModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ltown.hev_suit.client.managers.CommandManager;
import ltown.hev_suit.client.managers.EventManager;
import ltown.hev_suit.client.managers.HudManager;
import ltown.hev_suit.client.managers.ResourcePackManager;
import ltown.hev_suit.client.managers.SettingsManager;
import ltown.hev_suit.client.managers.SoundManager;

public class Hev_suitClient implements ClientModInitializer {

    private static final Logger LOGGER = LogManager.getLogger("Hev_suitClient");

    @Override
    public void onInitializeClient() {
        LOGGER.debug("Initializing HEV Suit Client");
        ResourcePackManager.registerBuiltInResourcePack();
        SettingsManager.loadSettings();
        SoundManager.registerSounds();
        EventManager.registerEventListeners();
        CommandManager.registerToggleCommands();
        HudManager.registerHud();
    }
}