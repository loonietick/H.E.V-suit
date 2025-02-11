package ltown.hev_suit.client.managers;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import java.util.ArrayList;
import java.util.List;

public class HudManager {

    private static final int AMBER_COLOR = 0xFFFFAE00;
    private static final int DARK_AMBER = 0xFF8B5E00;
    private static final int RED_COLOR = 0xFFFF0000;
    private static final int DAMAGE_INDICATOR_COLOR = 0x77FF0000;
    private static final int INDICATOR_SIZE = 80; // Increased size (width)
    private static final int INDICATOR_MARGIN = 60; // Increased margin
    private static final int INDICATOR_LENGTH = 15; // Length of the triangle
    private static final float INDICATOR_DURATION = 15.0f; // Duration in ticks

    private static class DamageIndicator {
        final Vec3d direction;
        float timeLeft;

        DamageIndicator(Vec3d direction) {
            this.direction = direction;
            this.timeLeft = INDICATOR_DURATION;
        }
    }

    private static final List<DamageIndicator> activeIndicators = new ArrayList<>();

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

            // Render damage indicators
            if (SettingsManager.damageIndicatorsEnabled) {
                renderDamageIndicators(graphics, client);
            }
        });
    }

    private static void drawNumericDisplay(DrawContext graphics, TextRenderer textRenderer, int x, int y, int value, String label) {
        graphics.fill(x - 2, y - 2, x + 90, y + 12, 0x80000000);
        
        int displayColor;
        if (label.equals("HEALTH")) {
            // Start transitioning to red at 85% health (17 hearts)
            displayColor = getTransitionColor(value, 85, AMBER_COLOR, RED_COLOR);
        } else if (label.equals("ARMOR")) {
            // Start transitioning to red at 50% armor durability
            displayColor = getTransitionColor(value, 50, AMBER_COLOR, RED_COLOR);
        } else {
            displayColor = AMBER_COLOR;
        }

        graphics.drawTextWithShadow(textRenderer, String.format("%d", value), x, y, displayColor);
        graphics.drawTextWithShadow(textRenderer, label, x, y - 10, DARK_AMBER);
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

    private static void renderDamageIndicators(DrawContext graphics, MinecraftClient client) {
        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();
        int centerX = width / 2;
        int centerY = height / 2;
        // Removed unused variable: PlayerEntity player = client.player;

        // Update and remove expired indicators
        activeIndicators.removeIf(indicator -> {
            indicator.timeLeft -= 1f/20f; // Assuming 20 TPS
            return indicator.timeLeft <= 0;
        });

        for (DamageIndicator indicator : activeIndicators) {
            float alpha = Math.min(1.0f, indicator.timeLeft / (INDICATOR_DURATION * 0.5f));
            int color = (((int)(alpha * 0x77)) << 24) | (DAMAGE_INDICATOR_COLOR & 0x00FFFFFF);

            // Calculate indicator position based on damage direction
            Vec3d dir = indicator.direction.normalize();

            // Determine dominant direction based on player's view
            String dominantDirection = getDominantDirection(dir);

            // Draw triangle based on dominant direction
            int[] xPoints = new int[3];
            int[] yPoints = new int[3];

            switch (dominantDirection) {
                case "FRONT":
                    // Inverted: apex at crosshair; base shifted upward
                    xPoints[0] = centerX;
                    yPoints[0] = centerY - INDICATOR_MARGIN; // apex at center edge
                    xPoints[1] = centerX - INDICATOR_SIZE/2;
                    yPoints[1] = centerY - INDICATOR_MARGIN - INDICATOR_LENGTH;
                    xPoints[2] = centerX + INDICATOR_SIZE/2;
                    yPoints[2] = centerY - INDICATOR_MARGIN - INDICATOR_LENGTH;
                    break;
                case "BACK":
                    xPoints[0] = centerX;
                    yPoints[0] = centerY + INDICATOR_MARGIN; // apex at center edge
                    xPoints[1] = centerX - INDICATOR_SIZE/2;
                    yPoints[1] = centerY + INDICATOR_MARGIN + INDICATOR_LENGTH;
                    xPoints[2] = centerX + INDICATOR_SIZE/2;
                    yPoints[2] = centerY + INDICATOR_MARGIN + INDICATOR_LENGTH;
                    break;
                case "LEFT":
                    xPoints[0] = centerX - INDICATOR_MARGIN;
                    yPoints[0] = centerY; // apex at center edge
                    xPoints[1] = centerX - INDICATOR_MARGIN - INDICATOR_LENGTH;
                    yPoints[1] = centerY - INDICATOR_SIZE/2;
                    xPoints[2] = centerX - INDICATOR_MARGIN - INDICATOR_LENGTH;
                    yPoints[2] = centerY + INDICATOR_SIZE/2;
                    break;
                case "RIGHT":
                    xPoints[0] = centerX + INDICATOR_MARGIN;
                    yPoints[0] = centerY; // apex at center edge
                    xPoints[1] = centerX + INDICATOR_MARGIN + INDICATOR_LENGTH;
                    yPoints[1] = centerY - INDICATOR_SIZE/2;
                    xPoints[2] = centerX + INDICATOR_MARGIN + INDICATOR_LENGTH;
                    yPoints[2] = centerY + INDICATOR_SIZE/2;
                    break;
            }

            // Draw filled triangle
            fillTriangle(graphics, xPoints, yPoints, color);
        }
    }

    private static String getDominantDirection(Vec3d localDir) {
        double angle = Math.atan2(localDir.x, localDir.z);
        if (angle > -Math.PI/4 && angle <= Math.PI/4) {
            return "FRONT";
        } else if (angle > Math.PI/4 && angle <= 3 * Math.PI/4) {
            return "RIGHT";
        } else if (angle <= -Math.PI/4 && angle > -3 * Math.PI/4) {
            return "LEFT";
        } else {
            return "BACK";
        }
    }

    private static void fillTriangle(DrawContext graphics, int[] xPoints, int[] yPoints, int color) {
        // Draw filled triangle using multiple lines
        int minY = Math.min(Math.min(yPoints[0], yPoints[1]), yPoints[2]);
        int maxY = Math.max(Math.max(yPoints[0], yPoints[1]), yPoints[2]);

        for (int y = minY; y <= maxY; y++) {
            List<Integer> intersections = new ArrayList<>();
            
            // Find intersections with all three edges
            for (int i = 0; i < 3; i++) {
                int j = (i + 1) % 3;
                int x1 = xPoints[i], y1 = yPoints[i];
                int x2 = xPoints[j], y2 = yPoints[j];
                
                if ((y1 <= y && y2 > y) || (y2 <= y && y1 > y)) {
                    intersections.add(x1 + (y - y1) * (x2 - x1) / (y2 - y1));
                }
            }

            if (intersections.size() >= 2) {
                int x1 = Math.min(intersections.get(0), intersections.get(1));
                int x2 = Math.max(intersections.get(0), intersections.get(1));
                graphics.fill(x1, y, x2, y + 1, color);
            }
        }
    }

    public static void addDamageIndicator(Vec3d damageSource, Vec3d playerPos, float playerYaw, float playerPitch) {
        Vec3d direction = damageSource.subtract(playerPos).normalize();
        double rad = Math.toRadians(playerYaw);
        // Minecraft: forward = (-sin(yaw), 0, cos(yaw))
        double forwardX = -Math.sin(rad);
        double forwardZ = Math.cos(rad);
        // Right vector: cross(forward, up) = (-cos(yaw), 0, -sin(yaw))
        double rightX = -Math.cos(rad);
        double rightZ = -Math.sin(rad);
        // Compute local coordinates via dot product.
        double localX = direction.x * rightX + direction.z * rightZ;
        double localZ = direction.x * forwardX + direction.z * forwardZ;
        activeIndicators.add(new DamageIndicator(new Vec3d(localX, direction.y, localZ)));
    }
}