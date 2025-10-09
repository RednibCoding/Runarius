import java.net.Socket;

/**
 * Handles region NPCs from server.
 */
public class SV_RegionNPCsHandler implements IClientPacketHandler {
    @Override
    public void handle(GameConnection client, Socket socket, Buffer data) {
        short npcCount = data.getShort();
        
        Logger.debug("Region NPCs: count=" + npcCount);
        
        // TODO: Parse and render NPCs
        for (int i = 0; i < npcCount; i++) {
            // Parse NPC data
        }
    }
}
