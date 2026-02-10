/**
 * Represents an NPC (Non-Player Character) in the game world.
 * NPCs are loaded from data files and spawned at their designated positions.
 */
public class Npc {
    private int serverId;    // Unique server-side ID (12 bits max = 4095)
    private int typeId;      // NPC type from config (determines name, stats, appearance)
    private int x, y;        // Current world tile position
    private int spawnX, spawnY;  // Original spawn position
    private int minX, maxX, minY, maxY;  // Walk boundary box
    private int direction;   // Current facing direction (0-7)

    public Npc(int typeId, int x, int y, int minX, int maxX, int minY, int maxY) {
        this.typeId = typeId;
        this.x = x;
        this.y = y;
        this.spawnX = x;
        this.spawnY = y;
        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
        this.direction = 0;
    }

    public int getServerId() {
        return serverId;
    }

    public void setServerId(int serverId) {
        this.serverId = serverId;
    }

    public int getTypeId() {
        return typeId;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getSpawnX() {
        return spawnX;
    }

    public int getSpawnY() {
        return spawnY;
    }

    public int getMinX() {
        return minX;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMinY() {
        return minY;
    }

    public int getMaxY() {
        return maxY;
    }

    public int getDirection() {
        return direction;
    }

    public void setDirection(int direction) {
        this.direction = direction;
    }

    @Override
    public String toString() {
        return "Npc{type=" + typeId + ", pos=(" + x + "," + y + "), id=" + serverId + "}";
    }
}
