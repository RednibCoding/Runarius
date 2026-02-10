import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles saving and loading player data to/from JSON files.
 * Players are saved to data/players/{username}.json
 */
public class PlayerPersistence {
    private static final String SAVE_DIR = "data/players";

    /**
     * Save a player's data to disk.
     */
    public static void save(Player player) {
        try {
            Path dir = Paths.get(SAVE_DIR);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }

            String filename = player.getUsername().toLowerCase() + ".json";
            Path filePath = dir.resolve(filename);

            StringBuilder sb = new StringBuilder();
            sb.append("{\n");

            // Account info
            sb.append("  \"username\": \"").append(player.getUsername()).append("\",\n");
            sb.append("  \"usernameHash\": ").append(player.getUsernameHash()).append(",\n");

            // Position
            sb.append("  \"x\": ").append(player.getX()).append(",\n");
            sb.append("  \"y\": ").append(player.getY()).append(",\n");
            sb.append("  \"planeIndex\": ").append(player.getPlaneIndex()).append(",\n");

            // Appearance
            sb.append("  \"headGender\": ").append(player.getHeadGender()).append(",\n");
            sb.append("  \"headType\": ").append(player.getHeadType()).append(",\n");
            sb.append("  \"bodyGender\": ").append(player.getBodyGender()).append(",\n");
            sb.append("  \"hairColour\": ").append(player.getHairColour()).append(",\n");
            sb.append("  \"topColour\": ").append(player.getTopColour()).append(",\n");
            sb.append("  \"bottomColour\": ").append(player.getBottomColour()).append(",\n");
            sb.append("  \"skinColour\": ").append(player.getSkinColour()).append(",\n");

            // Stats
            sb.append("  \"currentStats\": ").append(intArrayToJson(player.getCurrentStats())).append(",\n");
            sb.append("  \"baseStats\": ").append(intArrayToJson(player.getBaseStats())).append(",\n");
            sb.append("  \"experience\": ").append(intArrayToJson(player.getExperience())).append(",\n");

            // Combat style and quest points
            sb.append("  \"combatStyle\": ").append(player.getCombatStyle()).append(",\n");
            sb.append("  \"questPoints\": ").append(player.getQuestPoints()).append(",\n");
            sb.append("  \"fatigue\": ").append(player.getFatigue()).append(",\n");

            // Settings
            sb.append("  \"blockChat\": ").append(player.isBlockChat()).append(",\n");
            sb.append("  \"blockPrivateMessages\": ").append(player.isBlockPrivateMessages()).append(",\n");
            sb.append("  \"blockTrade\": ").append(player.isBlockTrade()).append(",\n");
            sb.append("  \"blockDuel\": ").append(player.isBlockDuel()).append(",\n");

            // Inventory
            sb.append("  \"inventory\": [");
            List<Item> inventory = player.getInventory();
            for (int i = 0; i < inventory.size(); i++) {
                Item item = inventory.get(i);
                sb.append("{\"id\":").append(item.getId())
                  .append(",\"amount\":").append(item.getAmount())
                  .append(",\"equipped\":").append(item.isEquipped())
                  .append("}");
                if (i < inventory.size() - 1) sb.append(",");
            }
            sb.append("],\n");

            // Friends list
            sb.append("  \"friendList\": ").append(longListToJson(player.getFriendList())).append(",\n");

            // Ignore list
            sb.append("  \"ignoreList\": ").append(longListToJson(player.getIgnoreList())).append(",\n");

            // Login tracking
            sb.append("  \"lastLoginTime\": ").append(System.currentTimeMillis()).append(",\n");
            sb.append("  \"lastLoginIP\": \"").append(getPlayerIP(player)).append("\"\n");

            sb.append("}\n");

            Files.writeString(filePath, sb.toString());
            Logger.info("Saved player data: " + player.getUsername());

        } catch (IOException ex) {
            Logger.error("Failed to save player " + player.getUsername() + ": " + ex.getMessage());
        }
    }

    /**
     * Load a player's saved data. Returns true if save file was found and loaded.
     */
    public static boolean load(Player player) {
        try {
            String filename = player.getUsername().toLowerCase() + ".json";
            Path filePath = Paths.get(SAVE_DIR, filename);

            if (!Files.exists(filePath)) {
                Logger.info("No save file for " + player.getUsername() + " - using defaults");
                return false;
            }

            String content = Files.readString(filePath);

            // Position
            player.setX(extractInt(content, "\"x\""));
            player.setY(extractInt(content, "\"y\""));
            player.setPlaneIndex(extractIntOr(content, "planeIndex", 0));

            // Appearance
            int headGender = extractIntOr(content, "headGender", 1);
            int headType = extractIntOr(content, "headType", 1);
            int bodyGender = extractIntOr(content, "bodyGender", 1);
            int hairColour = extractIntOr(content, "hairColour", 2);
            int topColour = extractIntOr(content, "topColour", 8);
            int bottomColour = extractIntOr(content, "bottomColour", 14);
            int skinColour = extractIntOr(content, "skinColour", 0);
            player.setAppearance(headGender, headType, bodyGender, hairColour, topColour, bottomColour, skinColour);

            // Stats
            int[] currentStats = extractIntArray(content, "currentStats", 18);
            int[] baseStats = extractIntArray(content, "baseStats", 18);
            int[] experience = extractIntArray(content, "experience", 18);
            if (currentStats != null) System.arraycopy(currentStats, 0, player.getCurrentStats(), 0, 18);
            if (baseStats != null) System.arraycopy(baseStats, 0, player.getBaseStats(), 0, 18);
            if (experience != null) System.arraycopy(experience, 0, player.getExperience(), 0, 18);

            // Combat style and quest points
            player.setCombatStyle(extractIntOr(content, "combatStyle", 0));
            player.setFatigue(extractIntOr(content, "fatigue", 0));

            // Settings
            player.setBlockChat(extractBool(content, "blockChat", false));
            player.setBlockPrivateMessages(extractBool(content, "blockPrivateMessages", false));
            player.setBlockTrade(extractBool(content, "blockTrade", false));
            player.setBlockDuel(extractBool(content, "blockDuel", false));

            // Inventory
            loadInventory(player, content);

            // Friends list
            loadLongList(player.getFriendList(), content, "friendList");

            // Ignore list
            loadLongList(player.getIgnoreList(), content, "ignoreList");

            Logger.info("Loaded player data: " + player.getUsername() + " at (" + player.getX() + "," + player.getY() + ")");
            return true;

        } catch (Exception ex) {
            Logger.error("Failed to load player " + player.getUsername() + ": " + ex.getMessage());
            return false;
        }
    }

    /**
     * Check if a save file exists for the given username.
     */
    public static boolean hasSaveFile(String username) {
        Path filePath = Paths.get(SAVE_DIR, username.toLowerCase() + ".json");
        return Files.exists(filePath);
    }

    /**
     * Get the last login time from save file (for welcome screen).
     * Returns 0 if no save file.
     */
    public static long getLastLoginTime(String username) {
        try {
            Path filePath = Paths.get(SAVE_DIR, username.toLowerCase() + ".json");
            if (!Files.exists(filePath)) return 0;
            String content = Files.readString(filePath);
            return extractLongOr(content, "lastLoginTime", 0);
        } catch (Exception ex) {
            return 0;
        }
    }

    /**
     * Get the last login IP from save file (for welcome screen).
     */
    public static String getLastLoginIP(String username) {
        try {
            Path filePath = Paths.get(SAVE_DIR, username.toLowerCase() + ".json");
            if (!Files.exists(filePath)) return "0.0.0.0";
            String content = Files.readString(filePath);
            return extractStringOr(content, "lastLoginIP", "0.0.0.0");
        } catch (Exception ex) {
            return "0.0.0.0";
        }
    }

    // ===== Helper methods =====

    private static String intArrayToJson(int[] arr) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < arr.length; i++) {
            sb.append(arr[i]);
            if (i < arr.length - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    private static String longListToJson(List<Long> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i));
            if (i < list.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    private static String getPlayerIP(Player player) {
        try {
            return player.getSocket().getInetAddress().getHostAddress();
        } catch (Exception ex) {
            return "0.0.0.0";
        }
    }

    private static int extractInt(String content, String key) {
        Pattern p = Pattern.compile(Pattern.quote(key) + "\\s*:\\s*(-?\\d+)");
        Matcher m = p.matcher(content);
        if (m.find()) return Integer.parseInt(m.group(1));
        throw new RuntimeException("Key not found: " + key);
    }

    private static int extractIntOr(String content, String key, int defaultValue) {
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(-?\\d+)");
        Matcher m = p.matcher(content);
        if (m.find()) return Integer.parseInt(m.group(1));
        return defaultValue;
    }

    private static long extractLongOr(String content, String key, long defaultValue) {
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(-?\\d+)");
        Matcher m = p.matcher(content);
        if (m.find()) return Long.parseLong(m.group(1));
        return defaultValue;
    }

    private static String extractStringOr(String content, String key, String defaultValue) {
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]+)\"");
        Matcher m = p.matcher(content);
        if (m.find()) return m.group(1);
        return defaultValue;
    }

    private static boolean extractBool(String content, String key, boolean defaultValue) {
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(true|false)");
        Matcher m = p.matcher(content);
        if (m.find()) return Boolean.parseBoolean(m.group(1));
        return defaultValue;
    }

    private static int[] extractIntArray(String content, String key, int expectedLen) {
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\\[([^\\]]+)\\]");
        Matcher m = p.matcher(content);
        if (!m.find()) return null;

        String[] parts = m.group(1).split(",");
        int[] result = new int[expectedLen];
        for (int i = 0; i < Math.min(parts.length, expectedLen); i++) {
            result[i] = Integer.parseInt(parts[i].trim());
        }
        return result;
    }

    private static void loadInventory(Player player, String content) {
        player.getInventory().clear();

        // Extract inventory array content
        Pattern p = Pattern.compile("\"inventory\"\\s*:\\s*\\[(.+?)\\]", Pattern.DOTALL);
        Matcher m = p.matcher(content);
        if (!m.find()) return;

        String invContent = m.group(1);
        Pattern itemP = Pattern.compile("\\{\"id\":(\\d+),\"amount\":(\\d+),\"equipped\":(true|false)\\}");
        Matcher itemM = itemP.matcher(invContent);

        while (itemM.find()) {
            int id = Integer.parseInt(itemM.group(1));
            int amount = Integer.parseInt(itemM.group(2));
            boolean equipped = Boolean.parseBoolean(itemM.group(3));
            player.getInventory().add(new Item(id, amount, equipped));
        }
    }

    private static void loadLongList(List<Long> list, String content, String key) {
        list.clear();

        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\\[([^\\]]*?)\\]");
        Matcher m = p.matcher(content);
        if (!m.find()) return;

        String values = m.group(1).trim();
        if (values.isEmpty()) return;

        String[] parts = values.split(",");
        for (String part : parts) {
            long val = Long.parseLong(part.trim());
            list.add(val);
        }
    }
}
