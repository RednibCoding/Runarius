import java.net.Socket;

/**
 * Handles region wall objects from server.
 */
public class SV_RegionWallObjectsHandler implements IClientPacketHandler {
    @Override
    public void handle(GameConnection client, Socket socket, Buffer data) {
        short wallObjectCount = data.getShort();
        
        Logger.debug("Region wall objects: count=" + wallObjectCount);
        
        // TODO: Parse and render wall objects
        for (int i = 0; i < wallObjectCount; i++) {
            // Parse wall object data
        }
    }
}
