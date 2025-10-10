import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Game tick system that processes player movement and other time-based updates.
 * Runs at a fixed interval (640ms per tick, matching RSC server-js implementation).
 */
public class GameTick {
    private static final int TICK_INTERVAL_MS = 640; // 640ms = ~1.5 ticks per second
    private static GameTick instance;
    
    private Timer timer;
    private long tickCount = 0;
    
    private GameTick() {
        // Private constructor for singleton
    }
    
    public static GameTick getInstance() {
        if (instance == null) {
            instance = new GameTick();
        }
        return instance;
    }
    
    /**
     * Start the game tick loop
     */
    public void start() {
        if (timer != null) {
            Logger.warn("GameTick already running");
            return;
        }
        
        timer = new Timer("GameTick", true); // daemon thread
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                tick();
            }
        }, TICK_INTERVAL_MS, TICK_INTERVAL_MS);
        
        Logger.info("GameTick started (interval=" + TICK_INTERVAL_MS + "ms)");
    }
    
    /**
     * Stop the game tick loop
     */
    public void stop() {
        if (timer != null) {
            timer.cancel();
            timer = null;
            Logger.info("GameTick stopped");
        }
    }
    
    /**
     * Main game tick - processes all time-based updates
     */
    private void tick() {
        tickCount++;
        
        Logger.info("=== TICK " + tickCount + " START ===");
        
        try {
            // Process movement for all players
            processPlayerMovement();
            
            // TODO: Add other tick-based updates here:
            // - NPC movement
            // - Combat rounds
            // - Skill delays
            // - Item respawns
            // etc.
            
        } catch (Exception e) {
            Logger.error("Error in game tick " + tickCount + ": " + e.getMessage());
            e.printStackTrace();
        }
        
        Logger.info("=== TICK " + tickCount + " END ===");
    }
    
    /**
     * Process one movement step for each walking player.
     * 
     * Based on server-js pattern:
     * Phase 1: Process player ticks - movement + updateNearby for ALL players
     * Phase 2: Send region updates to ALL players
     */
    private void processPlayerMovement() {
        GameWorld world = GameWorld.getInstance();
        
        int playerCount = world.getAllPlayers().size();
        Logger.info("  Processing " + playerCount + " players");
        
        // === PHASE 1: Tick all players (movement + detect nearby entities) ===
        for (Player player : world.getAllPlayers()) {
            if (player == null) {
                continue;
            }
            
            try {
                // FIRST: Update nearby players (detects who's in range)
                // This is critical - must happen EVERY tick, not just on movement!
                updateNearbyPlayers(player);
                
                // THEN: Process movement if player has walk steps
                if (player.hasWalkSteps()) {
                    // Get next step from queue
                    int[] step = player.getWalkQueue().poll();
                    if (step != null) {
                        int deltaX = step[0];
                        int deltaY = step[1];
                        
                        // Update player position
                        int oldX = player.getX();
                        int oldY = player.getY();
                        int newX = oldX + deltaX;
                        int newY = oldY + deltaY;
                        
                        player.setX((short) newX);
                        player.setY((short) newY);
                        player.setWalking(true);
                        
                        // Calculate and update direction (0-7 for N/NE/E/SE/S/SW/W/NW)
                        int direction = calculateDirection(deltaX, deltaY);
                        player.setDirection(direction);
                        
                        // Mark this player as moved in nearby players' tracking sets
                        for (Player nearbyPlayer : world.getNearbyPlayers(newX, newY, 16)) {
                            if (nearbyPlayer != player && nearbyPlayer.getKnownPlayers().contains(player)) {
                                nearbyPlayer.getMovedPlayers().add(player);
                            }
                        }
                        
                        Logger.debug("Player " + player.getUsername() + " walked: " +
                                   "(" + oldX + "," + oldY + ") -> (" + newX + "," + newY + ") dir=" + direction);
                    }
                }
                
            } catch (Exception e) {
                Logger.error("Error processing tick for " + player.getUsername() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        // === PHASE 2: Send region updates to ALL players ===
        Logger.info("  Sending region updates to " + playerCount + " players");
        
        for (Player player : world.getAllPlayers()) {
            try {
                Logger.info("    Calling sendRegionPlayersUpdate for " + player.getUsername());
                CL_WalkHandler.sendRegionPlayersUpdate(player);
            } catch (Exception e) {
                Logger.error("Error sending region update to " + player.getUsername() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Update nearby players tracking (based on server-js updateNearby).
     * This detects:
     * - Players who are now out of range (add to removed)
     * - Players who are now in range (add to added)
     */
    private void updateNearbyPlayers(Player player) {
        GameWorld world = GameWorld.getInstance();
        
        // Check if any known players are now out of range
        for (Player knownPlayer : new ArrayList<>(player.getKnownPlayers())) {
            int distance = Math.max(
                Math.abs(knownPlayer.getX() - player.getX()),
                Math.abs(knownPlayer.getY() - player.getY())
            );
            
            if (distance > 16) {
                player.getRemovedPlayers().add(knownPlayer);
                Logger.debug(player.getUsername() + ": " + knownPlayer.getUsername() + " out of range");
            }
        }
        
        // Check for new players in range
        for (Player nearbyPlayer : world.getNearbyPlayers(player.getX(), player.getY(), 16)) {
            if (nearbyPlayer != player && !player.getKnownPlayers().contains(nearbyPlayer)) {
                player.getAddedPlayers().add(nearbyPlayer);
                Logger.debug(player.getUsername() + ": " + nearbyPlayer.getUsername() + " now in range");
            }
        }
    }
    
    /**
     * Calculate direction (0-7) from movement delta.
     * 
     * Direction encoding:
     * 0 = North (Y-)
     * 1 = Northeast (X+, Y-)
     * 2 = East (X+)
     * 3 = Southeast (X+, Y+)
     * 4 = South (Y+)
     * 5 = Southwest (X-, Y+)
     * 6 = West (X-)
     * 7 = Northwest (X-, Y-)
     */
    private int calculateDirection(int deltaX, int deltaY) {
        if (deltaX == 0 && deltaY < 0) return 0; // North
        if (deltaX > 0 && deltaY < 0) return 1;  // Northeast
        if (deltaX > 0 && deltaY == 0) return 2; // East
        if (deltaX > 0 && deltaY > 0) return 3;  // Southeast
        if (deltaX == 0 && deltaY > 0) return 4; // South
        if (deltaX < 0 && deltaY > 0) return 5;  // Southwest
        if (deltaX < 0 && deltaY == 0) return 6; // West
        if (deltaX < 0 && deltaY < 0) return 7;  // Northwest
        return 0; // Default to North if no movement
    }
    
    public long getTickCount() {
        return tickCount;
    }
}
