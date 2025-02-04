package ltown.hev_suit.client;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class HudManager {

    private static final int AMBER_COLOR = 0xFFFFAE00;
    private static final int DARK_AMBER = 0xFF8B5E00;

    public static void registerHud() {
        HudRenderCallback.EVENT.register((graphics, tickDelta) -> {
            if (!SettingsManager.hudEnabled) return;

            MinecraftClient client = MinecraftClient.getInstance();
            PlayerEntity player = client.player;

            if (player == null || client.options.hudHidden) return;

            int width = client.getWindow().getScaledWidth();
            int height = client.getWindow().getScaledHeight();
            int baseY = height - 29;
            TextRenderer textRenderer = client.textRenderer;

            // Health and Armor display
            int scaledHealth = (int)((player.getHealth() / player.getMaxHealth()) * 100);
            int scaledArmor = (int)((player.getArmor() / 20.0f) * 100);

            drawNumericDisplay(graphics, textRenderer, 10, baseY, scaledHealth, "HEALTH");
            if (scaledArmor > 0) {
                drawNumericDisplay(graphics, textRenderer, 10 + 100, baseY, scaledArmor, "ARMOR");
            }

            // Ammo display
            ItemStack mainHand = player.getMainHandStack();
            if (!mainHand.isEmpty()) {
                int currentAmmo = mainHand.getCount();
                int totalAmmo = calculateTotalAmmo(player, mainHand.getItem());
                drawAmmoDisplay(graphics, textRenderer, width - 110, baseY, currentAmmo, totalAmmo);
            }
        });
    }

    private static void drawNumericDisplay(DrawContext graphics, TextRenderer textRenderer, int x, int y, int value, String label) {
        graphics.fill(x - 2, y - 2, x + 90, y + 12, 0x80000000);
        graphics.drawTextWithShadow(textRenderer, String.format("%d", value), x, y, AMBER_COLOR);
        graphics.drawTextWithShadow(textRenderer, label, x, y - 10, DARK_AMBER);
    }

    private static void drawAmmoDisplay(DrawContext graphics, TextRenderer textRenderer, int x, int y, int currentAmmo, int totalAmmo) {
        graphics.fill(x - 2, y - 2, x + 90, y + 12, 0x80000000);
        graphics.drawTextWithShadow(textRenderer, String.format("%d/%d", currentAmmo, totalAmmo), x, y, AMBER_COLOR);
        graphics.drawTextWithShadow(textRenderer, "AMMO", x, y - 10, DARK_AMBER);
    }

    private static int calculateTotalAmmo(PlayerEntity player, Item item) {
        int total = 0;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() == item) {
                total += stack.getCount();
            }
        }
        return total;
    }
}
