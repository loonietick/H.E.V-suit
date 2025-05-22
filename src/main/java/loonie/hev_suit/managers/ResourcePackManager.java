package loonie.hev_suit.managers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.ResourcePackRepository;

public class ResourcePackManager {
    public static void registerResourcePack() {
        // Stub: instruct players to enable the HEV Suit resource pack manually
        ResourcePackRepository repo = Minecraft.getMinecraft().getResourcePackRepository();
        // You can programmatically add resource packs here if needed
        // Ensure the 'hev_resources' pack is included in 'resourcepacks/hev_resources' under assets
    }
}