import java.util.Random;

/**
 * Represents an NPC (Non-Player Character) in the game world.
 * NPCs are loaded from data files and spawned at their designated positions.
 */
public class Npc {
    private static final Random random = new Random();

    private int serverId;    // Unique server-side ID (12 bits max = 4095)
    private int typeId;      // NPC type from config (determines name, stats, appearance)
    private int x, y;        // Current world tile position
    private int spawnX, spawnY;  // Original spawn position
    private int minX, maxX, minY, maxY;  // Walk boundary box
    private int direction;   // Current facing direction (0-7)

    // Movement state
    private boolean moved;       // Whether this NPC moved this tick
    private int moveDirection;   // Direction of movement this tick (0-7)
    private int walkTimer;       // Ticks until next random walk attempt
    private boolean canWalk;     // Whether this NPC has walk bounds (can roam)

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
        this.direction = 2; // Face south by default
        this.moved = false;
        this.moveDirection = 0;
        this.walkTimer = random.nextInt(10) + 5; // Random initial delay
        // NPC can walk if it has walk bounds that differ from spawn point
        this.canWalk = (minX != maxX || minY != maxY);
    }

    /**
     * Attempt a random walk step within bounds.
     * Called each game tick. Returns true if the NPC moved.
     */
    public boolean tryRandomWalk() {
        moved = false;

        if (!canWalk) {
            return false;
        }

        walkTimer--;
        if (walkTimer > 0) {
            return false;
        }

        // Reset timer: walk again in 3-10 ticks
        walkTimer = random.nextInt(8) + 3;

        // Pick a random direction (8 possible, or stay still)
        // 50% chance to stay still for more natural movement
        if (random.nextInt(2) == 0) {
            return false;
        }

        int dx = random.nextInt(3) - 1; // -1, 0, or 1
        int dy = random.nextInt(3) - 1;

        if (dx == 0 && dy == 0) {
            return false;
        }

        int newX = x + dx;
        int newY = y + dy;

        // Check walk bounds
        if (newX < minX || newX > maxX || newY < minY || newY > maxY) {
            return false;
        }

        // Move
        x = newX;
        y = newY;
        moveDirection = calculateDirection(dx, dy);
        direction = moveDirection;
        moved = true;
        return true;
    }

    private int calculateDirection(int dx, int dy) {
        if (dx == 0 && dy < 0) return 0;  // North
        if (dx > 0 && dy < 0) return 1;   // NE
        if (dx > 0 && dy == 0) return 2;   // East
        if (dx > 0 && dy > 0) return 3;    // SE
        if (dx == 0 && dy > 0) return 4;   // South
        if (dx < 0 && dy > 0) return 5;    // SW
        if (dx < 0 && dy == 0) return 6;   // West
        if (dx < 0 && dy < 0) return 7;    // NW
        return 0;
    }

    public boolean hasMoved() {
        return moved;
    }

    public void resetMoved() {
        moved = false;
    }

    public int getMoveDirection() {
        return moveDirection;
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
