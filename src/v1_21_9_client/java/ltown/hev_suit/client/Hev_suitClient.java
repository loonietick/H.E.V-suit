package ltown.hev_suit.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ltown.hev_suit.client.managers.CommandManager;
import ltown.hev_suit.client.managers.EventManager;
import ltown.hev_suit.client.managers.HudManager;
import ltown.hev_suit.client.managers.ResourcePackManager;
import ltown.hev_suit.client.managers.SettingsManager;
import ltown.hev_suit.client.managers.SoundManager;
import ltown.hev_suit.client.managers.DebugOffsetManager;
public class Hev_suitClient implements ClientModInitializer {

    private static final Logger LOGGER = LogManager.getLogger("Hev_suitClient");

    private static boolean resourcePackRegistered = false;

    @Override
    public void onInitializeClient() {
        LOGGER.debug("Initializing HEV Suit Client");

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!resourcePackRegistered) {
                ResourcePackManager.registerBuiltInResourcePack();
                resourcePackRegistered = true;
            }
        });

        SettingsManager.loadSettings();
        DebugOffsetManager.reload();
        SoundManager.registerSounds();
        EventManager.registerEventListeners();
        CommandManager.registerToggleCommands();
        HudManager.registerHud();
        LOGGER.debug("HEV Suit Client Initialized");
    }
}