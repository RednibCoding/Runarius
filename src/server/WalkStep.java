/**
 * Represents a single step in a player's walk path.
 * Each step contains the target coordinates and movement direction.
 */
public class WalkStep {
    private final int targetX;
    private final int targetY;
    private final int direction; // 0-7 for 8 directions, or -1 for no direction
    
    public WalkStep(int targetX, int targetY) {
        this.targetX = targetX;
        this.targetY = targetY;
        this.direction = -1; // Will be calculated when needed
    }
    
    public WalkStep(int targetX, int targetY, int direction) {
        this.targetX = targetX;
        this.targetY = targetY;
        this.direction = direction;
    }
    
    public int getTargetX() {
        return targetX;
    }
    
    public int getTargetY() {
        return targetY;
    }
    
    public int getDirection() {
        return direction;
    }
    
    /**
     * Calculate the walking direction from current position to this step.
     * Returns 0-7 representing 8 directions:
     * 0 = North, 1 = NE, 2 = East, 3 = SE, 4 = South, 5 = SW, 6 = West, 7 = NW
     */
    public static int calculateDirection(int fromX, int fromY, int toX, int toY) {
        int deltaX = toX - fromX;
        int deltaY = toY - fromY;
        
        // Normalize to -1, 0, or 1
        int dx = Integer.compare(deltaX, 0);
        int dy = Integer.compare(deltaY, 0);
        
        // Map to direction (0-7)
        if (dx == 0 && dy == -1) return 0;  // North
        if (dx == 1 && dy == -1) return 1;  // NE
        if (dx == 1 && dy == 0) return 2;   // East
        if (dx == 1 && dy == 1) return 3;   // SE
        if (dx == 0 && dy == 1) return 4;   // South
        if (dx == -1 && dy == 1) return 5;  // SW
        if (dx == -1 && dy == 0) return 6;  // West
        if (dx == -1 && dy == -1) return 7; // NW
        
        return -1; // No movement
    }
    
    @Override
    public String toString() {
        return "WalkStep[x=" + targetX + ", y=" + targetY + ", dir=" + direction + "]";
    }
}
