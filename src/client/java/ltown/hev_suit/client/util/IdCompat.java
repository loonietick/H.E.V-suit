package ltown.hev_suit.client.util;

import net.minecraft.util.Identifier;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public final class IdCompat {
    private IdCompat() {}

    public static Identifier of(String namespace, String path) {
        // Preferred on 1.21.x
        try {
            Method m = Identifier.class.getMethod("of", String.class, String.class);
            return (Identifier) m.invoke(null, namespace, path);
        } catch (Throwable ignored) {}

        // Fallback for 1.20.x
        try {
            Constructor<Identifier> c = Identifier.class.getDeclaredConstructor(String.class, String.class);
            if (!c.canAccess(null)) c.setAccessible(true);
            return c.newInstance(namespace, path);
        } catch (Throwable e) {
            // As a last resort, try parsing combined string
            try {
                Method parse = Identifier.class.getMethod("of", String.class);
                return (Identifier) parse.invoke(null, namespace + ":" + path);
            } catch (Throwable ignored2) {}
        }
        throw new IllegalArgumentException("Failed to create Identifier for " + namespace + ":" + path);
    }
}

