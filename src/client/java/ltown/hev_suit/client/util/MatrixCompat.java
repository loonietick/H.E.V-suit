package ltown.hev_suit.client.util;

import java.lang.reflect.Method;

/**
 * Cross-version matrix helpers bridging 1.21.4 MatrixStack and newer 1.21.x Matrix3x2fStack.
 * Uses reflection to call whichever methods exist at runtime.
 */
public final class MatrixCompat {
    private static Method PUSH_MATRIX;
    private static Method POP_MATRIX;
    private static Method PUSH;
    private static Method POP;
    private static Method TRANSLATE_XY;
    private static Method TRANSLATE_XYZ_F;
    private static Method TRANSLATE_XYZ_D;
    private static Method SCALE_XY;
    private static Method SCALE_XYZ;

    private MatrixCompat() {}

    private static void ensureResolved(Object m) {
        Class<?> cls = m.getClass();
        if (PUSH_MATRIX == null) {
            try { PUSH_MATRIX = cls.getMethod("pushMatrix"); } catch (Throwable ignored) {}
        }
        if (POP_MATRIX == null) {
            try { POP_MATRIX = cls.getMethod("popMatrix"); } catch (Throwable ignored) {}
        }
        if (PUSH == null) {
            try { PUSH = cls.getMethod("push"); } catch (Throwable ignored) {}
        }
        if (POP == null) {
            try { POP = cls.getMethod("pop"); } catch (Throwable ignored) {}
        }
        if (TRANSLATE_XY == null) {
            try { TRANSLATE_XY = cls.getMethod("translate", float.class, float.class); } catch (Throwable ignored) {}
        }
        if (TRANSLATE_XYZ_F == null) {
            try { TRANSLATE_XYZ_F = cls.getMethod("translate", float.class, float.class, float.class); } catch (Throwable ignored) {}
        }
        if (TRANSLATE_XYZ_D == null) {
            try { TRANSLATE_XYZ_D = cls.getMethod("translate", double.class, double.class, double.class); } catch (Throwable ignored) {}
        }
        if (SCALE_XY == null) {
            try { SCALE_XY = cls.getMethod("scale", float.class, float.class); } catch (Throwable ignored) {}
        }
        if (SCALE_XYZ == null) {
            try { SCALE_XYZ = cls.getMethod("scale", float.class, float.class, float.class); } catch (Throwable ignored) {}
        }
    }

    public static void push(Object m) {
        ensureResolved(m);
        try {
            if (PUSH_MATRIX != null) { PUSH_MATRIX.invoke(m); return; }
        } catch (Throwable ignored) {}
        try {
            if (PUSH != null) { PUSH.invoke(m); }
        } catch (Throwable ignored) {}
    }

    public static void pop(Object m) {
        ensureResolved(m);
        try {
            if (POP_MATRIX != null) { POP_MATRIX.invoke(m); return; }
        } catch (Throwable ignored) {}
        try {
            if (POP != null) { POP.invoke(m); }
        } catch (Throwable ignored) {}
    }

    public static void translate(Object m, float x, float y) {
        ensureResolved(m);
        try {
            if (TRANSLATE_XY != null) { TRANSLATE_XY.invoke(m, x, y); return; }
        } catch (Throwable ignored) {}
        try {
            if (TRANSLATE_XYZ_F != null) { TRANSLATE_XYZ_F.invoke(m, x, y, 0.0f); return; }
        } catch (Throwable ignored) {}
        try {
            if (TRANSLATE_XYZ_D != null) { TRANSLATE_XYZ_D.invoke(m, (double)x, (double)y, 0.0d); }
        } catch (Throwable ignored) {}
    }

    public static void scale(Object m, float sx, float sy) {
        ensureResolved(m);
        try {
            if (SCALE_XY != null) { SCALE_XY.invoke(m, sx, sy); return; }
        } catch (Throwable ignored) {}
        try {
            if (SCALE_XYZ != null) { SCALE_XYZ.invoke(m, sx, sy, 1.0f); }
        } catch (Throwable ignored) {}
    }
}
