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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class HudManager {

    // These colors will remain fixed
    private static final int DAMAGE_INDICATOR_COLOR = 0x77FF0000;
    private static final int THREAT_INDICATOR_COLOR = 0x77FFA500; 
    private static final int RED_COLOR = 0xFFFF0000;
    
    private static final int INDICATOR_SIZE = 80; // width
    private static final int INDICATOR_MARGIN = 60; // margin
    private static final int INDICATOR_LENGTH = 15; // triangle width
    private static final float INDICATOR_DURATION = 20.0f; // duration in ticks
    
    // Threat indicator constants
    private static final int THREAT_INDICATOR_SIZE = 30; // smaller than damage indicators
    private static final int THREAT_INDICATOR_MARGIN = 20; // closer to crosshair than damage indicators
    private static final int THREAT_INDICATOR_LENGTH = 5; // smaller triangle
    private static final float THREAT_SCAN_INTERVAL = 20.0f; // 5 seconds
    private static final float THREAT_PULSE_DURATION = 40.0f; // pulse cycle length in ticks
    private static final float THREAT_HORIZONTAL_RADIUS = 15.0f;
    private static final float THREAT_Y_BUFFER = 3.0f;

    private static class DamageIndicator {
        final Vec3d direction;
        float timeLeft;

        DamageIndicator(Vec3d direction) {
            this.direction = direction;
            this.timeLeft = INDICATOR_DURATION;
        }
    }
    
    private static class ThreatIndicator {
        final Entity entity;
        Vec3d currentDirection;
        boolean isActive;

        ThreatIndicator(Entity entity, Vec3d direction) {
            this.entity = entity;
            this.currentDirection = direction;
            this.isActive = true;
        }
        
        public void update(Vec3d playerPos, float playerYaw) {
            // Check if entity is dead or removed
            if (entity.isRemoved() || isEntityDead(entity)) {
                this.isActive = false;
                return;
            }
            
            // Update the direction vector based on current positions
            Vec3d entityPos = entity.getPos();
            Vec3d directionVec = entityPos.subtract(playerPos).normalize();
            
            // Convert to local coordinates
            double rad = Math.toRadians(playerYaw);
            double forwardX = -Math.sin(rad);
            double forwardZ = Math.cos(rad);
            double rightX = -Math.cos(rad);
            double rightZ = -Math.sin(rad);
            
            double localX = directionVec.x * rightX + directionVec.z * rightZ;
            double localZ = directionVec.x * forwardX + directionVec.z * forwardZ;
            
            this.currentDirection = new Vec3d(localX, directionVec.y, localZ);
        }
    }

    private static final List<DamageIndicator> activeIndicators = new ArrayList<>();
    private static final Map<Integer, ThreatIndicator> activeThreatIndicators = new HashMap<>();
    private static float threatScanTimer = 0;
    private static float tickCounter = 0;

    // Helper method to check if an entity is dead
    private static boolean isEntityDead(Entity entity) {
        if (entity instanceof LivingEntity) {
            return ((LivingEntity) entity).getHealth() <= 0;
        }
        return false;
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

        
            if (SettingsManager.hudHealthEnabled) {
                int scaledHealth = (int)((player.getHealth() / player.getMaxHealth()) * 100);
                drawNumericDisplay(graphics, textRenderer, 10, baseY, scaledHealth, "HEALTH");
            }

      
            if (SettingsManager.hudArmorEnabled) {
                int baseArmor = player.getArmor();
                if (baseArmor > 0) {
                    double durabilityMultiplier = calculateArmorDurabilityMultiplier(player);
                    int scaledArmor = (int)(baseArmor * durabilityMultiplier * 5); // Multiply by 5 to get percentage
                    drawNumericDisplay(graphics, textRenderer, 10 + 100, baseY, scaledArmor, "ARMOR");
                }
            }

            // Ammo display
            if (SettingsManager.hudAmmoEnabled) {
                ItemStack mainHand = player.getMainHandStack();
                if (!mainHand.isEmpty()) {
                    int currentAmmo = mainHand.getCount();
                    int totalAmmo = calculateTotalAmmo(player, mainHand.getItem());
                    drawAmmoDisplay(graphics, textRenderer, width - 110, baseY, currentAmmo, totalAmmo);
                }
            }

            // Render damage indicators
            if (SettingsManager.damageIndicatorsEnabled) {
                renderDamageIndicators(graphics, client);
            }

            // Render threat indicators - fix parameter type issue
            if (SettingsManager.threatIndicatorsEnabled) {
                // Supply a boolean argument (e.g., 'true') to getTickDelta(boolean)
                renderThreatIndicators(graphics, MinecraftClient.getInstance(), tickDelta.getTickDelta(true));
            }
        });

        // Update threat scan and hud update only every 5 seconds
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            tickCounter++;
            
            if (client.player != null && SettingsManager.threatIndicatorsEnabled) {
                threatScanTimer++;
                
                if (threatScanTimer >= THREAT_SCAN_INTERVAL) {
                    scanForThreats(client);
                    updateThreatIndicators(client);
                    threatScanTimer = 0;
                }
            }
        });
    }

    public static int getScaledArmorValue(PlayerEntity player) {
        int baseArmor = player.getArmor();
        double durabilityMultiplier = calculateArmorDurabilityMultiplier(player);
        return (int) (baseArmor * durabilityMultiplier * 5);
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

    private static int calculateTotalAmmo(PlayerEntity player, Item item) {
        // Special handling for bows and crossbows - count arrows instead
        if (item instanceof net.minecraft.item.BowItem || 
            item instanceof net.minecraft.item.CrossbowItem) {
            int total = 0;
            for (int i = 0; i < player.getInventory().size(); i++) {
                ItemStack stack = player.getInventory().getStack(i);
                if (stack.getItem() instanceof net.minecraft.item.ArrowItem) {
                    total += stack.getCount();
                }
            }
            return total;
        }
        
        // Default behavior for other items
        int total = 0;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() == item) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static void drawAmmoDisplay(DrawContext graphics, TextRenderer textRenderer, int x, int y, int currentAmmo, int totalAmmo) {
        graphics.fill(x - 2, y - 2, x + 90, y + 12, 0x80000000);
        
        // For bows and crossbows, show only total arrows
        if (MinecraftClient.getInstance().player != null && 
            (MinecraftClient.getInstance().player.getMainHandStack().getItem() instanceof net.minecraft.item.BowItem ||
             MinecraftClient.getInstance().player.getMainHandStack().getItem() instanceof net.minecraft.item.CrossbowItem)) {
            graphics.drawTextWithShadow(textRenderer, String.format("%d", totalAmmo), x, y, SettingsManager.hudPrimaryColor);
        } else {
            graphics.drawTextWithShadow(textRenderer, String.format("%d/%d", currentAmmo, totalAmmo), x, y, SettingsManager.hudPrimaryColor);
        }
        
        graphics.drawTextWithShadow(textRenderer, "AMMO", x, y - 10, SettingsManager.hudSecondaryColor);
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


        activeIndicators.removeIf(indicator -> {
            indicator.timeLeft -= 1f/20f; // Assuming 20 TPS
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

   
            fillTriangle(graphics, xPoints, yPoints, color);
        }
    }

    private static void scanForThreats(MinecraftClient client) {
        PlayerEntity player = client.player;
        if (player == null) return;

        Vec3d playerPos = player.getPos();

        Box detectionBox = new Box(
            playerPos.x - THREAT_HORIZONTAL_RADIUS,
            playerPos.y - THREAT_Y_BUFFER,
            playerPos.z - THREAT_HORIZONTAL_RADIUS,
            playerPos.x + THREAT_HORIZONTAL_RADIUS,
            playerPos.y + THREAT_Y_BUFFER,
            playerPos.z + THREAT_HORIZONTAL_RADIUS
        );

        // Filter hostile entities within the new detection box
        List<Entity> nearbyEntities = client.world.getEntitiesByClass(
            Entity.class,
            detectionBox,
            entity -> (entity instanceof HostileEntity || entity instanceof Monster) &&
                      !entity.isRemoved() &&
                      entity.isAlive() &&
                      Math.sqrt(Math.pow(entity.getX() - playerPos.x, 2) + Math.pow(entity.getZ() - playerPos.z, 2)) <= THREAT_HORIZONTAL_RADIUS &&
                      Math.abs(entity.getY() - playerPos.y) <= THREAT_Y_BUFFER
        );

        for (Entity entity : nearbyEntities) {
            if (!activeThreatIndicators.containsKey(entity.getId())) {
                Vec3d direction = entity.getPos().subtract(playerPos);
                double rad = Math.toRadians(player.getYaw());
                double forwardX = -Math.sin(rad);
                double forwardZ = Math.cos(rad);
                double rightX = -Math.cos(rad);
                double rightZ = -Math.sin(rad);
                double localX = direction.x * rightX + direction.z * rightZ;
                double localZ = direction.x * forwardX + direction.z * forwardZ;
                activeThreatIndicators.put(entity.getId(), new ThreatIndicator(entity, new Vec3d(localX, direction.y, localZ)));
            }
        }
    }

    private static void updateThreatIndicators(MinecraftClient client) {
        if (client.player == null) return;
        
        Vec3d playerPos = client.player.getPos();
        float playerYaw = client.player.getYaw();
        
        Iterator<Map.Entry<Integer, ThreatIndicator>> it = activeThreatIndicators.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, ThreatIndicator> entry = it.next();
            ThreatIndicator indicator = entry.getValue();
            // Remove if the entity is outside the allowed horizontal or y-range
            double horizontalDistance = Math.sqrt(Math.pow(indicator.entity.getX() - playerPos.x, 2) + Math.pow(indicator.entity.getZ() - playerPos.z, 2));
            double yDiff = Math.abs(indicator.entity.getY() - playerPos.y);
            if (!indicator.isActive || indicator.entity.isRemoved() || isEntityDead(indicator.entity) ||
                horizontalDistance > THREAT_HORIZONTAL_RADIUS || yDiff > THREAT_Y_BUFFER) {
                it.remove();
                continue;
            }
            
            indicator.update(playerPos, playerYaw);
        }
    }

    private static void renderThreatIndicators(DrawContext graphics, MinecraftClient client, float tickDelta) {
        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();
        int centerX = width / 2;
        int centerY = height / 2;

        for (ThreatIndicator indicator : activeThreatIndicators.values()) {
            if (!indicator.isActive) continue;
            
            // Calculate pulsing alpha using sin wave
            float pulseProgress = (tickCounter % THREAT_PULSE_DURATION) / THREAT_PULSE_DURATION;
            float pulseValue = (MathHelper.sin(pulseProgress * 2 * (float)Math.PI) + 1f) / 2f; // 0 to 1 pulsing
            float alpha = 0.3f + (pulseValue * 0.6f); // Range from 0.3 to 0.9 alpha
            
            int color = (((int)(alpha * 0xFF)) << 24) | (THREAT_INDICATOR_COLOR & 0x00FFFFFF);

            // Get dominant direction
            String dominantDirection = getDominantDirection(indicator.currentDirection);

            // Draw triangle based on dominant direction
            int[] xPoints = new int[3];
            int[] yPoints = new int[3];

            switch (dominantDirection) {
                case "FRONT":
                    xPoints[0] = centerX;
                    yPoints[0] = centerY - THREAT_INDICATOR_MARGIN;
                    xPoints[1] = centerX - THREAT_INDICATOR_SIZE/2;
                    yPoints[1] = centerY - THREAT_INDICATOR_MARGIN - THREAT_INDICATOR_LENGTH;
                    xPoints[2] = centerX + THREAT_INDICATOR_SIZE/2;
                    yPoints[2] = centerY - THREAT_INDICATOR_MARGIN - THREAT_INDICATOR_LENGTH;
                    break;
                case "BACK":
                    xPoints[0] = centerX;
                    yPoints[0] = centerY + THREAT_INDICATOR_MARGIN;
                    xPoints[1] = centerX - THREAT_INDICATOR_SIZE/2;
                    yPoints[1] = centerY + THREAT_INDICATOR_MARGIN + THREAT_INDICATOR_LENGTH;
                    xPoints[2] = centerX + THREAT_INDICATOR_SIZE/2;
                    yPoints[2] = centerY + THREAT_INDICATOR_MARGIN + THREAT_INDICATOR_LENGTH;
                    break;
                case "LEFT":
                    xPoints[0] = centerX - THREAT_INDICATOR_MARGIN;
                    yPoints[0] = centerY;
                    xPoints[1] = centerX - THREAT_INDICATOR_MARGIN - THREAT_INDICATOR_LENGTH;
                    yPoints[1] = centerY - THREAT_INDICATOR_SIZE/2;
                    xPoints[2] = centerX - THREAT_INDICATOR_MARGIN - THREAT_INDICATOR_LENGTH;
                    yPoints[2] = centerY + THREAT_INDICATOR_SIZE/2;
                    break;
                case "RIGHT":
                    xPoints[0] = centerX + THREAT_INDICATOR_MARGIN;
                    yPoints[0] = centerY;
                    xPoints[1] = centerX + THREAT_INDICATOR_MARGIN + THREAT_INDICATOR_LENGTH;
                    yPoints[1] = centerY - THREAT_INDICATOR_SIZE/2;
                    xPoints[2] = centerX + THREAT_INDICATOR_MARGIN + THREAT_INDICATOR_LENGTH;
                    yPoints[2] = centerY + THREAT_INDICATOR_SIZE/2;
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