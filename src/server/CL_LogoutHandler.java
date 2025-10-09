import java.io.IOException;
import java.net.Socket;

/**
 * Handles player logout requests.
 */
public class CL_LogoutHandler implements IPacketHandler {
    @Override
    public void handle(Socket socket, Buffer data) {
        try {
            // Find the player by socket
            Player player = findPlayerBySocket(socket);
            if (player == null) {
                Logger.error("Logout: player not found for socket");
                return;
            }
            
            Logger.info(player.getUsername() + " is logging out");
            
            // Remove player from world
            GameWorld.getInstance().removePlayer(player);
            
            // TODO: Save player data
            
            // Close the connection
            socket.close();
            
        } catch (Exception ex) {
            Logger.error("Logout error: " + ex.getMessage());
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
