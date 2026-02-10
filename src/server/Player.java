import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * Represents a connected player in the game world.
 * Keeps track of player state, position, stats, and connection info.
 */
public class Player {
    // Network
    private Socket socket;
    private long sessionId;
    
    // Account info
    private String username;
    private long usernameHash;
    private int serverId;
    
    // Multiplayer tracking (based on server-js LocalEntities pattern)
    // These track which players this player currently knows about
    private Set<Player> knownPlayers = new HashSet<>();      // Currently visible players
    private Set<Player> addedPlayers = new HashSet<>();      // New players this tick
    private Set<Player> removedPlayers = new HashSet<>();    // Players leaving this tick
    private Set<Player> movedPlayers = new HashSet<>();      // Players that moved this tick
    private int direction = 0;  // Current facing direction (0-7)
    
    // NPC tracking (which NPCs this player knows about)
    private Set<Npc> knownNpcs = new HashSet<>();
    private Set<Npc> addedNpcs = new HashSet<>();
    private Set<Npc> removedNpcs = new HashSet<>();
    
    // Position
    private int x;
    private int y;
    private int planeIndex;
    
    // Movement - Queue of coordinate deltas to process
    // Each step is processed one per game tick for smooth walking
    private Queue<int[]> walkQueue = new LinkedList<>(); // Each entry: [deltaX, deltaY]
    private boolean isWalking = false;
    
    // Combat stats (18 total in RSC)
    private int[] currentStats = new int[18];
    private int[] baseStats = new int[18];
    private int[] experience = new int[18];
    
    // Appearance
    private int headGender;
    private int headType;
    private int bodyGender;
    private int hairColour;
    private int topColour;
    private int bottomColour;
    private int skinColour;
    
    // Inventory (max 30 items in RSC)
    private List<Item> inventory = new ArrayList<>();
    private static final int MAX_INVENTORY_SIZE = 30;
    
    // Equipment
    private List<Integer> equippedItems = new ArrayList<>();
    
    // Social
    private List<Long> friendList = new ArrayList<>();
    private List<Long> ignoreList = new ArrayList<>();
    
    // Settings
    private boolean blockChat;
    private boolean blockPrivateMessages;
    private boolean blockTrade;
    private boolean blockDuel;
    
    // State
    private int combatStyle;
    private boolean[] prayersActive = new boolean[14]; // 14 prayers in RSC
    private int questPoints;
    private boolean[] questsCompleted = new boolean[50];
    
    // Fatigue system
    private int fatigue;
    
    public Player(Socket socket, String username, long sessionId) {
        this.socket = socket;
        this.username = username;
        this.sessionId = sessionId;
        this.usernameHash = NetHelper.hashUsername(username);
        
        // Initialize default stats
        initializeStats();
        
        // Initialize default appearance
        initializeAppearance();
    }
    
    private void initializeStats() {
        // Set all base stats to 1 with 0 experience (typical RSC starting stats)
        for (int i = 0; i < 18; i++) {
            baseStats[i] = 1;
            currentStats[i] = 1;
            experience[i] = 0;
        }
        
        // Set hitpoints to 10 (stat index 3)
        baseStats[3] = 10;
        currentStats[3] = 10;
        experience[3] = 1154; // Starting HP experience
    }
    
    private void initializeAppearance() {
        // Default appearance values (brown hair, green shirt, brown pants, light skin)
    this.headGender = 1;
    this.headType = 1;
    this.bodyGender = 1;
        this.hairColour = 2;      // Brown
        this.topColour = 8;       // Green
        this.bottomColour = 14;   // Brown
        this.skinColour = 0;      // Light skin
    }
    
    // Getters and setters
    public Socket getSocket() {
        return socket;
    }
    
    public long getSessionId() {
        return sessionId;
    }
    
    public String getUsername() {
        return username;
    }
    
    public long getUsernameHash() {
        return usernameHash;
    }
    
    public int getServerId() {
        return serverId;
    }
    
    public void setServerId(int serverId) {
        this.serverId = serverId;
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
    
    public int getPlaneIndex() {
        return planeIndex;
    }
    
    public void setPlaneIndex(int planeIndex) {
        this.planeIndex = planeIndex;
    }
    
    public int[] getCurrentStats() {
        return currentStats;
    }
    
    public int[] getBaseStats() {
        return baseStats;
    }
    
    public int[] getExperience() {
        return experience;
    }
    
    public List<Item> getInventory() {
        return inventory;
    }
    
    public boolean addItem(int itemId, int amount) {
        if (inventory.size() >= MAX_INVENTORY_SIZE) {
            return false;
        }
        
        // Check if item is stackable and already in inventory
        for (Item item : inventory) {
            if (item.getId() == itemId && item.isStackable()) {
                item.setAmount(item.getAmount() + amount);
                return true;
            }
        }
        
        // Add new item
        inventory.add(new Item(itemId, amount));
        return true;
    }
    
    public boolean removeItem(int itemId, int amount) {
        for (int i = 0; i < inventory.size(); i++) {
            Item item = inventory.get(i);
            if (item.getId() == itemId) {
                if (item.getAmount() > amount) {
                    item.setAmount(item.getAmount() - amount);
                } else {
                    inventory.remove(i);
                }
                return true;
            }
        }
        return false;
    }
    
    public List<Long> getFriendList() {
        return friendList;
    }
    
    public List<Long> getIgnoreList() {
        return ignoreList;
    }
    
    public boolean isBlockChat() {
        return blockChat;
    }
    
    public void setBlockChat(boolean blockChat) {
        this.blockChat = blockChat;
    }
    
    public boolean isBlockPrivateMessages() {
        return blockPrivateMessages;
    }
    
    public void setBlockPrivateMessages(boolean blockPrivateMessages) {
        this.blockPrivateMessages = blockPrivateMessages;
    }
    
    public boolean isBlockTrade() {
        return blockTrade;
    }
    
    public void setBlockTrade(boolean blockTrade) {
        this.blockTrade = blockTrade;
    }
    
    public boolean isBlockDuel() {
        return blockDuel;
    }
    
    public void setBlockDuel(boolean blockDuel) {
        this.blockDuel = blockDuel;
    }
    
    public int getCombatStyle() {
        return combatStyle;
    }
    
    public void setCombatStyle(int combatStyle) {
        this.combatStyle = combatStyle;
    }
    
    public int getQuestPoints() {
        return questPoints;
    }
    
    public boolean[] getQuestsCompleted() {
        return questsCompleted;
    }
    
    public int getFatigue() {
        return fatigue;
    }
    
    public void setFatigue(int fatigue) {
        this.fatigue = fatigue;
    }
    
    public List<Integer> getEquippedItems() {
        return equippedItems;
    }
    
    public void setAppearance(int headGender, int headType, int bodyGender, 
                             int hairColour, int topColour, int bottomColour, int skinColour) {
        this.headGender = headGender;
        this.headType = headType;
        this.bodyGender = bodyGender;
        this.hairColour = hairColour;
        this.topColour = topColour;
        this.bottomColour = bottomColour;
        this.skinColour = skinColour;
    }
    
    public int getHeadGender() {
        return headGender;
    }
    
    public int getHeadType() {
        return headType;
    }
    
    public int getBodyGender() {
        return bodyGender;
    }
    
    public int getHairColour() {
        return hairColour;
    }
    
    public int getTopColour() {
        return topColour;
    }
    
    public int getBottomColour() {
        return bottomColour;
    }
    
    public int getSkinColour() {
        return skinColour;
    }
    
    // American spelling aliases for appearance methods
    public int getHairColor() {
        return hairColour;
    }
    
    public int getTopColor() {
        return topColour;
    }
    
    public int getBottomColor() {
        return bottomColour;
    }
    
    public int getSkinColor() {
        return skinColour;
    }
    
    /**
     * Calculate combat level based on stats (RSC formula)
     */
    public int getCombatLevel() {
        // RSC Combat level formula:
        // Base = (Defence + Hits + Prayer/2) * 0.25
        // Melee = (Attack + Strength) * 0.325
        // Magic = Magic * 1.5 * 0.325
        // Range = Ranged * 1.5 * 0.325
        // Combat Level = Base + highest of (Melee, Magic, Range)
        
        int defence = baseStats[1];   // Defence (index 1)
        int hits = baseStats[3];      // Hits (index 3)
        int prayer = baseStats[5];    // Prayer (index 5)
        int attack = baseStats[0];    // Attack (index 0)
        int strength = baseStats[2];  // Strength (index 2)
        int magic = baseStats[6];     // Magic (index 6)
        int ranged = baseStats[4];    // Ranged (index 4)
        
        double base = (defence + hits + prayer / 2.0) * 0.25;
        double melee = (attack + strength) * 0.325;
        double mage = magic * 1.5 * 0.325;
        double range = ranged * 1.5 * 0.325;
        
        double combatLevel = base + Math.max(melee, Math.max(mage, range));
        
        return (int) Math.floor(combatLevel);
    }
    
        // ===== Movement Methods =====
    
    public Queue<int[]> getWalkQueue() {
        return walkQueue;
    }
    
    public void addToWalkQueue(int deltaX, int deltaY) {
        walkQueue.offer(new int[] {deltaX, deltaY});
    }
    
    public void clearWalkQueue() {
        walkQueue.clear();
        isWalking = false;
    }
    
    public boolean isWalking() {
        return isWalking;
    }
    
    public void setWalking(boolean walking) {
        this.isWalking = walking;
    }
    
    public boolean hasWalkSteps() {
        return !walkQueue.isEmpty();
    }
    
    // ===== Multiplayer Tracking Methods =====
    // Based on server-js LocalEntities pattern
    
    public Set<Player> getKnownPlayers() {
        return knownPlayers;
    }
    
    public Set<Player> getAddedPlayers() {
        return addedPlayers;
    }
    
    public Set<Player> getRemovedPlayers() {
        return removedPlayers;
    }
    
    public Set<Player> getMovedPlayers() {
        return movedPlayers;
    }
    
    public Set<Npc> getKnownNpcs() {
        return knownNpcs;
    }
    
    public Set<Npc> getAddedNpcs() {
        return addedNpcs;
    }
    
    public Set<Npc> getRemovedNpcs() {
        return removedNpcs;
    }
    
    public int getDirection() {
        return direction;
    }
    
    public void setDirection(int direction) {
        this.direction = direction;
    }
}
