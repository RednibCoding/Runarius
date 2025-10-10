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
            // Read starting coordinates (where the player currently is according to client)
            short startX = data.getShort();
            short startY = data.getShort();
            
            // Get player by socket
            Player player = GameWorld.getInstance().getPlayerBySocket(socket);
            if (player == null) {
                Logger.error("Walk packet from unknown player");
                return;
            }
            
            // Read walk path (remaining bytes are delta X,Y pairs)
            int remainingBytes = data.remaining();
            int stepCount = remainingBytes / 2;
            
            Logger.info("Walk request from " + player.getUsername() + ": " +
                       "client_pos=(" + startX + "," + startY + "), " +
                       "server_pos=(" + player.getX() + "," + player.getY() + "), " +
                       "steps=" + stepCount);
            
            // Clear any existing walk queue (new walk command cancels previous)
            player.clearWalkQueue();
            
            // First, create steps from server's current position to client's start position
            // This handles any desync between client and server
            int currentX = player.getX();
            int currentY = player.getY();
            
            if (currentX != startX || currentY != startY) {
                Logger.debug("Position desync detected - creating intermediate steps");
                createStraightLineSteps(player, currentX, currentY, startX, startY);
                currentX = startX;
                currentY = startY;
            }
            
            // Then add the path steps from the client
            for (int i = 0; i < stepCount; i++) {
                byte deltaX = data.getByte();
                byte deltaY = data.getByte();
                player.addToWalkQueue(deltaX, deltaY);
                Logger.debug("  Step " + i + ": delta=(" + deltaX + "," + deltaY + ")");
            }
            
            Logger.info("Queued " + player.getWalkQueue().size() + " total walk steps for " + player.getUsername());
            
            // Note: Actual movement will be processed by game tick system
            // Each tick will process one step from the queue and send position updates
            
        } catch (Exception ex) {
            Logger.error("Error handling walk packet: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    /**
     * Create straight-line walk steps from start to end position.
     * This handles client-server position desync by creating intermediate steps.
     * 
     * Based on server-js createSteps() function - can move in straight lines
     * (only X or Y changing) or perfect diagonals (X and Y changing by same amount).
     */
    private void createStraightLineSteps(Player player, int startX, int startY, int endX, int endY) {
        int totalSteps = Math.abs(endX - startX) + Math.abs(endY - startY);
        
        if (totalSteps == 0) {
            return; // Already at destination
        }
        
        int currentSteps = 0;
        
        // Determine movement direction (-1, 0, or 1)
        int deltaX = startX < endX ? 1 : (startX > endX ? -1 : 0);
        int deltaY = startY < endY ? 1 : (startY > endY ? -1 : 0);
        
        // Check if this is a perfectly diagonal path
        if (Math.abs(endX - startX) != Math.abs(endY - startY)) {
            // Not diagonal - move in only one direction
            if (endX - startX != 0) {
                deltaY = 0; // Moving in X direction only
            } else {
                deltaX = 0; // Moving in Y direction only
            }
        }
        
        // Add steps to walk queue
        while (currentSteps < totalSteps) {
            player.addToWalkQueue(deltaX, deltaY);
            currentSteps += Math.abs(deltaX) + Math.abs(deltaY);
        }
        
        Logger.debug("Created " + (currentSteps / (Math.abs(deltaX) + Math.abs(deltaY))) + 
                    " steps from (" + startX + "," + startY + ") to (" + endX + "," + endY + ")");
    }
    
    /**
     * Send updated position to player using SV_REGION_PLAYERS packet.
     * Called by the game tick system after each movement step.
     */
    public static void sendPositionUpdate(Player player) throws IOException {
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
