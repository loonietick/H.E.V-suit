package ltown.hev_suit.client.util;

import net.minecraft.util.Identifier;

import java.lang.reflect.Method;

public final class GfxCompat {
    private GfxCompat() {}

    public static Object matrices(Object graphicsOrStack) {
        if (graphicsOrStack == null) return null;
        try {
            Method m = graphicsOrStack.getClass().getMethod("getMatrices");
            return m.invoke(graphicsOrStack);
        } catch (Throwable ignored) {
            // Assume this is already a MatrixStack
            return graphicsOrStack;
        }
    }

    public static void fill(Object graphicsOrStack, int x1, int y1, int x2, int y2, int argb) {
        if (graphicsOrStack == null) return;
        // Try DrawContext#fill
        try {
            Method m = graphicsOrStack.getClass().getMethod("fill", int.class, int.class, int.class, int.class, int.class);
            m.invoke(graphicsOrStack, x1, y1, x2, y2, argb);
            return;
        } catch (Throwable ignored) {}

        // Fallback to DrawableHelper.fill(MatrixStack,...)
        try {
            Class<?> dh = Class.forName("net.minecraft.client.gui.DrawableHelper", false, graphicsOrStack.getClass().getClassLoader());
            Method m = dh.getMethod("fill", graphicsOrStack.getClass(), int.class, int.class, int.class, int.class, int.class);
            m.invoke(null, graphicsOrStack, x1, y1, x2, y2, argb);
        } catch (Throwable ignored) {
            // Give up silently
        }
    }

    public static void drawTexture(Object graphicsOrStack, Identifier id, int x, int y, float u, float v,
                                   int w, int h, int texW, int texH, int color) {
        // Delegate to DrawCompat which handles both DrawContext and DrawableHelper cases reflectively
        DrawCompat.drawTexture(graphicsOrStack, id, x, y, u, v, w, h, texW, texH, color);
    }
}

