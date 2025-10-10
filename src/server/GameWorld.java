import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the game world state including all players, NPCs, objects, and items.
 * This is the central authority for game state.
 * 
 * NOTE: Renamed from "World" to "GameWorld" to avoid naming conflict with client's World class.
 * The client has its own World class for rendering/terrain, so server uses GameWorld.
 */
public class GameWorld {
    private static GameWorld instance;
    
    // Player management
    private Map<String, Player> players = new ConcurrentHashMap<>();
    private Map<Long, Player> playersBySessionId = new ConcurrentHashMap<>();
    private Map<Long, Player> pendingPlayers = new ConcurrentHashMap<>(); // Players with session but not logged in
    private int nextServerId = 1;
    
    // World configuration
    private static final int PLANE_WIDTH = 944;
    private static final int PLANE_HEIGHT = 944;
    private static final int PLANE_MULTIPLIER = 1;
    
    // Default spawn location
    // Using coordinates that have terrain data in land63.jag (around section 50, 50)
    // Lumbridge (122, 657) doesn't have terrain data in our JAG files
    private static final int DEFAULT_SPAWN_X = 1400;
    private static final int DEFAULT_SPAWN_Y = 1400;
    private static final int DEFAULT_PLANE = 0;
    
    private GameWorld() {
        // Private constructor for singleton
    }
    
    public static synchronized GameWorld getInstance() {
        if (instance == null) {
            instance = new GameWorld();
        }
        return instance;
    }
    
    /**
     * Register a new player in the world.
     */
    public void addPlayer(Player player) {
        player.setServerId(nextServerId++);
        player.setX(DEFAULT_SPAWN_X);
        player.setY(DEFAULT_SPAWN_Y);
        player.setPlaneIndex(DEFAULT_PLANE);
        
        players.put(player.getUsername().toLowerCase(), player);
        playersBySessionId.put(player.getSessionId(), player);
        
        Logger.info("Player " + player.getUsername() + " joined the world (ID: " + player.getServerId() + ")");
    }
    
    /**
     * Remove a player from the world.
     */
    public void removePlayer(Player player) {
        players.remove(player.getUsername().toLowerCase());
        playersBySessionId.remove(player.getSessionId());
        
        Logger.info("Player " + player.getUsername() + " left the world");
    }
    
    /**
     * Get a player by username.
     */
    public Player getPlayer(String username) {
        return players.get(username.toLowerCase());
    }
    
    /**
     * Get a player by session ID.
     */
    public Player getPlayerBySessionId(long sessionId) {
        return playersBySessionId.get(sessionId);
    }
    
    /**
     * Get a player by their socket connection.
     */
    public Player getPlayerBySocket(java.net.Socket socket) {
        for (Player player : players.values()) {
            if (player.getSocket() == socket) {
                return player;
            }
        }
        // Also check pending players
        for (Player player : pendingPlayers.values()) {
            if (player.getSocket() == socket) {
                return player;
            }
        }
        return null;
    }
    
    /**
     * Add a pending player (has session but not logged in yet).
     */
    public void addPendingPlayer(Player player) {
        pendingPlayers.put(player.getSessionId(), player);
        Logger.info("Pending player created: " + player.getUsername() + " (session: " + player.getSessionId() + ")");
    }
    
    /**
     * Get a pending player by session ID and remove from pending list.
     */
    public Player getPendingPlayer(long sessionId) {
        Player player = pendingPlayers.get(sessionId);
        if (player != null) {
            pendingPlayers.remove(sessionId);
        }
        return player;
    }
    
    /**
     * Get all online players.
     */
    public List<Player> getAllPlayers() {
        return new ArrayList<>(players.values());
    }
    
    /**
     * Get all players near a specific location.
     */
    public List<Player> getNearbyPlayers(int x, int y, int radius) {
        List<Player> nearbyPlayers = new ArrayList<>();
        for (Player player : players.values()) {
            int dx = Math.abs(player.getX() - x);
            int dy = Math.abs(player.getY() - y);
            if (dx <= radius && dy <= radius) {
                nearbyPlayers.add(player);
            }
        }
        return nearbyPlayers;
    }
    
    /**
     * Check if a player is online.
     */
    public boolean isPlayerOnline(String username) {
        return players.containsKey(username.toLowerCase());
    }
    
    /**
     * Check if a player with given hash is online.
     */
    public boolean isPlayerOnline(long usernameHash) {
        for (Player player : players.values()) {
            if (player.getUsernameHash() == usernameHash) {
                return true;
            }
        }
        return false;
    }
    
    public int getPlaneWidth() {
        return PLANE_WIDTH;
    }
    
    public int getPlaneHeight() {
        return PLANE_HEIGHT;
    }
    
    public int getPlaneMultiplier() {
        return PLANE_MULTIPLIER;
    }
}
