import java.net.Socket;

/**
 * Handles player chat messages.
 */
public class CL_ChatHandler implements IPacketHandler {
    @Override
    public void handle(Socket socket, Buffer data) {
        try {
            // Read remaining bytes as the message
            int messageLength = data.remaining();
            byte[] message = new byte[messageLength];
            for (int i = 0; i < messageLength; i++) {
                message[i] = data.getByte();
            }
            
            // Find the player by socket
            Player player = findPlayerBySocket(socket);
            if (player == null) {
                Logger.error("Chat: player not found for socket");
                return;
            }
            
            if (player.isBlockChat()) {
                return; // Player has chat blocked
            }
            
            Logger.info(player.getUsername() + " says: [" + message.length + " bytes]");
            
            // TODO: Broadcast to nearby players
            // For now, just log it
            
        } catch (Exception ex) {
            Logger.error("Chat error: " + ex.getMessage());
        }
    }
    
    private Player findPlayerBySocket(Socket socket) {
        for (Player p : GameWorld.getInstance().getAllPlayers()) {
            if (p.getSocket() == socket) {
                return p;
            }
        }
        return null;
    }
}
