package ltown.hev_suit.client.managers;

import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ResourcePackManager {
    private static final Logger LOGGER = LogManager.getLogger("ResourcePackManager");

    public static void registerBuiltInResourcePack() {
        try {
            var modContainer = FabricLoader.getInstance()
                .getModContainer("hev_suit")
                .orElseThrow(() -> new RuntimeException("Could not find HEV Suit mod container"));

                net.fabricmc.fabric.api.resource.ResourceManagerHelper.registerBuiltinResourcePack(
                    Identifier.of("hev_suit", "hev_resources"),
                    modContainer,
                    ResourcePackActivationType.ALWAYS_ENABLED
                );
            
            LOGGER.info("HEV Suit resource pack registered successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to register HEV Suit resource pack", e);
        }
    }
}
