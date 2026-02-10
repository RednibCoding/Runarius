import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class WorldService {
    private static final int WORLD_WIDTH = 2304;
    private static final int WORLD_HEIGHT = 1776;
    private static final int DEFAULT_SPAWN_X = 122;
    private static final int DEFAULT_SPAWN_Y = 657;
    private static final int DEFAULT_PLANE = 0;

    // World entities loaded from data files
    private List<Npc> npcs = new ArrayList<>();
    private List<DataLoader.GameObjectData> objects = new ArrayList<>();
    private List<DataLoader.WallObjectData> wallObjects = new ArrayList<>();
    private List<DataLoader.GroundItemData> groundItems = new ArrayList<>();

    // Definitions loaded from config files
    private List<ItemDefinition> itemDefinitions = new ArrayList<>();
    private List<NpcDefinition> npcDefinitions = new ArrayList<>();
    private Map<Integer, Integer> edibleItems = new HashMap<>();

    public void spawnPlayer(Player player) {
        player.setX(DEFAULT_SPAWN_X);
        player.setY(DEFAULT_SPAWN_Y);
        player.setPlaneIndex(DEFAULT_PLANE);
    }

    /**
     * Load all world data from JSON files.
     * Call this once during server startup.
     */
    public void loadData(String dataPath) {
        // Load config definitions first (item defs, NPC defs)
        try {
            itemDefinitions = DataLoader.loadItemDefinitions(dataPath + "/config/items.json");
            DataLoader.loadWieldableData(dataPath + "/wieldable.json", itemDefinitions);
        } catch (IOException ex) {
            Logger.error("Failed to load item definitions: " + ex.getMessage());
        }

        try {
            npcDefinitions = DataLoader.loadNpcDefinitions(dataPath + "/config/npcs.json");
        } catch (IOException ex) {
            Logger.error("Failed to load NPC definitions: " + ex.getMessage());
        }

        try {
            edibleItems = DataLoader.loadEdibleData(dataPath + "/edible.json");
        } catch (IOException ex) {
            Logger.error("Failed to load edible data: " + ex.getMessage());
        }

        // Load spawn locations
        try {
            npcs = DataLoader.loadNpcs(dataPath + "/locations/npcs.json");
            // Initialize NPC combat stats from definitions
            for (Npc npc : npcs) {
                NpcDefinition def = getNpcDefinition(npc.getTypeId());
                npc.initFromDefinition(def);
            }
        } catch (IOException ex) {
            Logger.error("Failed to load NPCs: " + ex.getMessage());
        }

        try {
            objects = DataLoader.loadObjects(dataPath + "/locations/objects.json");
        } catch (IOException ex) {
            Logger.error("Failed to load objects: " + ex.getMessage());
        }

        try {
            wallObjects = DataLoader.loadWallObjects(dataPath + "/locations/wall-objects.json");
        } catch (IOException ex) {
            Logger.error("Failed to load wall objects: " + ex.getMessage());
        }

        try {
            groundItems = DataLoader.loadGroundItems(dataPath + "/locations/items.json");
        } catch (IOException ex) {
            Logger.error("Failed to load ground items: " + ex.getMessage());
        }

        Logger.info("World data loaded: " + npcs.size() + " NPCs, " +
                    objects.size() + " objects, " + wallObjects.size() + " walls, " +
                    groundItems.size() + " ground items, " +
                    itemDefinitions.size() + " item defs, " +
                    npcDefinitions.size() + " NPC defs");
    }

    // ===== Query methods =====

    public List<Npc> getNpcs() {
        return Collections.unmodifiableList(npcs);
    }

    public List<Npc> getNearbyNpcs(int x, int y, int radius) {
        List<Npc> result = new ArrayList<>();
        for (Npc npc : npcs) {
            if (Math.abs(npc.getX() - x) <= radius && Math.abs(npc.getY() - y) <= radius) {
                result.add(npc);
            }
        }
        return result;
    }

    public List<DataLoader.GameObjectData> getNearbyObjects(int x, int y, int radius) {
        List<DataLoader.GameObjectData> result = new ArrayList<>();
        for (DataLoader.GameObjectData obj : objects) {
            if (Math.abs(obj.x - x) <= radius && Math.abs(obj.y - y) <= radius) {
                result.add(obj);
            }
        }
        return result;
    }

    public List<DataLoader.WallObjectData> getNearbyWallObjects(int x, int y, int radius) {
        List<DataLoader.WallObjectData> result = new ArrayList<>();
        for (DataLoader.WallObjectData wall : wallObjects) {
            if (Math.abs(wall.x - x) <= radius && Math.abs(wall.y - y) <= radius) {
                result.add(wall);
            }
        }
        return result;
    }

    public List<DataLoader.GroundItemData> getNearbyGroundItems(int x, int y, int radius) {
        List<DataLoader.GroundItemData> result = new ArrayList<>();
        for (DataLoader.GroundItemData item : groundItems) {
            if (Math.abs(item.x - x) <= radius && Math.abs(item.y - y) <= radius) {
                result.add(item);
            }
        }
        return result;
    }

    public int getWorldWidth() {
        return WORLD_WIDTH;
    }

    public int getWorldHeight() {
        return WORLD_HEIGHT;
    }

    public int getPlaneMultiplier() {
        return 1;
    }

    // ===== Definition Queries =====

    public ItemDefinition getItemDefinition(int itemId) {
        if (itemId >= 0 && itemId < itemDefinitions.size()) {
            return itemDefinitions.get(itemId);
        }
        return null;
    }

    public NpcDefinition getNpcDefinition(int npcTypeId) {
        if (npcTypeId >= 0 && npcTypeId < npcDefinitions.size()) {
            return npcDefinitions.get(npcTypeId);
        }
        return null;
    }

    public List<ItemDefinition> getItemDefinitions() {
        return Collections.unmodifiableList(itemDefinitions);
    }

    public List<NpcDefinition> getNpcDefinitions() {
        return Collections.unmodifiableList(npcDefinitions);
    }

    public int getEdibleHealAmount(int itemId) {
        return edibleItems.getOrDefault(itemId, 0);
    }

    public boolean isEdible(int itemId) {
        return edibleItems.containsKey(itemId);
    }

    // ===== Ground Item Management =====

    /**
     * Remove a ground item at the specified position with the given item ID.
     * Returns true if the item was found and removed.
     */
    public boolean removeGroundItem(int x, int y, int itemId) {
        Iterator<DataLoader.GroundItemData> it = groundItems.iterator();
        while (it.hasNext()) {
            DataLoader.GroundItemData item = it.next();
            if (item.x == x && item.y == y && item.id == itemId) {
                it.remove();
                Logger.debug("Removed ground item: id=" + itemId + " at (" + x + "," + y + ")");
                return true;
            }
        }
        return false;
    }

    /**
     * Add a ground item to the world.
     */
    public void addGroundItem(int itemId, int x, int y) {
        groundItems.add(new DataLoader.GroundItemData(itemId, x, y, 0));
        Logger.debug("Added ground item: id=" + itemId + " at (" + x + "," + y + ")");
    }

    // ===== NPC Management =====

    /**
     * Find an NPC by its server index.
     */
    public Npc getNpcByServerId(int serverId) {
        for (Npc npc : npcs) {
            if (npc.getServerId() == serverId) {
                return npc;
            }
        }
        return null;
    }

    /**
     * Find a game object at the specified world coordinates.
     */
    public DataLoader.GameObjectData getObjectAt(int x, int y) {
        for (DataLoader.GameObjectData obj : objects) {
            if (obj.x == x && obj.y == y) {
                return obj;
            }
        }
        return null;
    }

    /**
     * Find a wall object at the specified world coordinates.
     */
    public DataLoader.WallObjectData getWallObjectAt(int x, int y) {
        for (DataLoader.WallObjectData wall : wallObjects) {
            if (wall.x == x && wall.y == y) {
                return wall;
            }
        }
        return null;
    }
}
