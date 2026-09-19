package ltown.hev_suit.client.integration;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import ltown.hev_suit.client.screen.HevSuitConfigScreen;

public class HevSuitModMenuPlugin implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return HevSuitConfigScreen::new;
    }
}
