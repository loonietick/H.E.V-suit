package ltown.hev_suit.client.managers;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
// Avoid importing DrawContext directly to support 1.19.4
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
import net.minecraft.entity.EquipmentSlot;

import net.minecraft.util.Identifier;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.texture.NativeImage;
// Removed: NativeImageBackedTexture (no dynamic alpha textures)
import java.io.IOException;

public class HudManager {

    // === hud tuning (edit these variables; rebuild to apply) ===
    private static class Vec2i { int x; int y; Vec2i(){} Vec2i(int x,int y){this.x=x;this.y=y;} }
    private static class HudTuning {
        static float iconScaleMul = 1.0f;   // global icon scale multiplier
        static float digitScaleMul = 1.0f;  // global digit scale multiplier
        static int baselineOffsetY = 0;     // shift the whole row up/down
        static int digitBaselineOffsetY = 0;// shift digits vs icons
        static int ammoDividerOffsetX = 0;  // shift ammo divider left/right
        static int ammoDividerTopOffset = 0;    // shorten/extend from top (px)
        static int ammoDividerBottomOffset = 0; // shorten/extend from bottom (px)

        static final java.util.Map<String, Vec2i> iconOffsets = new java.util.HashMap<>();     // per-icon pixel offsets
        static final java.util.Map<String, Integer> digitKerning = new java.util.HashMap<>();  // per-digit kerning

        static {
            // defaults — tweak freely; keys are the png base names (e.g., "health", "armoron", "digit_1")
            iconOffsets.put("health",  new Vec2i(0, 0));
            iconOffsets.put("armoron", new Vec2i(0, 0));
            iconOffsets.put("noarmor", new Vec2i(0, 0));
            for (int d = 0; d <= 9; d++) digitKerning.put(Integer.toString(d), 0);
        }
    }

    private static String keyOf(Identifier id){
        String p = id.getPath(); // textures/gui/hud/health.png
        int slash = p.lastIndexOf('/') + 1;
        String file = p.substring(slash);
        if (file.endsWith(".png")) file = file.substring(0, file.length() - 4);
        return file;
    }
    private static int iconOffX(Identifier id){ return iconOffX(id, null, null); }
    private static int iconOffY(Identifier id){ return iconOffY(id, null, null); }
    private static int iconOffX(Identifier id, String ctx){ return iconOffX(id, ctx, null); }
    private static int iconOffY(Identifier id, String ctx){ return iconOffY(id, ctx, null); }
    private static int iconOffX(Identifier id, String ctx, String role){
        String k = keyOf(id);
        Vec2i v = HudTuning.iconOffsets.get(k);
        int base = (v==null?0:v.x);
        return base + DebugOffsetManager.getOffsetX(k, ctx, role);
    }
    private static int iconOffY(Identifier id, String ctx, String role){
        String k = keyOf(id);
        Vec2i v = HudTuning.iconOffsets.get(k);
        int base = (v==null?0:v.y);
        return base + DebugOffsetManager.getOffsetY(k, ctx, role);
    }
    private static int kernFor(char c){ Integer k = HudTuning.digitKerning.get(Character.toString(c)); return k==null?0:k; }

    // === HL1-like sprite draw helpers (SPR_DrawGeneric analog) ===
    private static class Rect { int left, top, right, bottom; Rect(int l,int t,int r,int b){left=l;top=t;right=r;bottom=b;} int width(){return right-left;} int height(){return bottom-top;} }

    /**
     * Draw a sub-rectangle of a texture at x,y, stretched to destW x destH, using the same semantics as
     * Half-Life's SPR_DrawGeneric: if destW/destH are -1, the source (sub-rect) size is used.
     * Color is ARGB (AA RR GG BB).
     */
    private static void sprDrawGeneric(Object g, Identifier texId, int x, int y, int destW, int destH, Rect prc, int color) {
        // compute texture dims & source rect
        int fullW, fullH, u, v, rw, rh;
        if (prc != null) {
            // need real texture size for sub-rect UVs
            int[] tex = sizeOf(texId);
            if (tex[0] <= 0 || tex[1] <= 0) return;
            fullW = tex[0];
            fullH = tex[1];
            u = Math.max(0, Math.min(fullW, prc.left));
            v = Math.max(0, Math.min(fullH, prc.top));
            rw = Math.max(0, Math.min(fullW - u, prc.width()));
            rh = Math.max(0, Math.min(fullH - v, prc.height()));
            if (rw == 0 || rh == 0) return;
        } else {
            // try real size; if not a pack resource (dynamic hlalpha), assume src==dest
            int[] tex = sizeOf(texId);
            if (tex[0] > 0 && tex[1] > 0) {
                fullW = tex[0]; fullH = tex[1];
                u = 0; v = 0; rw = fullW; rh = fullH;
            } else {
                fullW = (destW > 0 ? destW : 1);
                fullH = (destH > 0 ? destH : 1);
                u = 0; v = 0; rw = fullW; rh = fullH;
            }
        }

        int outW = (destW <= 0 ? rw : destW);
        int outH = (destH <= 0 ? rh : destH);

        var m = ((DrawContext) g).getMatrices();
        m.pushMatrix();
        float sx = outW / (float)rw;
        float sy = outH / (float)rh;
        m.translate((float) x, (float) y);
        m.scale(sx, sy);
        ((DrawContext) g).drawTexture(RenderPipelines.GUI_TEXTURED, texId, 0, 0, (float)u, (float)v, rw, rh, fullW, fullH, withOpacity(color, HUD_OPACITY));
        m.popMatrix();
    }

    // (unused overloads removed)

    // === HL1 HUD: asset-driven helpers ===
    private static final String MODID = "hev_suit"; // must match fabric.mod.json id

    private static Identifier tex(String name) {
        return Identifier.of(MODID, "textures/gui/hud/" + name + ".png");
    }

    // No HL1 alpha keying or post-processing; use textures as-authored

    private static Identifier ensureHlProcessed(Identifier src) { return src; }

    // (HL alpha helpers removed)

    // cache widths/heights per texture
    private static final Map<Identifier, int[]> TEX_SIZE = new HashMap<>(); // {w,h}
    private static int[] sizeOf(Identifier id) {
        int[] cached = TEX_SIZE.get(id);
        if (cached != null && cached[0] > 0 && cached[1] > 0) return cached;
        try {
            var rm = MinecraftClient.getInstance().getResourceManager();
            var opt = rm.getResource(id);
            if (opt.isEmpty()) {
                if (!id.getPath().startsWith("hlalpha/")) {
                    // Don't cache misses; resource packs may not be ready yet
                    System.err.println("[HEV] Missing HUD texture (will retry): " + id);
                }
                return new int[]{0,0};
            }
            try (var in = opt.get().getInputStream()) {
                NativeImage img = NativeImage.read(in);
                int[] sz = new int[]{img.getWidth(), img.getHeight()};
                if (sz[0] > 0 && sz[1] > 0) TEX_SIZE.put(id, sz);
                return sz;
            }
        } catch (IOException e) {
            System.err.println("[HEV] Failed to read HUD texture (will retry): " + id + " -> " + e);
            return new int[]{0,0};
        }
    }


    private static void drawImage(Object g, Identifier id, int x, int y, int color) {
        Identifier pid = ensureHlProcessed(id);
        int[] s = sizeOf(pid);
        if (s[0] <= 0 || s[1] <= 0) return;
        sprDrawGeneric(g, pid, x, y, s[0], s[1], null, color);
    }

    // (unused color lerp removed)

    // (unused non-scaled draw helpers removed)
    private static final Map<Character, Identifier> DIGITS = new HashMap<>() {{
        put('0', tex("digit_0")); put('1', tex("digit_1")); put('2', tex("digit_2")); put('3', tex("digit_3")); put('4', tex("digit_4"));
        put('5', tex("digit_5")); put('6', tex("digit_6")); put('7', tex("digit_7")); put('8', tex("digit_8")); put('9', tex("digit_9"));
    }};

    // Static list of all HUD asset basenames currently in the project.
    // Used by alignment mode to render every texture in a single centered row.
    private static final String[] ALL_HUD_ASSETS = new String[] {
        "armoron", "armorsmall",
        "biohazard", "biohazardsm",
        "cold", "coldsm",
        "eletrical", "eletricalsm",
        "fire", "firesm",
        "health",
        "lightoff", "lighton",
        "nervegas", "nervegassm",
        "noarmor", "noarmorsmall",
        "oxygen", "oxygensm",
        "radiation", "radiationsm",
        "waste", "wastesm",
        // digits listed explicitly below when building the alignment list
    };

    // icons
    private static final Identifier ICON_HEALTH    = tex("health");
    private static final Identifier ICON_ARMOR_ON  = tex("armoron");
    private static final Identifier ICON_NOARMOR   = tex("noarmor");
    private static final Identifier ICON_O2_SM     = tex("oxygensm");
    private static final Identifier ICON_FIRE_SM   = tex("firesm");

    // === HL1 sizing + tint ===
    // (unused HL1 tuning constants removed)

    // global opacity for textured HUD elements (emulates HL1's slight see-through)
    private static float HUD_OPACITY = 0.60f; // 0..1
    private static int withOpacity(int argb, float opacity) {
        int a = (argb >>> 24) & 0xFF;
        int na = Math.max(0, Math.min(255, Math.round(a * opacity)));
        return (na << 24) | (argb & 0x00FFFFFF);
    }

    // scaled draw helpers (use matrix scale so we don't need the larger drawTexture overloads)
    private static void drawImageScaled(Object g, Identifier id, int x, int y, float scale, int color) {
        Identifier pid = ensureHlProcessed(id);
        int[] s = sizeOf(pid);
        if (s[0] <= 0 || s[1] <= 0) return;
        var m = ((DrawContext) g).getMatrices();
        m.pushMatrix();
        m.scale(scale, scale);
        drawImage(g, pid, Math.round(x / scale), Math.round(y / scale), color);
        m.popMatrix();
    }
    // (unused overload removed)

    private static int drawIconBottomAlignedScaled(Object g, Identifier id, int xLeft, int baselineY, float scale, int color) {
        return drawIconBottomAlignedScaled(g, id, xLeft, baselineY, scale, color, null, null);
    }
    private static int drawIconBottomAlignedScaled(Object g, Identifier id, int xLeft, int baselineY, float scale, int color, String context) {
        return drawIconBottomAlignedScaled(g, id, xLeft, baselineY, scale, color, context, null);
    }
    private static int drawIconBottomAlignedScaled(Object g, Identifier id, int xLeft, int baselineY, float scale, int color, String context, String role) {
        Identifier pid = ensureHlProcessed(id);
        int[] s = sizeOf(pid);
        if (s[0] <= 0 || s[1] <= 0) return 0;
        int offX = Math.round(iconOffX(id, context, role) * scale);
        int offY = Math.round(iconOffY(id, context, role) * scale);
        int yTop = baselineY - Math.round(s[1] * scale) + offY;
        drawImageScaled(g, pid, xLeft + offX, yTop, scale, color);
        return Math.round(s[0] * scale);
    }
    // (unused overload removed)

    private static int measureDigitString(String text, float scale) {
        int w = 0;
        for (int i = 0; i < text.length(); i++) {
            Identifier id = DIGITS.get(text.charAt(i));
            if (id == null) continue;
            int[] sz = sizeOf(id);
            if (sz[0] <= 0) continue;
            w += Math.round((sz[0] - 1) * scale); // -1px kerning like HL1
        }
        return w;
    }

    private static int drawDigitStringScaled(Object g, String text, int xLeft, int baselineY, float scale, int color) {
        return drawDigitStringScaled(g, text, xLeft, baselineY, scale, color, null);
    }
    private static int drawDigitStringScaled(Object g, String text, int xLeft, int baselineY, float scale, int color, String context) {
        int cursor = xLeft;
        for (int i = 0; i < text.length(); i++) {
            Identifier id = DIGITS.get(text.charAt(i));
            if (id == null) continue;
            Identifier pid = ensureHlProcessed(id);
            int[] sz = sizeOf(pid);
            if (sz[0] <= 0 || sz[1] <= 0) continue;
            String role = (i == 0) ? "primary" : "secondary";
            int adv = drawIconBottomAlignedScaled(g, pid, cursor, baselineY, scale, color, context, role);
            int kern = kernFor(text.charAt(i));
            cursor += adv + Math.round(kern * scale) - Math.round(1 * scale);
        }
        return cursor - xLeft;
    }
    private static int drawDigitStringScaled(Object g, String text, int xLeft, int baselineY, float scale) {
        return drawDigitStringScaled(g, text, xLeft, baselineY, scale, SettingsManager.hudPrimaryColor);
    }
    // These colors will remain fixed
    private static final int DAMAGE_INDICATOR_COLOR = 0x77FF0000;
    private static final int THREAT_INDICATOR_COLOR = 0x77FFA500; 
    
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
            // removed unused variables: baseY, textRenderer

            // === UI scale derived from scaled window size (respects Minecraft GUI Scale) ===
            int[] d0sz = sizeOf(DIGITS.get('0'));
            int baseDigitH = d0sz[1] > 0 ? d0sz[1] : 24;
            int[] iconHsz = sizeOf(ICON_HEALTH);
            int baseIconH = iconHsz[1] > 0 ? iconHsz[1] : 32;

            // target pixel heights (clamped) – keeps HUD readable but never huge
            int targetDigitH = Math.max(10, Math.min(18, Math.round(height * 0.030f))); // ~6% of scaled height, capped
            float uiDigitScale = ((float) targetDigitH / (float) baseDigitH) * HudTuning.digitScaleMul;
            int targetIconH  = Math.round(targetDigitH * 1.35f);
            float uiIconScale = ((float) targetIconH / (float) baseIconH) * HudTuning.iconScaleMul;

            int uiPAD       = Math.max(6, Math.round(targetDigitH * 0.70f)); // screen padding
            int uiGAP       = Math.max(2, Math.round(targetDigitH * 0.25f)); // icon↔digits gap
            int uiCOL_GAP   = Math.max(6, Math.round(targetDigitH * 0.90f)); // between health and suit clusters
            int uiBaseline  = height - uiPAD + Math.round(HudTuning.baselineOffsetY * uiDigitScale);

            // === Centered row: optionally render ALL assets for alignment, otherwise normal HUD ===
            final int PRIMARY = SettingsManager.hudPrimaryColor;
            final int RED     = 0xFFFF0000;

            // Compute strings and state used in measurement/draw
            float raw = player.getHealth() + player.getAbsorptionAmount();
            float maxH = Math.max(1f, player.getMaxHealth());
            int hp = Math.round(MathHelper.clamp((raw / maxH) * 100f, 0f, 100f));
            int armorPct = Math.max(0, getScaledArmorValue(player));
            ItemStack mainHand = player.getMainHandStack();
            int loaded = mainHand.isEmpty() ? 0 : mainHand.getCount();
            int reserve = mainHand.isEmpty() ? 0 : calculateTotalAmmo(player, mainHand.getItem());
            String lStr = Integer.toString(Math.max(0, loaded));
            String rStr = Integer.toString(Math.max(0, reserve));

            boolean showO2   = player.isSubmergedInWater() || player.getAir() < player.getMaxAir();
            boolean showFire = player.isOnFire();

            int smallGap = Math.max(4, Math.round(targetDigitH * 0.35f));

            // Baselines for both modes
            int baselineCenter = (height / 2) + Math.round(HudTuning.baselineOffsetY * uiDigitScale);
            int digitsBaseCenter = baselineCenter - Math.round(2 * uiDigitScale) + Math.round(HudTuning.digitBaselineOffsetY * uiDigitScale);

            // Alignment mode: show every asset in a single centered line for offset tuning
            if (SettingsManager.hudAlignmentMode) {
                // Build an ordered list of assets with their intended scales
                class AS { Identifier id; float sc; AS(String n, float s){ id=tex(n); sc=s; } AS(Identifier i, float s){ id=i; sc=s; } }
                java.util.ArrayList<AS> items = new java.util.ArrayList<>();
                // Add every asset in assets/hev_suit/textures/gui/hud
                for (String name : ALL_HUD_ASSETS) {
                    // scale by family
                    float sc = (name.startsWith("digit_")) ? uiDigitScale
                              : (name.endsWith("sm") || name.endsWith("small")) ? (uiIconScale * 0.75f)
                              : uiIconScale;
                    items.add(new AS(name, sc));
                }
                // Add digits 0..9 explicitly
                for (char c='0'; c<='9'; c++) items.add(new AS(DIGITS.get(c), uiDigitScale));

                // Compute total width with uniform gaps
                int gap = Math.max(4, Math.round(targetDigitH * 0.35f));
                int total = 0, count = 0;
                for (AS a : items) {
                    int[] sz = sizeOf(a.id);
                    if (sz[0] <= 0) continue;
                    if (count > 0) total += gap;
                    total += Math.round(sz[0] * a.sc);
                    count++;
                }
                int start = (width - total) / 2;
                int baselineAlign = baselineCenter;
                int x = start;
                for (AS a : items) {
                    int[] sz = sizeOf(a.id);
                    if (sz[0] <= 0 || sz[1] <= 0) continue;
                    if (x != start) x += gap;
                    x += drawIconBottomAlignedScaled(graphics, a.id, x, baselineAlign, a.sc, PRIMARY, null);
                }
                // Skip normal HUD when in alignment mode
                return;
            }

            // === Normal HUD layout (left and right anchored) ===
            // Small status pips (above left row)
            if (showO2 || showFire) {
                if (showO2) {
                    drawIconBottomAlignedScaled(graphics, ICON_O2_SM, uiPAD, uiBaseline - Math.round(targetIconH + targetDigitH * 0.9f), uiIconScale * 0.75f, PRIMARY, "status_o2_icon");
                }
                if (showFire) {
                    drawIconBottomAlignedScaled(graphics, ICON_FIRE_SM, uiPAD + Math.round(targetDigitH * 1.2f), uiBaseline - Math.round(targetIconH + targetDigitH * 0.9f), uiIconScale * 0.75f, PRIMARY, "status_fire_icon");
                }
            }

            // Left row: health and armor
            int xLeft = uiPAD;
            if (SettingsManager.hudHealthEnabled) {
                int healthColor = (hp < 20) ? RED : PRIMARY;
                xLeft += drawIconBottomAlignedScaled(graphics, ICON_HEALTH, xLeft, uiBaseline, uiIconScale, healthColor, "health_icon") + uiGAP;
                int healthDigitsBase = uiBaseline - Math.round(2 * uiDigitScale) + Math.round(HudTuning.digitBaselineOffsetY * uiDigitScale);
                xLeft += drawDigitStringScaled(graphics, Integer.toString(hp), xLeft, healthDigitsBase, uiDigitScale, healthColor, "health_digits");
            }
            if (SettingsManager.hudArmorEnabled) {
                int armorDigitsBase = uiBaseline - Math.round(2 * uiDigitScale) + Math.round(HudTuning.digitBaselineOffsetY * uiDigitScale);
                int armorPctClamped = Math.max(0, armorPct);
                if (SettingsManager.hudHealthEnabled) {
                    int barX1 = xLeft + Math.max(4, Math.round(targetDigitH * 0.25f));
                    int barX2 = barX1 + 1;
                    int barTop = uiBaseline - Math.round(16 * uiDigitScale);
                    int barBot = uiBaseline - Math.round(6 * uiDigitScale);
                    ((DrawContext) graphics).fill(barX1, barTop, barX2, barBot, withOpacity(PRIMARY, HUD_OPACITY));
                    xLeft = barX2 + uiCOL_GAP;
                }
                Identifier armorIcon = (armorPctClamped > 0 ? ICON_ARMOR_ON : ICON_NOARMOR);
                xLeft += drawIconBottomAlignedScaled(graphics, armorIcon, xLeft, uiBaseline, uiIconScale, PRIMARY, "armor_icon") + uiGAP;
                drawDigitStringScaled(graphics, Integer.toString(armorPctClamped), xLeft, armorDigitsBase, uiDigitScale, PRIMARY, "armor_digits");
            }

            // Right: ammo (reserve | divider | loaded)
            if (SettingsManager.hudAmmoEnabled) {
                int rWidth = measureDigitString(rStr, uiDigitScale);
                int dividerGap = Math.max(6, Math.round(targetDigitH * 0.45f));
                int startR = width - uiPAD - rWidth;
                int digitsBase = uiBaseline - Math.round(2 * uiDigitScale) + Math.round(HudTuning.digitBaselineOffsetY * uiDigitScale);
                drawDigitStringScaled(graphics, rStr, startR, digitsBase, uiDigitScale, PRIMARY, "ammo_right_digits");

                int barX1 = startR - dividerGap + Math.round(HudTuning.ammoDividerOffsetX * uiDigitScale);
                int barX2 = barX1 + 1;
                int barTop = uiBaseline - Math.round(16 * uiDigitScale) + Math.round(HudTuning.ammoDividerTopOffset * uiDigitScale);
                int barBot = uiBaseline - Math.round(6 * uiDigitScale) + Math.round(HudTuning.ammoDividerBottomOffset * uiDigitScale);
                ((DrawContext) graphics).fill(barX1, barTop, barX2, barBot, withOpacity(PRIMARY, HUD_OPACITY));

                int lWidth = measureDigitString(lStr, uiDigitScale);
                int startL = barX1 - Math.max(6, Math.round(targetDigitH * 0.35f)) - lWidth;
                drawDigitStringScaled(graphics, lStr, startL, digitsBase, uiDigitScale, PRIMARY, "ammo_left_digits");
            }

            // Render damage indicators
            if (SettingsManager.damageIndicatorsEnabled) {
                renderDamageIndicators(graphics, client);
            }

            // Render threat indicators
            if (SettingsManager.threatIndicatorsEnabled) {
                renderThreatIndicators(graphics, MinecraftClient.getInstance());
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
            // removed unused armor flash state
        });
    }

    public static int getScaledArmorValue(PlayerEntity player) {
        int baseArmor = player.getArmor();
        double durabilityMultiplier = calculateArmorDurabilityMultiplier(player);
        return (int) (baseArmor * durabilityMultiplier * 5);
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


    private static double calculateArmorDurabilityMultiplier(PlayerEntity player) {
        double totalDurability = 0;
        int armorPieces = 0;
        EquipmentSlot[] armorSlots = new EquipmentSlot[] {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        };
        for (EquipmentSlot armorSlot : armorSlots) {
            net.minecraft.item.ItemStack armorPiece = player.getEquippedStack(armorSlot);
            if (!armorPiece.isEmpty()) {
                // Exclude elytras from durability calculation
                if (armorSlot == EquipmentSlot.CHEST && armorPiece.getItem().getTranslationKey().toLowerCase().contains("elytra")) {
                    continue;
                }
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

    private static void renderDamageIndicators(Object graphics, MinecraftClient client) {
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

    private static void renderThreatIndicators(Object graphics, MinecraftClient client) {
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

    private static void fillTriangle(Object graphics, int[] xPoints, int[] yPoints, int color) {
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
                ((DrawContext) graphics).fill(x1, y, x2, y + 1, color);
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
