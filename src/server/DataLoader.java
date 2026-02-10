import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads game data from JSON files in doc/rs-data/.
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

    // ===== Item & NPC Definition Loading =====

    /**
     * Load item definitions from config/items.json.
     * Format: Array of objects with name, description, command, sprite, price, stackable, etc.
     */
    public static List<ItemDefinition> loadItemDefinitions(String filePath) throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(filePath)));
        List<ItemDefinition> items = new ArrayList<>();

        // Split by each JSON object boundary - items.json has clean formatting
        // Match each top-level object: { ... }
        Pattern objPattern = Pattern.compile("\\{\\s*\"name\"[^}]+\\}", Pattern.DOTALL);
        Matcher objMatcher = objPattern.matcher(content);

        int id = 0;
        while (objMatcher.find()) {
            String obj = objMatcher.group();

            String name = extractString(obj, "name");
            String description = extractStringOrNull(obj, "description");
            String command = extractStringOrNull(obj, "command");
            int sprite = extractIntOr(obj, "sprite", 0);
            int price = extractIntOr(obj, "price", 0);
            boolean stackable = extractBoolOr(obj, "stackable", false);
            boolean special = extractBoolOr(obj, "special", false);
            boolean members = extractBoolOr(obj, "members", false);
            boolean untradeable = extractBoolOr(obj, "untradeable", false);

            // Parse equip slots: "equip": ["right-hand"] or "equip": null
            List<String> equipSlots = extractStringArray(obj, "equip");

            items.add(new ItemDefinition(id, name, description, command, sprite, price,
                                         stackable, special, members, untradeable, equipSlots));
            id++;
        }

        Logger.info("Loaded " + items.size() + " item definitions from " + filePath);
        return items;
    }

    /**
     * Load wieldable item bonuses from wieldable.json and apply to item definitions.
     * Format: { "itemId": { "animation": N, "armour": N, ... }, ... }
     */
    public static void loadWieldableData(String filePath, List<ItemDefinition> items) throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(filePath)));

        // Match key-value pairs: "itemId": { ... }
        Pattern entryPattern = Pattern.compile("\"(\\d+)\"\\s*:\\s*\\{([^}]+)\\}");
        Matcher entryMatcher = entryPattern.matcher(content);

        int count = 0;
        while (entryMatcher.find()) {
            int itemId = Integer.parseInt(entryMatcher.group(1));
            String obj = entryMatcher.group(2);

            if (itemId >= 0 && itemId < items.size()) {
                int animation = extractIntOr(obj, "animation", 0);
                int armour = extractIntOr(obj, "armour", 0);
                int weaponAim = extractIntOr(obj, "weaponAim", 0);
                int weaponPower = extractIntOr(obj, "weaponPower", 0);
                int magic = extractIntOr(obj, "magic", 0);
                int prayer = extractIntOr(obj, "prayer", 0);

                items.get(itemId).setWieldableStats(animation, armour, weaponAim, weaponPower, magic, prayer);
                count++;
            }
        }

        Logger.info("Loaded " + count + " wieldable data entries from " + filePath);
    }

    /**
     * Load NPC type definitions from config/npcs.json.
     * Format: Array of objects with name, attack, strength, hits, defense, hostility, etc.
     */
    public static List<NpcDefinition> loadNpcDefinitions(String filePath) throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(filePath)));
        List<NpcDefinition> npcs = new ArrayList<>();

        Pattern objPattern = Pattern.compile("\\{\\s*\"name\"[^}]+\"combatAnimation\"\\s*:\\s*\\d+\\s*\\}", Pattern.DOTALL);
        Matcher objMatcher = objPattern.matcher(content);

        int id = 0;
        while (objMatcher.find()) {
            String obj = objMatcher.group();

            String name = extractString(obj, "name");
            String description = extractStringOrNull(obj, "description");
            String command = extractStringOrNull(obj, "command");
            int attack = extractIntOr(obj, "attack", 0);
            int strength = extractIntOr(obj, "strength", 0);
            int hits = extractIntOr(obj, "hits", 0);
            int defense = extractIntOr(obj, "defense", 0);
            String hostility = extractStringOrNull(obj, "hostility");
            int combatAnimation = extractIntOr(obj, "combatAnimation", 0);

            npcs.add(new NpcDefinition(id, name, description, command,
                                       attack, strength, hits, defense,
                                       hostility, combatAnimation));
            id++;
        }

        Logger.info("Loaded " + npcs.size() + " NPC definitions from " + filePath);
        return npcs;
    }

    /**
     * Load edible item data from edible.json.
     * Returns map of itemId -> healAmount.
     */
    public static Map<Integer, Integer> loadEdibleData(String filePath) throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(filePath)));
        Map<Integer, Integer> edible = new HashMap<>();

        // Match simple entries: "id": healAmount
        Pattern simplePattern = Pattern.compile("\"(\\d+)\"\\s*:\\s*(\\d+)");
        Matcher simpleMatcher = simplePattern.matcher(content);
        while (simpleMatcher.find()) {
            int itemId = Integer.parseInt(simpleMatcher.group(1));
            int heal = Integer.parseInt(simpleMatcher.group(2));
            edible.put(itemId, heal);
        }

        // Match object entries: "id": { "hits": N, ... }
        Pattern objPattern = Pattern.compile("\"(\\d+)\"\\s*:\\s*\\{[^}]*\"hits\"\\s*:\\s*(\\d+)[^}]*\\}");
        Matcher objMatcher = objPattern.matcher(content);
        while (objMatcher.find()) {
            int itemId = Integer.parseInt(objMatcher.group(1));
            int heal = Integer.parseInt(objMatcher.group(2));
            edible.put(itemId, heal);
        }

        Logger.info("Loaded " + edible.size() + " edible items from " + filePath);
        return edible;
    }

    // ===== Additional helper methods =====

    private static String extractString(String obj, String key) {
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]*?)\"");
        Matcher m = p.matcher(obj);
        if (m.find()) return m.group(1);
        return "";
    }

    private static String extractStringOrNull(String obj, String key) {
        // Check for null value first
        Pattern nullP = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*null");
        if (nullP.matcher(obj).find()) return null;

        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]*?)\"");
        Matcher m = p.matcher(obj);
        if (m.find()) return m.group(1);
        return null;
    }

    private static boolean extractBoolOr(String obj, String key, boolean defaultValue) {
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(true|false)");
        Matcher m = p.matcher(obj);
        if (m.find()) return Boolean.parseBoolean(m.group(1));
        return defaultValue;
    }

    private static List<String> extractStringArray(String obj, String key) {
        // Check for null
        Pattern nullP = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*null");
        if (nullP.matcher(obj).find()) return null;

        // Match array: "key": ["val1", "val2"]
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\\[([^\\]]*)\\]");
        Matcher m = p.matcher(obj);
        if (!m.find()) return null;

        String arrayContent = m.group(1).trim();
        if (arrayContent.isEmpty()) return new ArrayList<>();

        List<String> result = new ArrayList<>();
        Pattern valP = Pattern.compile("\"([^\"]+)\"");
        Matcher valM = valP.matcher(arrayContent);
        while (valM.find()) {
            result.add(valM.group(1));
        }
        return result.isEmpty() ? null : result;
    }
}
