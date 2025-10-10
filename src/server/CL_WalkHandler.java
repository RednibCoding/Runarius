import java.io.IOException;
import java.net.Socket;

/**
 * Handles player walking packets (CL_WALK and CL_WALK_ACTION).
 * 
 * Packet format:
 * [2 bytes] Target X (absolute world coordinate)
 * [2 bytes] Target Y (absolute world coordinate)
 * [N pairs] Walk path steps (byte pairs: deltaX, deltaY)
 */
public class CL_WalkHandler implements IPacketHandler {
    @Override
    public void handle(Socket socket, Buffer data) {
        try {
            // Read target coordinates
            short targetX = data.getShort();
            short targetY = data.getShort();
            
            Logger.debug("Walk packet: targetX=" + targetX + ", targetY=" + targetY);
            
            // Get player by socket
            Player player = GameWorld.getInstance().getPlayerBySocket(socket);
            if (player == null) {
                Logger.error("Walk packet from unknown player");
                return;
            }
            
            // Read walk path (remaining bytes are delta X,Y pairs)
            int remainingBytes = data.remaining();
            int stepCount = remainingBytes / 2;
            
            Logger.debug("Walk path has " + stepCount + " steps");
            
            // For now, just move player to target position
            // TODO: Implement proper pathfinding and step-by-step movement
            player.setX(targetX);
            player.setY(targetY);
            
            Logger.info("Player " + player.getUsername() + " moved to (" + targetX + ", " + targetY + ")");
            
            // TODO: Broadcast movement to other players
            // GameWorld.getInstance().broadcastPlayerMovement(player);
            
        } catch (Exception ex) {
            Logger.error("Error handling walk packet: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
