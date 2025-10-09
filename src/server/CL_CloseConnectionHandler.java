import java.io.IOException;
import java.net.Socket;

/**
 * Handles when client closes connection.
 */
public class CL_CloseConnectionHandler implements IPacketHandler {
    @Override
    public void handle(Socket socket, Buffer data) {
        try {
            // Find the player by socket
            Player player = findPlayerBySocket(socket);
            if (player == null) {
                Logger.error("Close connection: player not found for socket");
                return;
            }
            
            Logger.info(player.getUsername() + " closed connection");
            
            // Remove player from world
            GameWorld.getInstance().removePlayer(player);
            
            // Close the socket
            socket.close();
            
        } catch (Exception ex) {
            Logger.error("Close connection error: " + ex.getMessage());
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
