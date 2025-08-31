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
        int[] v = OFFSETS.get(name);
        return v == null ? 0 : v[0];
    }

    public static int getOffsetY(String name) {
        ensureLoaded();
        int[] v = OFFSETS.get(name);
        return v == null ? 0 : v[1];
    }

    // Context-aware lookups: first try name@context, then name
    public static int getOffsetX(String name, String context) {
        if (context != null && !context.isEmpty()) {
            int[] v = OFFSETS.get(name + "@" + context);
            if (v != null) return v[0];
        }
        return getOffsetX(name);
    }
    public static int getOffsetY(String name, String context) {
        if (context != null && !context.isEmpty()) {
            int[] v = OFFSETS.get(name + "@" + context);
            if (v != null) return v[1];
        }
        return getOffsetY(name);
    }

    // Context + role (e.g., primary/secondary). Tries name@context#role → name@context → name
    public static int getOffsetX(String name, String context, String role) {
        if (context != null && !context.isEmpty() && role != null && !role.isEmpty()) {
            int[] v = OFFSETS.get(name + "@" + context + "#" + role);
            if (v != null) return v[0];
        }
        return getOffsetX(name, context);
    }
    public static int getOffsetY(String name, String context, String role) {
        if (context != null && !context.isEmpty() && role != null && !role.isEmpty()) {
            int[] v = OFFSETS.get(name + "@" + context + "#" + role);
            if (v != null) return v[1];
        }
        return getOffsetY(name, context);
    }

    private static void createDefaultFile() {
        try {
            File parent = FILE.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            JsonObject root = new JsonObject();
            String[] assets = new String[] {
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
                "waste", "wastesm"
            };
            for (String name : assets) {
                JsonObject obj = new JsonObject();
                obj.addProperty("x", 0);
                obj.addProperty("y", 0);
                root.add(name, obj);
            }
            for (int d = 0; d <= 9; d++) {
                JsonObject obj = new JsonObject();
                obj.addProperty("x", 0);
                obj.addProperty("y", 0);
                root.add("digit_" + d, obj);
            }
            try (FileWriter w = new FileWriter(FILE)) {
                GSON.toJson(root, w);
            }
            LOGGER.info("[HEV] Created default debug offsets file at " + FILE.getPath());
        } catch (Exception e) {
            LOGGER.error("[HEV] Failed to create default debug offsets file: " + e);
        }
    }
}
