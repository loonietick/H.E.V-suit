package ltown.hev_suit.client.managers;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import ltown.hev_suit.client.managers.SettingsManager;

public class HudManager {

    private static final int RED_COLOR = 0xFFFF0000;

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

            // Health display
            int scaledHealth = (int)((player.getHealth() / player.getMaxHealth()) * 100);
            drawNumericDisplay(graphics, textRenderer, 10, baseY, scaledHealth, "HEALTH");

            // Armor display with durability calculation
            int baseArmor = player.getArmor();
            if (baseArmor > 0) {
                double durabilityMultiplier = calculateArmorDurabilityMultiplier(player);
                int scaledArmor = (int)(baseArmor * durabilityMultiplier * 5); // Multiply by 5 to get percentage
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
        
        int displayColor;
        if (label.equals("HEALTH")) {
            // Start transitioning to red at 85% health (17 hearts)
            displayColor = getTransitionColor(value, 85, SettingsManager.hudPrimaryColor, RED_COLOR);
        } else if (label.equals("ARMOR")) {
            // Start transitioning to red at 50% armor durability
            displayColor = getTransitionColor(value, 50, SettingsManager.hudPrimaryColor, RED_COLOR);
        } else {
            displayColor = SettingsManager.hudPrimaryColor;
        }

        graphics.drawTextWithShadow(textRenderer, String.format("%d", value), x, y, displayColor);
        graphics.drawTextWithShadow(textRenderer, label, x, y - 10, SettingsManager.hudSecondaryColor);
    }

    private static int getTransitionColor(int value, int threshold, int startColor, int endColor) {
        if (value >= threshold) return startColor;
        
        // Calculate transition progress (0.0 to 1.0)
        float progress = value / (float)threshold;
        
        // Extract color components
        int startR = (startColor >> 16) & 0xFF;
        int startG = (startColor >> 8) & 0xFF;
        int startB = startColor & 0xFF;
        
        int endR = (endColor >> 16) & 0xFF;
        int endG = (endColor >> 8) & 0xFF;
        int endB = endColor & 0xFF;
        
        // Interpolate between colors
        int r = (int)(startR + (endR - startR) * (1 - progress));
        int g = (int)(startG + (endG - startG) * (1 - progress));
        int b = (int)(startB + (endB - startB) * (1 - progress));
        
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static void drawAmmoDisplay(DrawContext graphics, TextRenderer textRenderer, int x, int y, int currentAmmo, int totalAmmo) {
        graphics.fill(x - 2, y - 2, x + 90, y + 12, 0x80000000);
        graphics.drawTextWithShadow(textRenderer, String.format("%d/%d", currentAmmo, totalAmmo), x, y, SettingsManager.hudPrimaryColor);
        graphics.drawTextWithShadow(textRenderer, "AMMO", x, y - 10, SettingsManager.hudSecondaryColor);
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

    public static int getScaledArmorValue(PlayerEntity player) {
        int baseArmor = player.getArmor();
        double durabilityMultiplier = calculateArmorDurabilityMultiplier(player);
        return (int)(baseArmor * durabilityMultiplier * 5);
    }

    private static double calculateArmorDurabilityMultiplier(PlayerEntity player) {
        double totalDurability = 0;
        int armorPieces = 0;

        for (ItemStack armorPiece : player.getArmorItems()) {
            if (!armorPiece.isEmpty()) {
                int maxDurability = armorPiece.getMaxDamage();
                if (maxDurability > 0) {
                    int currentDamage = armorPiece.getDamage();
                    double pieceDurability = (maxDurability - currentDamage) / (double)maxDurability;
                    totalDurability += pieceDurability;
                    armorPieces++;
                }
            }
        }

        return armorPieces > 0 ? totalDurability / armorPieces : 1.0;
    }
}