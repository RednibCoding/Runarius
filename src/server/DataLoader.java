import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads game data from JSON files in doc/rs-data/locations/.
 * Uses simple regex-based parsing since the JSON formats are predictable.
 */
public class DataLoader {

    /**
     * Load NPC spawn locations from locations/npcs.json.
     * Format: [{"id": N, "x": N, "y": N, "minX": N, "maxX": N, "minY": N, "maxY": N}, ...]
     */
    public static List<Npc> loadNpcs(String filePath) throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(filePath)));
        List<Npc> npcs = new ArrayList<>();

        // Match each JSON object
        Pattern objPattern = Pattern.compile("\\{[^}]+\\}");
        Matcher objMatcher = objPattern.matcher(content);

        int serverId = 0;
        while (objMatcher.find()) {
            String obj = objMatcher.group();
            int id = extractInt(obj, "id");
            int x = extractInt(obj, "\"x\"");
            int y = extractInt(obj, "\"y\"");
            int minX = extractIntOr(obj, "minX", x);
            int maxX = extractIntOr(obj, "maxX", x);
            int minY = extractIntOr(obj, "minY", y);
            int maxY = extractIntOr(obj, "maxY", y);

            Npc npc = new Npc(id, x, y, minX, maxX, minY, maxY);
            npc.setServerId(serverId++);
            npcs.add(npc);
        }

        Logger.info("Loaded " + npcs.size() + " NPC spawns from " + filePath);
        return npcs;
    }

    /**
     * Represents a game object spawn location.
     */
    public static class GameObjectData {
        public final int id;
        public final int direction;
        public final int x;
        public final int y;

        public GameObjectData(int id, int direction, int x, int y) {
            this.id = id;
            this.direction = direction;
            this.x = x;
            this.y = y;
        }
    }

    /**
     * Represents a wall object spawn location.
     */
    public static class WallObjectData {
        public final int id;
        public final int direction;
        public final int x;
        public final int y;

        public WallObjectData(int id, int direction, int x, int y) {
            this.id = id;
            this.direction = direction;
            this.x = x;
            this.y = y;
        }
    }

    /**
     * Represents a ground item spawn location.
     */
    public static class GroundItemData {
        public final int id;
        public final int x;
        public final int y;
        public final int respawn;

        public GroundItemData(int id, int x, int y, int respawn) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.respawn = respawn;
        }
    }

    /**
     * Load game object locations from locations/objects.json.
     * Format: [{"id": N, "direction": N, "x": N, "y": N}, ...]
     */
    public static List<GameObjectData> loadObjects(String filePath) throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(filePath)));
        List<GameObjectData> objects = new ArrayList<>();

        Pattern objPattern = Pattern.compile("\\{[^}]+\\}");
        Matcher objMatcher = objPattern.matcher(content);

        while (objMatcher.find()) {
            String obj = objMatcher.group();
            int id = extractInt(obj, "id");
            int direction = extractIntOr(obj, "direction", 0);
            int x = extractInt(obj, "\"x\"");
            int y = extractInt(obj, "\"y\"");
            objects.add(new GameObjectData(id, direction, x, y));
        }

        Logger.info("Loaded " + objects.size() + " game objects from " + filePath);
        return objects;
    }

    /**
     * Load wall object locations from locations/wall-objects.json.
     * Format: [{"id": N, "direction": N, "x": N, "y": N}, ...]
     */
    public static List<WallObjectData> loadWallObjects(String filePath) throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(filePath)));
        List<WallObjectData> walls = new ArrayList<>();

        Pattern objPattern = Pattern.compile("\\{[^}]+\\}");
        Matcher objMatcher = objPattern.matcher(content);

        while (objMatcher.find()) {
            String obj = objMatcher.group();
            int id = extractInt(obj, "id");
            int direction = extractIntOr(obj, "direction", 0);
            int x = extractInt(obj, "\"x\"");
            int y = extractInt(obj, "\"y\"");
            walls.add(new WallObjectData(id, direction, x, y));
        }

        Logger.info("Loaded " + walls.size() + " wall objects from " + filePath);
        return walls;
    }

    /**
     * Load ground item locations from locations/items.json.
     * Format: [{"id": N, "respawn": N, "x": N, "y": N}, ...]
     */
    public static List<GroundItemData> loadGroundItems(String filePath) throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(filePath)));
        List<GroundItemData> items = new ArrayList<>();

        Pattern objPattern = Pattern.compile("\\{[^}]+\\}");
        Matcher objMatcher = objPattern.matcher(content);

        while (objMatcher.find()) {
            String obj = objMatcher.group();
            int id = extractInt(obj, "id");
            int x = extractInt(obj, "\"x\"");
            int y = extractInt(obj, "\"y\"");
            int respawn = extractIntOr(obj, "respawn", 60000);
            items.add(new GroundItemData(id, x, y, respawn));
        }

        Logger.info("Loaded " + items.size() + " ground items from " + filePath);
        return items;
    }

    // ===== Helper methods =====

    private static int extractInt(String obj, String key) {
        Pattern p = Pattern.compile(quote(key) + "\\s*:\\s*(-?\\d+)");
        Matcher m = p.matcher(obj);
        if (m.find()) {
            return Integer.parseInt(m.group(1));
        }
        throw new RuntimeException("Key '" + key + "' not found in: " + obj);
    }

    private static int extractIntOr(String obj, String key, int defaultValue) {
        Pattern p = Pattern.compile(quote(key) + "\\s*:\\s*(-?\\d+)");
        Matcher m = p.matcher(obj);
        if (m.find()) {
            return Integer.parseInt(m.group(1));
        }
        return defaultValue;
    }

    /**
     * Quote a key for regex, handling both "key" and key formats.
     */
    private static String quote(String key) {
        if (key.startsWith("\"") && key.endsWith("\"")) {
            // Already quoted
            return Pattern.quote(key);
        }
        // Match with or without quotes
        return "\"" + Pattern.quote(key) + "\"";
    }
}
