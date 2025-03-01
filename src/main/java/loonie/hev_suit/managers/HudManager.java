package loonie.hev_suit.managers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;

import java.util.*;

public class HudManager {
    private static final int AMBER_COLOR = 0xFFFFAE00;
    private static final int DARK_AMBER = 0xFF8B5E00;
    private static final int RED_COLOR = 0xFFFF0000;
    private static final int DAMAGE_INDICATOR_COLOR = 0x77FF0000;
    private static final int THREAT_INDICATOR_COLOR = 0x77FFA500;
    
    // Add missing constants
    private static final float INDICATOR_DURATION = 2.0f; // 2 seconds
    private static final int INDICATOR_SIZE = 10;
    private static final int INDICATOR_MARGIN = 15;
    private static final int INDICATOR_LENGTH = 20;
    private static final int THREAT_HORIZONTAL_RADIUS = 50;
    private static final int THREAT_Y_BUFFER = 20;
    private static final int THREAT_SCAN_INTERVAL = 10; // ticks

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
            if (entity.isDead || !entity.isEntityAlive()) {
                this.isActive = false;
                return;
            }
            
            Vec3d entityPos = new Vec3d(entity.posX, entity.posY, entity.posZ);
            Vec3d directionVec = entityPos.subtract(playerPos).normalize();
            
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

    private static void scanForThreats(Minecraft mc) {
        EntityPlayer player = mc.player;
        if (player == null) return;

        Vec3d playerPos = new Vec3d(player.posX, player.posY, player.posZ);
        AxisAlignedBB detectionBox = new AxisAlignedBB(
            playerPos.x - THREAT_HORIZONTAL_RADIUS,
            playerPos.y - THREAT_Y_BUFFER,
            playerPos.z - THREAT_HORIZONTAL_RADIUS,
            playerPos.x + THREAT_HORIZONTAL_RADIUS,
            playerPos.y + THREAT_Y_BUFFER,
            playerPos.z + THREAT_HORIZONTAL_RADIUS
        );

        List<Entity> nearbyEntities = mc.world.getEntitiesWithinAABB(
            EntityMob.class,
            detectionBox,
            entity -> entity instanceof EntityMob &&
                      !entity.isDead &&
                      entity.isEntityAlive() &&
                      Math.sqrt(Math.pow(entity.posX - playerPos.x, 2) + 
                              Math.pow(entity.posZ - playerPos.z, 2)) <= THREAT_HORIZONTAL_RADIUS &&
                      Math.abs(entity.posY - playerPos.y) <= THREAT_Y_BUFFER
        );

        for (Entity entity : nearbyEntities) {
            if (!activeThreatIndicators.containsKey(entity.getEntityId())) {
                Vec3d direction = new Vec3d(
                    entity.posX - playerPos.x,
                    entity.posY - playerPos.y,
                    entity.posZ - playerPos.z
                );
                double rad = Math.toRadians(player.rotationYaw);
                double forwardX = -Math.sin(rad);
                double forwardZ = Math.cos(rad);
                double rightX = -Math.cos(rad);
                double rightZ = -Math.sin(rad);
                double localX = direction.x * rightX + direction.z * rightZ;
                double localZ = direction.x * forwardX + direction.z * forwardZ;
                activeThreatIndicators.put(entity.getEntityId(), 
                    new ThreatIndicator(entity, new Vec3d(localX, direction.y, localZ)));
            }
        }
    }

    private static void renderDamageIndicators(Minecraft mc, int width, int height) {
        int centerX = width / 2;
        int centerY = height / 2;

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ZERO
        );

        Iterator<DamageIndicator> it = activeIndicators.iterator();
        while (it.hasNext()) {
            DamageIndicator indicator = it.next();
            indicator.timeLeft -= 1f/20f;
            
            if (indicator.timeLeft <= 0) {
                it.remove();
                continue;
            }

            float alpha = Math.min(1.0f, indicator.timeLeft / (INDICATOR_DURATION * 0.5f));
            int color = (((int)(alpha * 0x77)) << 24) | (DAMAGE_INDICATOR_COLOR & 0x00FFFFFF);

            Vec3d dir = indicator.direction.normalize();
            String dominantDirection = getDominantDirection(dir);

            renderIndicatorTriangle(centerX, centerY, dominantDirection, color, 
                INDICATOR_SIZE, INDICATOR_MARGIN, INDICATOR_LENGTH);
        }

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    private static void renderIndicatorTriangle(int centerX, int centerY, String direction, 
                                              int color, int size, int margin, int length) {
        GL11.glBegin(GL11.GL_TRIANGLES);
        
        float a = (float)(color >> 24 & 255) / 255.0F;
        float r = (float)(color >> 16 & 255) / 255.0F;
        float g = (float)(color >> 8 & 255) / 255.0F;
        float b = (float)(color & 255) / 255.0F;
        GL11.glColor4f(r, g, b, a);

        switch (direction) {
            case "FRONT":
                GL11.glVertex2f(centerX, centerY - margin);
                GL11.glVertex2f(centerX - size/2f, centerY - margin - length);
                GL11.glVertex2f(centerX + size/2f, centerY - margin - length);
                break;
            case "BACK":
                GL11.glVertex2f(centerX, centerY + margin);
                GL11.glVertex2f(centerX - size/2f, centerY + margin + length);
                GL11.glVertex2f(centerX + size/2f, centerY + margin + length);
                break;
            case "LEFT":
                GL11.glVertex2f(centerX - margin, centerY);
                GL11.glVertex2f(centerX - margin - length, centerY - size/2f);
                GL11.glVertex2f(centerX - margin - length, centerY + size/2f);
                break;
            case "RIGHT":
                GL11.glVertex2f(centerX + margin, centerY);
                GL11.glVertex2f(centerX + margin + length, centerY - size/2f);
                GL11.glVertex2f(centerX + margin + length, centerY + size/2f);
                break;
        }
        
        GL11.glEnd();
    }

    // Add missing helper methods
    private static String getDominantDirection(Vec3d dir) {
        double absX = Math.abs(dir.x);
        double absZ = Math.abs(dir.z);
        
        if (absX > absZ) {
            return dir.x > 0 ? "RIGHT" : "LEFT";
        } else {
            return dir.z > 0 ? "BACK" : "FRONT";
        }
    }
    
    private static int getTransitionColor(int value, int threshold, int colorAbove, int colorBelow) {
        if (value > threshold) {
            return colorAbove;
        }
        return colorBelow;
    }
    
    private static void updateThreatIndicators(Minecraft mc) {
        if (mc.player == null) return;
        
        Vec3d playerPos = new Vec3d(mc.player.posX, mc.player.posY, mc.player.posZ);
        float playerYaw = mc.player.rotationYaw;
        
        Iterator<Map.Entry<Integer, ThreatIndicator>> it = activeThreatIndicators.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, ThreatIndicator> entry = it.next();
            ThreatIndicator indicator = entry.getValue();
            
            if (indicator.entity.isDead || !indicator.entity.isEntityAlive()) {
                it.remove();
                continue;
            }
            
            indicator.update(playerPos, playerYaw);
        }
    }
    
    private static void renderThreatIndicators(Minecraft mc, int width, int height, float partialTicks) {
        int centerX = width / 2;
        int centerY = height / 2;

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ZERO
        );

        for (ThreatIndicator indicator : activeThreatIndicators.values()) {
            if (!indicator.isActive) continue;

            String dominantDirection = getDominantDirection(indicator.currentDirection);
            renderIndicatorTriangle(centerX, centerY, dominantDirection, THREAT_INDICATOR_COLOR, 
                INDICATOR_SIZE, INDICATOR_MARGIN, INDICATOR_LENGTH);
        }

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }
    
    private static int calculateTotalAmmo(EntityPlayer player, Item currentItem) {
        int totalAmmo = 0;
        
        // Simple implementation assuming item count represents ammo
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() == currentItem) {
                totalAmmo += stack.getCount();
            }
        }
        
        return totalAmmo;
    }
    
    private static void drawAmmoDisplay(FontRenderer fr, int x, int y, int currentAmmo, int totalAmmo) {
        drawRect(x - 2, y - 2, x + 90, y + 12, 0x80000000);
        
        String displayText = String.format("%d/%d", currentAmmo, totalAmmo);
        fr.drawStringWithShadow(displayText, x, y, AMBER_COLOR);
        fr.drawStringWithShadow("AMMO", x, y - 10, DARK_AMBER);
    }

    // ...rest of existing methods...

    public static int getScaledArmorValue(EntityPlayer player) {
        int baseArmor = player.getTotalArmorValue();
        double durabilityMultiplier = calculateArmorDurabilityMultiplier(player);
        return (int) (baseArmor * durabilityMultiplier * 5);
    }

    private static double calculateArmorDurabilityMultiplier(EntityPlayer player) {
        double totalDurability = 0;
        int armorPieces = 0;

        for (ItemStack armorPiece : player.inventory.armorInventory) {
            if (armorPiece != null && !armorPiece.isEmpty()) {
                int maxDurability = armorPiece.getMaxDamage();
                if (maxDurability > 0) {
                    int currentDamage = armorPiece.getItemDamage();
                    double pieceDurability = (maxDurability - currentDamage) / (double)maxDurability;
                    totalDurability += pieceDurability;
                    armorPieces++;
                }
            }
        }

        return armorPieces > 0 ? totalDurability / armorPieces : 1.0;
    }

    public static void addDamageIndicator(Vec3d damageSource, Vec3d playerPos, float playerYaw, float playerPitch) {
        Vec3d direction = damageSource.subtract(playerPos).normalize();
        double rad = Math.toRadians(playerYaw);
        double forwardX = -Math.sin(rad);
        double forwardZ = Math.cos(rad);
        double rightX = -Math.cos(rad);
        double rightZ = -Math.sin(rad);
        double localX = direction.x * rightX + direction.z * rightZ;
        double localZ = direction.x * forwardX + direction.z * forwardZ;
        activeIndicators.add(new DamageIndicator(new Vec3d(localX, direction.y, localZ)));
    }

    // ...existing fields...

    private static boolean isEntityDead(Entity entity) {
        if (entity instanceof EntityLivingBase) {
            return !entity.isEntityAlive() || ((EntityLivingBase) entity).getHealth() <= 0;
        }
        return false;
    }

    public static void registerHud() {
        MinecraftForge.EVENT_BUS.register(new HudManager());
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;
        if (!SettingsManager.hudEnabled) return;

        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        if (player == null) return;

        ScaledResolution scaled = new ScaledResolution(mc);
        int width = scaled.getScaledWidth();
        int height = scaled.getScaledHeight();
        int baseY = height - 29;
        FontRenderer fontRenderer = mc.fontRenderer;

        // Health Display
        if (SettingsManager.hudHealthEnabled) {
            int scaledHealth = (int)((player.getHealth() / player.getMaxHealth()) * 100);
            drawNumericDisplay(fontRenderer, 10, baseY, scaledHealth, "HEALTH");
        }

        // Armor Display
        if (SettingsManager.hudArmorEnabled) {
            int baseArmor = player.getTotalArmorValue();
            if (baseArmor > 0) {
                double durabilityMultiplier = calculateArmorDurabilityMultiplier(player);
                int scaledArmor = (int)(baseArmor * durabilityMultiplier * 5);
                drawNumericDisplay(fontRenderer, 10 + 100, baseY, scaledArmor, "ARMOR");
            }
        }

        // Ammo Display
        if (SettingsManager.hudAmmoEnabled) {
            ItemStack mainHand = player.getHeldItemMainhand();
            if (mainHand != null && !mainHand.isEmpty()) {
                int currentAmmo = mainHand.getCount();
                int totalAmmo = calculateTotalAmmo(player, mainHand.getItem());
                drawAmmoDisplay(fontRenderer, width - 110, baseY, currentAmmo, totalAmmo);
            }
        }

        // Render Indicators
        if (SettingsManager.damageIndicatorsEnabled) {
            renderDamageIndicators(mc, width, height);
        }

        if (SettingsManager.threatIndicatorsEnabled) {
            renderThreatIndicators(mc, width, height, event.getPartialTicks());
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        tickCounter++;
        
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player != null && SettingsManager.threatIndicatorsEnabled) {
            threatScanTimer++;
            
            if (threatScanTimer >= THREAT_SCAN_INTERVAL) {
                scanForThreats(mc);
                updateThreatIndicators(mc);
                threatScanTimer = 0;
            }
        }
    }

    // Update drawing methods for 1.12.2
    private static void drawNumericDisplay(FontRenderer fr, int x, int y, int value, String label) {
        drawRect(x - 2, y - 2, x + 90, y + 12, 0x80000000);
        
        int displayColor = label.equals("HEALTH") ? 
            getTransitionColor(value, 85, AMBER_COLOR, RED_COLOR) :
            getTransitionColor(value, 50, AMBER_COLOR, RED_COLOR);

        fr.drawStringWithShadow(String.format("%d", value), x, y, displayColor);
        fr.drawStringWithShadow(label, x, y - 10, DARK_AMBER);
    }

    // Helper method from Minecraft's GuiScreen
    private static void drawRect(int left, int top, int right, int bottom, int color) {
        if (left < right) {
            int i = left;
            left = right;
            right = i;
        }

        if (top < bottom) {
            int j = top;
            top = bottom;
            bottom = j;
        }

        float a = (float)(color >> 24 & 255) / 255.0F;
        float r = (float)(color >> 16 & 255) / 255.0F;
        float g = (float)(color >> 8 & 255) / 255.0F;
        float b = (float)(color & 255) / 255.0F;

        // OpenGL rendering code here
    }

    // ...rest of the methods updated for 1.12.2...
}
