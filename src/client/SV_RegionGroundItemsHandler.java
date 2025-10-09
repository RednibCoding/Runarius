import java.net.Socket;

/**
 * Handles region ground items from server.
 */
public class SV_RegionGroundItemsHandler implements IClientPacketHandler {
    @Override
    public void handle(GameConnection client, Socket socket, Buffer data) {
        short itemCount = data.getShort();
        
        Logger.debug("Region ground items: count=" + itemCount);
        
        // TODO: Parse and render ground items
        for (int i = 0; i < itemCount; i++) {
            // Parse ground item data
        }
    }
}
