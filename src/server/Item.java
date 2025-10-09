/**
 * Represents an item in the game.
 * Can be in player inventory, bank, ground, or shop.
 */
public class Item {
    private int id;
    private int amount;
    
    public Item(int id, int amount) {
        this.id = id;
        this.amount = amount;
    }
    
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public int getAmount() {
        return amount;
    }
    
    public void setAmount(int amount) {
        this.amount = amount;
    }
    
    /**
     * Check if this item is stackable.
     * In RSC, items like arrows, runes, coins are stackable.
     * This is a placeholder - would need actual item definitions.
     */
    public boolean isStackable() {
        // TODO: Load from item definitions
        // For now, assume coins (id 10) and some common stackable items
        return id == 10 || id == 31 || id == 32 || id == 33; // coins, arrows, etc
    }
}
