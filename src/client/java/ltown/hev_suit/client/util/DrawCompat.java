package ltown.hev_suit.client.util;

import net.minecraft.util.Identifier;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Function;

/**
 * Cross-version DrawContext helpers to avoid compile-time dependencies on
 * classes added in newer 1.21.x versions (e.g., RenderPipelines).
 */
public final class DrawCompat {
    private DrawCompat() {}

    /**
     * Attempts, in order, to invoke one of the available DrawContext#drawTexture overloads:
     * - drawTexture(RenderPipelines, Identifier, x, y, u, v, w, h, texW, texH, color)
     * - drawTexture(Identifier, x, y, u, v, w, h, texW, texH, color)
     * - drawTexture(Identifier, x, y, u, v, w, h, texW, texH)
     * If no overload is found, this call is a no-op.
     */
    public static void drawTexture(Object g, Identifier id,
                                   int x, int y, float u, float v,
                                   int w, int h, int texW, int texH, int color) {
        if (g == null) return;
        ClassLoader cl = g.getClass().getClassLoader();
        // Prefer RenderPipeline param (1.21.5+; in 1.21.7 the param type is com.mojang.blaze3d.pipeline.RenderPipeline,
        // GUI_TEXTURED constant is in net.minecraft.client.gl.RenderPipelines)
        try {
            Class<?> paramType = Class.forName("com.mojang.blaze3d.pipeline.RenderPipeline", false, cl);
            Class<?> holder = Class.forName("net.minecraft.client.gl.RenderPipelines", false, cl);
            Field guiTextured = holder.getField("GUI_TEXTURED");
            Object pipeline = guiTextured.get(null); // instance of RenderPipeline
            Method m = g.getClass().getMethod("drawTexture", paramType, Identifier.class,
                    int.class, int.class, float.class, float.class, int.class, int.class, int.class, int.class, int.class);
            m.invoke(g, pipeline, id, x, y, u, v, w, h, texW, texH, color);
            return;
        } catch (Throwable ignored) {
            // Fall through
        }

        // Fallback: Function<Identifier, RenderLayer> + color (1.21.4)
        try {
            // Resolve getGuiTextured reflectively to avoid compile errors
            Class<?> renderLayer = Class.forName("net.minecraft.client.render.RenderLayer", false, cl);
            final Method getGuiTextured = renderLayer.getMethod("getGuiTextured", Identifier.class);
            @SuppressWarnings("unchecked")
            Function<Identifier, Object> fn = (Identifier rid) -> {
                try {
                    return getGuiTextured.invoke(null, rid);
                } catch (Throwable t) {
                    return null;
                }
            };
            Method m = g.getClass().getMethod("drawTexture", Function.class, Identifier.class,
                    int.class, int.class, float.class, float.class, int.class, int.class, int.class, int.class, int.class);
            m.invoke(g, fn, id, x, y, u, v, w, h, texW, texH, color);
            return;
        } catch (Throwable ignored) {
            // Fall through
        }

        // Fallback: Function<Identifier, RenderLayer> without color (1.21.4)
        try {
            Class<?> renderLayer = Class.forName("net.minecraft.client.render.RenderLayer", false, cl);
            final Method getGuiTextured = renderLayer.getMethod("getGuiTextured", Identifier.class);
            @SuppressWarnings("unchecked")
            Function<Identifier, Object> fn = (Identifier rid) -> {
                try {
                    return getGuiTextured.invoke(null, rid);
                } catch (Throwable t) {
                    return null;
                }
            };
            Method m = g.getClass().getMethod("drawTexture", Function.class, Identifier.class,
                    int.class, int.class, float.class, float.class, int.class, int.class, int.class, int.class);
            m.invoke(g, fn, id, x, y, u, v, w, h, texW, texH);
            return;
        } catch (Throwable ignored) {
            // Fall through
        }

        // Try color overload without pipeline
        try {
            Method m = g.getClass().getMethod("drawTexture", Identifier.class,
                    int.class, int.class, float.class, float.class, int.class, int.class, int.class, int.class, int.class);
            m.invoke(g, id, x, y, u, v, w, h, texW, texH, color);
            return;
        } catch (Throwable ignored) {
            // Fall through
        }

        // Try with z (int) before UVs, colorless
        try {
            // Apply shader color as a tint when using colorless overloads
            try {
                Class<?> rs = Class.forName("com.mojang.blaze3d.systems.RenderSystem", false, cl);
                Method setShaderColor = rs.getMethod("setShaderColor", float.class, float.class, float.class, float.class);
                float a = ((color >>> 24) & 0xFF) / 255.0f;
                float r = ((color >>> 16) & 0xFF) / 255.0f;
                float gch = ((color >>> 8) & 0xFF) / 255.0f;
                float b = (color & 0xFF) / 255.0f;
                setShaderColor.invoke(null, r, gch, b, a);
            } catch (Throwable ignoredTint) {}
            Method m = g.getClass().getMethod("drawTexture", Identifier.class,
                    int.class, int.class, int.class, float.class, float.class, int.class, int.class, int.class, int.class);
            m.invoke(g, id, x, y, 0, u, v, w, h, texW, texH);
            try {
                Class<?> rs = Class.forName("com.mojang.blaze3d.systems.RenderSystem", false, cl);
                Method setShaderColor = rs.getMethod("setShaderColor", float.class, float.class, float.class, float.class);
                setShaderColor.invoke(null, 1f, 1f, 1f, 1f);
            } catch (Throwable ignoredReset) {}
            return;
        } catch (Throwable ignored) {
            // Fall through
        }

        // Try with z (int) before UVs, with color
        try {
            Method m = g.getClass().getMethod("drawTexture", Identifier.class,
                    int.class, int.class, int.class, float.class, float.class, int.class, int.class, int.class, int.class, int.class);
            m.invoke(g, id, x, y, 0, u, v, w, h, texW, texH, color);
            return;
        } catch (Throwable ignored) {
            // Fall through
        }

        // Try basic overload without color
        try {
            try {
                Class<?> rs = Class.forName("com.mojang.blaze3d.systems.RenderSystem", false, cl);
                Method setShaderColor = rs.getMethod("setShaderColor", float.class, float.class, float.class, float.class);
                float a = ((color >>> 24) & 0xFF) / 255.0f;
                float r = ((color >>> 16) & 0xFF) / 255.0f;
                float gch = ((color >>> 8) & 0xFF) / 255.0f;
                float b = (color & 0xFF) / 255.0f;
                setShaderColor.invoke(null, r, gch, b, a);
            } catch (Throwable ignoredTint2) {}
            Method m = g.getClass().getMethod("drawTexture", Identifier.class,
                    int.class, int.class, float.class, float.class, int.class, int.class, int.class, int.class);
            m.invoke(g, id, x, y, u, v, w, h, texW, texH);
            try {
                Class<?> rs = Class.forName("com.mojang.blaze3d.systems.RenderSystem", false, cl);
                Method setShaderColor = rs.getMethod("setShaderColor", float.class, float.class, float.class, float.class);
                setShaderColor.invoke(null, 1f, 1f, 1f, 1f);
            } catch (Throwable ignoredReset2) {}
            return;
        } catch (Throwable ignored) {
            // Fall through
        }

        // Try legacy int-u,v with color
        try {
            Method m = g.getClass().getMethod("drawTexture", Identifier.class,
                    int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class);
            m.invoke(g, id, x, y, (int) u, (int) v, w, h, texW, texH, color);
            return;
        } catch (Throwable ignored) {
            // Fall through
        }

        // Try legacy int-u,v without color (most common on older minors)
        try {
            Method m = g.getClass().getMethod("drawTexture", Identifier.class,
                    int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class);
            m.invoke(g, id, x, y, (int) u, (int) v, w, h, texW, texH);
            return;
        } catch (Throwable ignored) {}

        // DrawableHelper fallback for pre-DrawContext
        try {
            Class<?> dh = Class.forName("net.minecraft.client.gui.DrawableHelper", false, cl);
            // Try setting shader color to honor tint/opacity
            try {
                Class<?> rs = Class.forName("com.mojang.blaze3d.systems.RenderSystem", false, cl);
                Method setShaderColor = rs.getMethod("setShaderColor", float.class, float.class, float.class, float.class);
                float a = ((color >>> 24) & 0xFF) / 255.0f;
                float r = ((color >>> 16) & 0xFF) / 255.0f;
                float gch = ((color >>> 8) & 0xFF) / 255.0f;
                float b = (color & 0xFF) / 255.0f;
                setShaderColor.invoke(null, r, gch, b, a);
            } catch (Throwable ignoredColor) {}
            // Prefer int u/v overload
            try {
                Method m = dh.getMethod("drawTexture", g.getClass(), Identifier.class,
                        int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class);
                m.invoke(null, g, id, x, y, (int) u, (int) v, w, h, texW, texH);
                // reset shader color if we changed it
                try {
                    Class<?> rs = Class.forName("com.mojang.blaze3d.systems.RenderSystem", false, cl);
                    Method setShaderColor = rs.getMethod("setShaderColor", float.class, float.class, float.class, float.class);
                    setShaderColor.invoke(null, 1f, 1f, 1f, 1f);
                } catch (Throwable ignoredReset) {}
                return;
            } catch (Throwable ignored2) {}
            // Try float UV overload
            try {
                Method m = dh.getMethod("drawTexture", g.getClass(), Identifier.class,
                        int.class, int.class, float.class, float.class, int.class, int.class, int.class, int.class);
                m.invoke(null, g, id, x, y, u, v, w, h, texW, texH);
                try {
                    Class<?> rs = Class.forName("com.mojang.blaze3d.systems.RenderSystem", false, cl);
                    Method setShaderColor = rs.getMethod("setShaderColor", float.class, float.class, float.class, float.class);
                    setShaderColor.invoke(null, 1f, 1f, 1f, 1f);
                } catch (Throwable ignoredReset2) {}
                return;
            } catch (Throwable ignored3) {}

            // Last resort: bind texture then call DrawableHelper.drawTexture without Identifier parameter
            try {
                Class<?> rs = Class.forName("com.mojang.blaze3d.systems.RenderSystem", false, cl);
                Method setShaderTexture = rs.getMethod("setShaderTexture", int.class, Identifier.class);
                setShaderTexture.invoke(null, 0, id);
            } catch (Throwable ignoredBind) {}
            try {
                Method m = dh.getMethod("drawTexture", g.getClass(), int.class, int.class, int.class, float.class, float.class, int.class, int.class, int.class, int.class);
                m.invoke(null, g, x, y, 0, u, v, w, h, texW, texH);
                try {
                    Class<?> rs = Class.forName("com.mojang.blaze3d.systems.RenderSystem", false, cl);
                    Method setShaderColor = rs.getMethod("setShaderColor", float.class, float.class, float.class, float.class);
                    setShaderColor.invoke(null, 1f, 1f, 1f, 1f);
                } catch (Throwable ignoredReset3) {}
                return;
            } catch (Throwable ignored4) {}
        } catch (Throwable ignored) {}
    }
}
