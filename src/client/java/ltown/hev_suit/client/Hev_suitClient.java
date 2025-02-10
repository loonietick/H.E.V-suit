package ltown.hev_suit.client;

import net.fabricmc.api.ClientModInitializer;
import ltown.hev_suit.client.managers.CommandManager;
import ltown.hev_suit.client.managers.EventManager;
import ltown.hev_suit.client.managers.SoundManager;
import ltown.hev_suit.client.managers.HudManager;

public class Hev_suitClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        SoundManager.registerSounds();
        EventManager.registerEventListeners();
        CommandManager.registerToggleCommands();
        HudManager.registerHud(); 
    }
}