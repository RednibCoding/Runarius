import java.net.Socket;

/**
 * Handles player logout requests.
 */
public class CL_LogoutHandler implements IPacketHandler {
    @Override
    public void handle(Socket socket, Buffer data) {
        try {
            // Find the player by socket
            ServerContext context = ServerContext.get();
            Player player = context.getPlayers().findBySocket(socket).orElse(null);
            if (player == null) {
                Logger.error("Logout: player not found for socket");
                return;
            }
            
            Logger.info(player.getUsername() + " is logging out");
            
            context.getVisibilityService().handlePlayerRemoval(player);
            context.getPlayers().removePlayer(player);
            
            // TODO: Save player data
            
            // Close the connection
            socket.close();
            
        } catch (Exception ex) {
            Logger.error("Logout error: " + ex.getMessage());
        }
    }
    
}
