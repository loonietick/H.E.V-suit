package ltown.hev_suit.client.managers;

import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraft.client.MinecraftClient;

public class ResourcePackManager {
    private static final Logger LOGGER = LogManager.getLogger("ResourcePackManager");
    private static final String PACK_ID = "hev_resources"; // pack identifier string

    // Update activation type to DEFAULT_ENABLED
    public static void registerBuiltInResourcePack() {
        try {
            var modContainer = FabricLoader.getInstance()
                .getModContainer("hev_suit")
                .orElseThrow(() -> new RuntimeException("Could not find HEV Suit mod container"));

            Identifier id = ltown.hev_suit.client.util.IdCompat.of("hev_suit", PACK_ID);
            net.fabricmc.fabric.api.resource.ResourceManagerHelper.registerBuiltinResourcePack(
                id,
                modContainer,
                ResourcePackActivationType.DEFAULT_ENABLED
            );
            // Do not touch client.options here — it can be null during early init on 1.21.4.
            // DEFAULT_ENABLED will auto-activate unless user disabled it previously.
            LOGGER.info("HEV Suit builtin resource pack registered (DEFAULT_ENABLED)");
        } catch (Exception e) {
            LOGGER.error("Failed to register HEV Suit resource pack", e);
        }
    }
    

}
