import java.io.IOException;
import java.net.Socket;

/**
 * Handles player walking packets (CL_WALK and CL_WALK_ACTION).
 * 
 * Packet format:
 * [2 bytes] Start X (absolute world coordinate - where player currently is)
 * [2 bytes] Start Y (absolute world coordinate - where player currently is)
 * [N pairs] Walk path steps (byte pairs: deltaX, deltaY relative to start)
 * 
 * The path contains the steps to follow from the start position.
 * Each step is encoded as two signed bytes (deltaX, deltaY).
 */
public class CL_WalkHandler implements IPacketHandler {
    @Override
    public void handle(Socket socket, Buffer data) {
        try {
            // Read starting coordinates (where the player currently is)
            short startX = data.getShort();
            short startY = data.getShort();
            
            // Get player by socket
            Player player = GameWorld.getInstance().getPlayerBySocket(socket);
            if (player == null) {
                Logger.error("Walk packet from unknown player");
                return;
            }
            
            int oldX = player.getX();
            int oldY = player.getY();
            
            // Read walk path (remaining bytes are delta X,Y pairs)
            int remainingBytes = data.remaining();
            int stepCount = remainingBytes / 2;
            
            // Calculate final destination by following the path
            int finalX = startX;
            int finalY = startY;
            
            if (stepCount > 0) {
                // Read the path steps
                for (int i = 0; i < stepCount; i++) {
                    byte deltaX = data.getByte();
                    byte deltaY = data.getByte();
                    finalX += deltaX;
                    finalY += deltaY;
                }
            }
            
            Logger.info("Walk request from " + player.getUsername() + ": " +
                       "start=(" + startX + "," + startY + ") -> " +
                       "final=(" + finalX + "," + finalY + ") " +
                       "steps=" + stepCount);
            
            // For now, just teleport player to final position
            // TODO: Implement proper step-by-step movement with animation
            player.setX((short) finalX);
            player.setY((short) finalY);
            
            Logger.info("Position updated: " +
                       "(" + oldX + "," + oldY + ") -> (" + player.getX() + "," + player.getY() + ")");
            
            // Send position update back to the player so they see themselves move
            sendPositionUpdate(player);
            
            // TODO: Broadcast movement to other nearby players
            // GameWorld.getInstance().broadcastPlayerMovement(player);
            
        } catch (Exception ex) {
            Logger.error("Error handling walk packet: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    /**
     * Send updated position to player using SV_REGION_PLAYERS packet.
     * This is the same packet sent during login, but with updated coordinates.
     * 
     * NOTE: This is a temporary "teleport" solution. Proper movement should use
     * incremental directional updates encoded in the SV_REGION_PLAYERS packet format.
     */
    private void sendPositionUpdate(Player player) throws IOException {
        Buffer out = new Buffer();
        out.putShort(Opcodes.Server.SV_REGION_PLAYERS.value);
        
        // This packet uses bit-packed data
        // Client expects pdata[0] = opcode, then reads bit-packed data starting at bit 8
        byte[] bitData = new byte[5];
        int bitOffset = 0;
        
        int newX = player.getX();
        int newY = player.getY();
        
        Logger.debug("Sending position update: X=" + newX + " Y=" + newY);
        
        // Region X (11 bits) - player's NEW X coordinate (ABSOLUTE world coordinate)
        NetHelper.setBitMask(bitData, bitOffset, 11, newX);
        bitOffset += 11;
        
        // Region Y (13 bits) - player's NEW Y coordinate (ABSOLUTE world coordinate)
        NetHelper.setBitMask(bitData, bitOffset, 13, newY);
        bitOffset += 13;
        
        // Animation (4 bits) - 0 = standing
        // TODO: Set to walking direction based on movement
        NetHelper.setBitMask(bitData, bitOffset, 4, 0);
        bitOffset += 4;
        
        // Known player count (8 bits) - 0 means just update local player position
        NetHelper.setBitMask(bitData, bitOffset, 8, 0);
        
        // Write the bit-packed data
        out.putBytes(bitData);
        
        player.getSocket().getOutputStream().write(out.toArrayWithLen());
        player.getSocket().getOutputStream().flush();
        
        Logger.info("Position update sent to " + player.getUsername() + ": (" + newX + ", " + newY + ")");
    }
}
