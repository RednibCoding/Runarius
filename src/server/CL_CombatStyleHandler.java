import java.io.IOException;
import java.net.Socket;

/**
 * Handles player combat style changes.
 */
public class CL_CombatStyleHandler implements IPacketHandler {
    @Override
    public void handle(Socket socket, Buffer data) {
        try {
            byte style = data.getByte();
            
            // Find the player by socket
            Player player = findPlayerBySocket(socket);
            if (player == null) {
                Logger.error("Combat style: player not found for socket");
                return;
            }
            
            player.setCombatStyle(style);
            
            Logger.info(player.getUsername() + " changed combat style to: " + style);
            
        } catch (Exception ex) {
            Logger.error("Combat style error: " + ex.getMessage());
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
