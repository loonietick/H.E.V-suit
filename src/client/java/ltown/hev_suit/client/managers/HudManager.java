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
    private static final int INDICATOR_SIZE = 80;
    private static final int INDICATOR_MARGIN = 60;
    private static final int INDICATOR_LENGTH = 15;
    private static final float INDICATOR_DURATION = 15.0f;

    private static final float ANIMATION_SPEED = 0.2f;
    private static float hungerAnimation = 0;
    private static float ammoVisibility = 0;
    private static float hungerYOffset = 0;

    private static final int RIGHT_MARGIN = 10; // New constant for consistent right-side spacing
    private static final int HUD_ELEMENT_WIDTH = 90;
    private static final int LABEL_OFFSET = 12; // Space between label and value

    private static int hungerBaseOffset = 5; // New variable to adjust hunger element vertical position

    // New constants and variable for hunger bar width transition
    private static final int NORMAL_BAR_WIDTH = 60;
    private static final int FULL_BAR_WIDTH = HUD_ELEMENT_WIDTH; // full container width same as health hud (90)
    private static float currentHungerBarWidth = NORMAL_BAR_WIDTH;
    // New constants for container height
    private static final int COMPACT_BAR_HEIGHT = 6;
    private static final int FULL_BAR_HEIGHT = 12; // full container height matching health hud
    private static float currentBarHeight = FULL_BAR_HEIGHT;

    private static class DamageIndicator {
        final Vec3d direction;
        float timeLeft;

        DamageIndicator(Vec3d direction) {
            this.direction = direction;
            this.timeLeft = INDICATOR_DURATION;
        }
    }

    private static final List<DamageIndicator> activeIndicators = new ArrayList<>();

    private static float lerp(float start, float end, float delta) {
        return start + (end - start) * Math.min(1.0f, delta);
    }

    private static void updateAnimations(final PlayerEntity player, final ItemStack mainHand) {
        float targetHunger = (player.getHungerManager().getFoodLevel() / 20.0f) * 100;
        targetHunger = Math.min(targetHunger, 100);

        // Only show ammo if HUD is enabled and there's an item
        boolean showAmmoHud = SettingsManager.hudAmmoEnabled && !mainHand.isEmpty();
        ammoVisibility = lerp(ammoVisibility, showAmmoHud ? 1.0f : 0.0f, ANIMATION_SPEED);

        // Move hunger up only if ammo HUD is visible
        float targetYOffset = showAmmoHud ? -20 : 0;
        hungerYOffset = lerp(hungerYOffset, targetYOffset, ANIMATION_SPEED);

        hungerAnimation = lerp(hungerAnimation, targetHunger, ANIMATION_SPEED);
    }

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
            ItemStack mainHand = player.getMainHandStack();

            // Update all animations
            updateAnimations(player, mainHand);
            boolean ammoActive = ammoVisibility > 0.01f && !mainHand.isEmpty();
            int targetBarWidth = ammoActive ? NORMAL_BAR_WIDTH : FULL_BAR_WIDTH;
            currentHungerBarWidth = lerp(currentHungerBarWidth, targetBarWidth, ANIMATION_SPEED);
            int targetBarHeight = ammoActive ? COMPACT_BAR_HEIGHT : FULL_BAR_HEIGHT;
            currentBarHeight = lerp(currentBarHeight, targetBarHeight, ANIMATION_SPEED);
            float extraMargin = ammoActive ? 6 : 0;
            int rightAlignX = width - RIGHT_MARGIN - HUD_ELEMENT_WIDTH;

            // Draw Health and Armor (left side stays the same)
            if (SettingsManager.hudHealthEnabled) {
                int scaledHealth = (int) ((player.getHealth() / player.getMaxHealth()) * 100);
                drawNumericDisplay(graphics, textRenderer, 10, baseY, scaledHealth, "HEALTH", 1.0f);
            }

            if (SettingsManager.hudArmorEnabled && player.getArmor() > 0) {
                double durabilityMultiplier = calculateArmorDurabilityMultiplier(player);
                int scaledArmor = (int) (player.getArmor() * durabilityMultiplier * 5);
                drawNumericDisplay(graphics, textRenderer, 10 + 100, baseY, scaledArmor, "ARMOR", 1.0f);
            }

            float hungerY = baseY + hungerYOffset + (player.getArmor() > 0 ? hungerBaseOffset : 0) + extraMargin;
            int ammoY = baseY;

            if (SettingsManager.hudAmmoEnabled && ammoVisibility > 0.01f && !mainHand.isEmpty()) {
                int currentAmmo = mainHand.getCount();
                int totalAmmo = calculateTotalAmmo(player, mainHand.getItem());
                drawAmmoDisplay(graphics, textRenderer, rightAlignX, ammoY, currentAmmo, totalAmmo, ammoVisibility);
            } else {
                // Ensure hunger bar is full width if no ammo HUD is displayed
                currentHungerBarWidth = FULL_BAR_WIDTH;
                currentBarHeight = FULL_BAR_HEIGHT;
            }

            if (SettingsManager.hudHungerEnabled) {
                int scaledHunger = (int) hungerAnimation;
                drawCompactHungerDisplay(graphics, textRenderer, rightAlignX, (int)hungerY, scaledHunger, 1.0f);
            }

            if (SettingsManager.damageIndicatorsEnabled) {
                renderDamageIndicators(graphics, client);
            }
        });
    }

    private static void drawNumericDisplay(DrawContext graphics, TextRenderer textRenderer, int x, int y, int value, String label, float alpha) {
        int backgroundColor = (((int)(alpha * 0x80)) << 24) | 0x000000;
        graphics.fill(x - 2, y - 2, x + 90, y + 12, backgroundColor);
        
        int displayColor = getTransitionColor(value, 85, AMBER_COLOR, RED_COLOR);
        displayColor = applyAlpha(displayColor, alpha);
        int labelColor = applyAlpha(DARK_AMBER, alpha);

        graphics.drawTextWithShadow(textRenderer, String.format("%d", value), x, y, displayColor);
        graphics.drawTextWithShadow(textRenderer, label, x, y - 10, labelColor);
    }

    private static void drawCompactHungerDisplay(DrawContext graphics, TextRenderer textRenderer, int x, int y, int value, float alpha) {
        int barWidth = (int) currentHungerBarWidth;
        int barHeight = (int) currentBarHeight;
        int backgroundColor = (((int)(alpha * 0x80)) << 24) | 0x000000;

        // Draw background
        graphics.fill(x + HUD_ELEMENT_WIDTH - barWidth - 2, y - 2, x + HUD_ELEMENT_WIDTH + 2, y + barHeight + 2, backgroundColor);

        // Calculate filled dimensions
        float percent = value / 100.0f;
        int filledWidth = (int)(barWidth * percent);
        int filledHeight = barHeight;
        int filledY = y;
        if (barWidth == FULL_BAR_WIDTH) {
            filledWidth = (int)((barWidth - 4) * percent);
            filledHeight = barHeight - 2;
            filledY = y + 1;
        }

        // Draw progress bar
        int fillColor = applyAlpha(getTransitionColor(value, 85, AMBER_COLOR, RED_COLOR), alpha);
        graphics.fill(x + HUD_ELEMENT_WIDTH - barWidth, filledY,
                      x + HUD_ELEMENT_WIDTH - barWidth + filledWidth,
                      filledY + filledHeight, fillColor);

        // Draw centered text for both full and small modes
        String text = "HUNGER";
        int textWidth = textRenderer.getWidth(text);
        int centerX;
        if (barWidth == FULL_BAR_WIDTH) {
            centerX = x + (HUD_ELEMENT_WIDTH / 2) - (textWidth / 2);
        } else {
            // Center text within the small bar background
            centerX = x + HUD_ELEMENT_WIDTH - (barWidth / 2) - (textWidth / 2);
        }
        graphics.drawTextWithShadow(textRenderer, text, centerX, y - 10, applyAlpha(DARK_AMBER, alpha));
    }

    private static int getTransitionColor(int value, int threshold, int startColor, int endColor) {
        if (value >= threshold) return startColor;
        
        float progress = value / (float)threshold;
        
        int startR = (startColor >> 16) & 0xFF;
        int startG = (startColor >> 8) & 0xFF;
        int startB = startColor & 0xFF;
        
        int endR = (endColor >> 16) & 0xFF;
        int endG = (endColor >> 8) & 0xFF;
        int endB = endColor & 0xFF;
        
        int r = (int)(startR + (endR - startR) * (1 - progress));
        int g = (int)(startG + (endG - startG) * (1 - progress));
        int b = (int)(startB + (endB - startB) * (1 - progress));
        
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static void drawAmmoDisplay(DrawContext graphics, TextRenderer textRenderer, int x, int y, int currentAmmo, int totalAmmo, float alpha) {
        int backgroundColor = (((int)(alpha * 0x80)) << 24) | 0x000000;
        graphics.fill(x - 2, y - 2, x + HUD_ELEMENT_WIDTH + 2, y + 12, backgroundColor);
        
        int textColor = applyAlpha(AMBER_COLOR, alpha);
        int labelColor = applyAlpha(DARK_AMBER, alpha);
        
        // Draw label with consistent positioning
        graphics.drawTextWithShadow(textRenderer, "AMMO", x, y - LABEL_OFFSET, labelColor);
        
        // Draw ammo count with smaller scale
        String ammoText = String.format("%d/%d", currentAmmo, totalAmmo);
        graphics.drawTextWithShadow(textRenderer, ammoText, x, y, textColor);
    }

    private static int applyAlpha(int color, float alpha) {
        int a = ((int)(alpha * 255)) & 0xFF;
        return (a << 24) | (color & 0x00FFFFFF);
    }

    private static int calculateTotalAmmo(final PlayerEntity player, final Item item) {
        int total = 0;
        for (int i = 0; i < player.getInventory().size(); i++) {
            final ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() == item) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static double calculateArmorDurabilityMultiplier(final PlayerEntity player) {
        double totalDurability = 0;
        int armorPieces = 0;

        for (final ItemStack armorPiece : player.getArmorItems()) {
            if (!armorPiece.isEmpty()) {
                final int maxDurability = armorPiece.getMaxDamage();
                if (maxDurability > 0) {
                    final int currentDamage = armorPiece.getDamage();
                    final double pieceDurability = (maxDurability - currentDamage) / (double)maxDurability;
                    totalDurability += pieceDurability;
                    armorPieces++;
                }
            }
        }

        return armorPieces > 0 ? totalDurability / armorPieces : 1.0;
    }

    private static void renderDamageIndicators(final DrawContext graphics, final MinecraftClient client) {
        // Early exit if no active indicators are present
        if(activeIndicators.isEmpty()) return;

        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();
        int centerX = width / 2;
        int centerY = height / 2;

        activeIndicators.removeIf(indicator -> {
            indicator.timeLeft -= 1f/20f;
            return indicator.timeLeft <= 0;
        });

        for (DamageIndicator indicator : activeIndicators) {
            float alpha = Math.min(1.0f, indicator.timeLeft / (INDICATOR_DURATION * 0.5f));
            int color = (((int)(alpha * 0x77)) << 24) | (DAMAGE_INDICATOR_COLOR & 0x00FFFFFF);

            Vec3d dir = indicator.direction.normalize();
            String dominantDirection = getDominantDirection(dir);

            int[] xPoints = new int[3];
            int[] yPoints = new int[3];

            switch (dominantDirection) {
                case "FRONT":
                    xPoints[0] = centerX;
                    yPoints[0] = centerY - INDICATOR_MARGIN;
                    xPoints[1] = centerX - INDICATOR_SIZE/2;
                    yPoints[1] = centerY - INDICATOR_MARGIN - INDICATOR_LENGTH;
                    xPoints[2] = centerX + INDICATOR_SIZE/2;
                    yPoints[2] = centerY - INDICATOR_MARGIN - INDICATOR_LENGTH;
                    break;
                case "BACK":
                    xPoints[0] = centerX;
                    yPoints[0] = centerY + INDICATOR_MARGIN;
                    xPoints[1] = centerX - INDICATOR_SIZE/2;
                    yPoints[1] = centerY + INDICATOR_MARGIN + INDICATOR_LENGTH;
                    xPoints[2] = centerX + INDICATOR_SIZE/2;
                    yPoints[2] = centerY + INDICATOR_MARGIN + INDICATOR_LENGTH;
                    break;
                case "LEFT":
                    xPoints[0] = centerX - INDICATOR_MARGIN;
                    yPoints[0] = centerY;
                    xPoints[1] = centerX - INDICATOR_MARGIN - INDICATOR_LENGTH;
                    yPoints[1] = centerY - INDICATOR_SIZE/2;
                    xPoints[2] = centerX - INDICATOR_MARGIN - INDICATOR_LENGTH;
                    yPoints[2] = centerY + INDICATOR_SIZE/2;
                    break;
                case "RIGHT":
                    xPoints[0] = centerX + INDICATOR_MARGIN;
                    yPoints[0] = centerY;
                    xPoints[1] = centerX + INDICATOR_MARGIN + INDICATOR_LENGTH;
                    yPoints[1] = centerY - INDICATOR_SIZE/2;
                    xPoints[2] = centerX + INDICATOR_MARGIN + INDICATOR_LENGTH;
                    yPoints[2] = centerY + INDICATOR_SIZE/2;
                    break;
            }

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
        int minY = Math.min(Math.min(yPoints[0], yPoints[1]), yPoints[2]);
        int maxY = Math.max(Math.max(yPoints[0], yPoints[1]), yPoints[2]);

        for (int y = minY; y <= maxY; y++) {
            List<Integer> intersections = new ArrayList<>();
            
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

    public static void addDamageIndicator(final Vec3d damageSource, final Vec3d playerPos, final float playerYaw, final float playerPitch) {
        final Vec3d direction = damageSource.subtract(playerPos).normalize();
        final double rad = Math.toRadians(playerYaw);
        final double forwardX = -Math.sin(rad);
        final double forwardZ = Math.cos(rad);
        final double rightX = -Math.cos(rad);
        final double rightZ = -Math.sin(rad);
        final double localX = direction.x * rightX + direction.z * rightZ;
        final double localZ = direction.x * forwardX + direction.z * forwardZ;
        activeIndicators.add(new DamageIndicator(new Vec3d(localX, direction.y, localZ)));
    }
}