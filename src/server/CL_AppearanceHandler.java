import java.io.IOException;
import java.net.Socket;

/**
 * Handles player appearance changes.
 */
public class CL_AppearanceHandler implements IPacketHandler {
    @Override
    public void handle(Socket socket, Buffer data) {
        try {
            byte headGender = data.getByte();
            byte headType = data.getByte();
            byte bodyGender = data.getByte();
            byte unknown = data.getByte(); // 2colour in docs
            byte hairColour = data.getByte();
            byte topColour = data.getByte();
            byte bottomColour = data.getByte();
            byte skinColour = data.getByte();
            
            // Find the player by socket
            Player player = findPlayerBySocket(socket);
            if (player == null) {
                Logger.error("Appearance: player not found for socket");
                return;
            }
            
            player.setAppearance(headGender, headType, bodyGender, 
                               hairColour, topColour, bottomColour, skinColour);
            
            Logger.info(player.getUsername() + " changed appearance");
            
            for (Player viewer : GameWorld.getInstance().getAllPlayers()) {
                if (viewer == null) {
                    continue;
                }
                if (viewer == player || viewer.getKnownPlayers().contains(player) || viewer.getAddedPlayers().contains(player)) {
                    PlayerPacketSender.sendAppearance(viewer, player);
                }
            }
            
        } catch (Exception ex) {
            Logger.error("Appearance error: " + ex.getMessage());
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
