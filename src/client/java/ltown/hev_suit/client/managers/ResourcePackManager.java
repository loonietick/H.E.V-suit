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

            Identifier id = Identifier.of("hev_suit", PACK_ID);
            net.fabricmc.fabric.api.resource.ResourceManagerHelper.registerBuiltinResourcePack(
                id,
                modContainer,
                ResourcePackActivationType.DEFAULT_ENABLED
            );
            // Add the pack to client options if not already present
            MinecraftClient client = MinecraftClient.getInstance();
            if (!client.options.resourcePacks.contains(PACK_ID)) {
                client.options.resourcePacks.add(PACK_ID);
            }
            LOGGER.info("HEV Suit resource pack registered successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to register HEV Suit resource pack", e);
        }
    }
    

}
