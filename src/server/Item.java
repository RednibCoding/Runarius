/**
 * Represents an item in the game.
 * Can be in player inventory, bank, ground, or shop.
 */
public class Item {
    private int id;
    private int amount;
    private boolean equipped;
    
    public Item(int id, int amount) {
        this.id = id;
        this.amount = amount;
        this.equipped = false;
    }
    
    public Item(int id, int amount, boolean equipped) {
        this.id = id;
        this.amount = amount;
        this.equipped = equipped;
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
    
    public boolean isEquipped() {
        return equipped;
    }
    
    public void setEquipped(boolean equipped) {
        this.equipped = equipped;
    }
    
    /**
     * Check if this item is stackable.
     * Uses loaded item definitions from WorldService.
     */
    public boolean isStackable() {
        try {
            WorldService world = ServerContext.get().getWorldService();
            ItemDefinition def = world.getItemDefinition(id);
            if (def != null) {
                return def.isStackable();
            }
        } catch (Exception ex) {
            // Fall through to hardcoded defaults if context not available
        }
        // Fallback for pre-initialization or unknown items
        return id == 10 || id == 31 || id == 32 || id == 33;
    }
}
