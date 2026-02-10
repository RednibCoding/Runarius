import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
        try {
            npcs = DataLoader.loadNpcs(dataPath + "/locations/npcs.json");
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
                    groundItems.size() + " ground items");
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
}
