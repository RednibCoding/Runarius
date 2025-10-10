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
    }
    
    /**
     * Process one movement step for each walking player
     */
    private void processPlayerMovement() {
        GameWorld world = GameWorld.getInstance();
        
        for (Player player : world.getAllPlayers()) {
            if (player == null || !player.hasWalkSteps()) {
                continue;
            }
            
            try {
                // Get next step from queue
                int[] step = player.getWalkQueue().poll();
                if (step == null) {
                    continue;
                }
                
                int deltaX = step[0];
                int deltaY = step[1];
                
                Logger.debug("Processing step for " + player.getUsername() + 
                           ": deltaX=" + deltaX + ", deltaY=" + deltaY);
                
                // Update player position
                int oldX = player.getX();
                int oldY = player.getY();
                int newX = oldX + deltaX;
                int newY = oldY + deltaY;
                
                Logger.debug("  Movement: (" + oldX + "," + oldY + ") + (" + 
                           deltaX + "," + deltaY + ") = (" + newX + "," + newY + ")");
                
                player.setX((short) newX);
                player.setY((short) newY);
                player.setWalking(true);
                
                Logger.debug("Player " + player.getUsername() + " walked: " +
                           "(" + oldX + "," + oldY + ") -> (" + newX + "," + newY + ")");
                
                // Send position update to the player
                CL_WalkHandler.sendPositionUpdate(player);
                
                // TODO: Broadcast to nearby players
                
            } catch (Exception e) {
                Logger.error("Error processing movement for " + player.getUsername() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    public long getTickCount() {
        return tickCount;
    }
}
