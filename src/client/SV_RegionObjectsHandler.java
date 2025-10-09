import java.net.Socket;

/**
 * Handles region objects from server.
 */
public class SV_RegionObjectsHandler implements IClientPacketHandler {
    @Override
    public void handle(GameConnection client, Socket socket, Buffer data) {
        short objectCount = data.getShort();
        
        Logger.debug("Region objects: count=" + objectCount);
        
        // TODO: Parse and render objects
        for (int i = 0; i < objectCount; i++) {
            // Parse object data
        }
    }
}
