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
            PlayerRepository players = ServerContext.get().getPlayers();
            Player player = players.findBySocket(socket).orElse(null);
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
    
}
