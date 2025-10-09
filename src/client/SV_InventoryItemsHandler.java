import java.net.Socket;

/**
 * Handles inventory items sent from server on login or update.
 */
public class SV_InventoryItemsHandler implements IClientPacketHandler {
    @Override
    public void handle(GameConnection client, Socket socket, Buffer data) {
        byte itemCount = data.getByte();
        
        Logger.debug("Inventory: " + itemCount + " items");
        
        for (int i = 0; i < itemCount; i++) {
            short itemId = data.getShort();
            
            // Check if item is stackable (bit 15 set means stackable in RSC)
            boolean isStackable = (itemId & 0x8000) != 0;
            
            if (isStackable) {
                data.getInt(); // amount - skip for now
            }
            
            // TODO: Store items in client inventory
        }
    }
}
