package ltown.hev_suit.client.managers;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Temporary, debug-only offset loader for HUD textures.
 * Reads per-texture offsets from config/hev_suit_offsets.json if present.
 *
 * Structure:
 * {
 *   "health":   { "x": 2,  "y": -1 },
 *   "armoron":  { "x": 0,  "y":  0 },
 *   "digit_7":  { "x": -1, "y":  0 }
 * }
 *
 * Keys should match the PNG base name (no .png), e.g., "health", "digit_7".
 */
public final class DebugOffsetManager {
    private static final Logger LOGGER = LogManager.getLogger("DebugOffsetManager");
    private static final File FILE = new File("config/hev_suit_offsets.json");
    private static final Gson GSON = new Gson();

    private static final Map<String, int[]> BUILTIN_OFFSETS = createBuiltins();
    private static final Map<String, int[]> OFFSETS = new HashMap<>(); // name -> {x,y}
    private static long lastLoadTimestamp = 0L;

    private DebugOffsetManager() {}

    public static synchronized void reload() {
        OFFSETS.clear();
        if (!FILE.exists()) {
            createDefaultFile();
        }
        try (FileReader r = new FileReader(FILE)) {
            JsonObject root = GSON.fromJson(r, JsonObject.class);
            if (root == null) root = new JsonObject();
            for (Map.Entry<String, JsonElement> e : root.entrySet()) {
                String key = e.getKey();
                JsonObject obj = e.getValue().getAsJsonObject();
                int x = obj.has("x") ? obj.get("x").getAsInt() : 0;
                int y = obj.has("y") ? obj.get("y").getAsInt() : 0;
                OFFSETS.put(key, new int[]{x, y});
            }
            lastLoadTimestamp = System.currentTimeMillis();
            LOGGER.info("[HEV] Loaded " + OFFSETS.size() + " debug offsets from " + FILE.getPath());
        } catch (Exception ex) {
            LOGGER.error("[HEV] Failed reading debug offsets: " + ex);
        }
    }

    private static void ensureLoaded() {
        if (lastLoadTimestamp == 0L) reload();
    }

    public static int getOffsetX(String name) {
        ensureLoaded();
        int[] v = resolveOffsets(name);
        return v == null ? 0 : v[0];
    }

    public static int getOffsetY(String name) {
        ensureLoaded();
        int[] v = resolveOffsets(name);
        return v == null ? 0 : v[1];
    }

    // Context-aware lookups: first try name@context, then name
    public static int getOffsetX(String name, String context) {
        if (context != null && !context.isEmpty()) {
            int[] v = resolveOffsets(name + "@" + context);
            if (v != null) return v[0];
        }
        return getOffsetX(name);
    }
    public static int getOffsetY(String name, String context) {
        if (context != null && !context.isEmpty()) {
            int[] v = resolveOffsets(name + "@" + context);
            if (v != null) return v[1];
        }
        return getOffsetY(name);
    }

    // Context + role (e.g., primary/secondary). Tries name@context#role → name@context → name
    public static int getOffsetX(String name, String context, String role) {
        if (context != null && !context.isEmpty() && role != null && !role.isEmpty()) {
            int[] v = resolveOffsets(name + "@" + context + "#" + role);
            if (v != null) return v[0];
        }
        return getOffsetX(name, context);
    }
    public static int getOffsetY(String name, String context, String role) {
        if (context != null && !context.isEmpty() && role != null && !role.isEmpty()) {
            int[] v = resolveOffsets(name + "@" + context + "#" + role);
            if (v != null) return v[1];
        }
        return getOffsetY(name, context);
    }

    private static void createDefaultFile() {
        try {
            File parent = FILE.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            JsonObject root = new JsonObject();
            for (Map.Entry<String, int[]> entry : BUILTIN_OFFSETS.entrySet()) {
                JsonObject obj = new JsonObject();
                obj.addProperty("x", entry.getValue()[0]);
                obj.addProperty("y", entry.getValue()[1]);
                root.add(entry.getKey(), obj);
            }
            try (FileWriter w = new FileWriter(FILE)) {
                GSON.toJson(root, w);
            }
            LOGGER.info("[HEV] Created default debug offsets file at " + FILE.getPath());
        } catch (Exception e) {
            LOGGER.error("[HEV] Failed to create default debug offsets file: " + e);
        }
    }

    private static int[] resolveOffsets(String key) {
        int[] v = OFFSETS.get(key);
        if (v != null) return v;
        return BUILTIN_OFFSETS.get(key);
    }

    private static Map<String, int[]> createBuiltins() {
        Map<String, int[]> map = new HashMap<>();
        // base HUD icons
        put(map, "armoron", 0, 5);
        put(map, "armorsmall", 0, 0);
        put(map, "biohazard", 0, 0);
        put(map, "biohazardsm", 0, 0);
        put(map, "cold", 0, 0);
        put(map, "coldsm", 0, 0);
        put(map, "eletrical", 0, 0);
        put(map, "eletricalsm", 0, 0);
        put(map, "fire", 0, 0);
        put(map, "firesm", 0, 0);
        put(map, "health", 0, 3);
        put(map, "lightoff", 0, 0);
        put(map, "lighton", 0, 0);
        put(map, "nervegas", 0, 0);
        put(map, "nervegassm", 0, 0);
        put(map, "noarmor", 0, 5);
        put(map, "noarmorsmall", 0, 0);
        put(map, "oxygen", 0, 0);
        put(map, "oxygensm", 0, 0);
        put(map, "radiation", 0, 0);
        put(map, "radiationsm", 0, 0);
        put(map, "waste", 0, 0);
        put(map, "wastesm", 0, 0);

        // digit sprites (global defaults remain centered)
        for (int d = 0; d <= 9; d++) {
            put(map, "digit_" + d, 0, 0);
        }

        // context-specific tuning (health digits row)
        put(map, "digit_0@health_digits", -9, 0);
        put(map, "digit_1@health_digits", -10, 0);
        put(map, "digit_2@health_digits", -10, 0);
        put(map, "digit_3@health_digits", -10, -1);
        put(map, "digit_4@health_digits", -10, 0);
        put(map, "digit_5@health_digits", -10, 0);
        put(map, "digit_6@health_digits", -10, 0);
        put(map, "digit_7@health_digits", -10, 0);
        put(map, "digit_8@health_digits", -8, 0);
        put(map, "digit_9@health_digits", -10, 0);

        return Map.copyOf(map);
    }

    private static void put(Map<String, int[]> map, String key, int x, int y) {
        map.put(key, new int[]{x, y});
    }
}
