import java.io.IOException;
import java.net.Socket;
import java.util.Set;

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
            
            // Then process the path steps from the client
            // Each step's delta is CUMULATIVE from the start position (targetX + deltaX)
            // We need to create smooth steps from current position to each waypoint
            for (int i = 0; i < stepCount; i++) {
                byte deltaX = data.getByte();
                byte deltaY = data.getByte();
                Logger.debug("  Raw step " + i + ": deltaX=" + deltaX + " (0x" + 
                           String.format("%02X", deltaX) + "), deltaY=" + deltaY + 
                           " (0x" + String.format("%02X", deltaY) + ")");
                
                // Calculate target position for this waypoint (cumulative delta from start)
                int waypointX = startX + deltaX;
                int waypointY = startY + deltaY;
                
                Logger.debug("  Waypoint " + i + ": (" + waypointX + "," + waypointY + ")");
                
                // Create smooth steps from current position to this waypoint
                createStraightLineSteps(player, currentX, currentY, waypointX, waypointY);
                
                // Update current position for next waypoint
                currentX = waypointX;
                currentY = waypointY;
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
    
    /**
     * Send multiplayer region players update using OLD bit-packed format.
     * Based on server-js LocalEntities.sendRegionPlayers() pattern.
     * 
     * Called after processing all player ticks to send updates about:
     * - Known players that moved
     * - Players leaving viewport
     * - New players entering viewport
     * 
     * Packet structure (bit-packed):
     * - Local player: regionX (11), regionY (13), animation (4)
     * - Known player count (8)
     * - For each known player:
     *   - reqUpdate (1) - has update?
     *   - If yes: updateType (1) - 0=moved, 1=removing
     *   - If moved: direction (3)
     * - New players added at end (while space remains):
     *   - serverIndex (11), offsetX (5), offsetY (5), animation (4)
     */
    public static void sendRegionPlayersUpdate(Player player) throws IOException {
        Logger.info("=== sendRegionPlayersUpdate called for " + player.getUsername() + " ===");
        
        // Get tracking sets
        Set<Player> knownPlayers = player.getKnownPlayers();
        Set<Player> addedPlayers = player.getAddedPlayers();
        Set<Player> movedPlayers = player.getMovedPlayers();
        Set<Player> removedPlayers = player.getRemovedPlayers();
        
        Logger.info(">>> Sending SV_REGION_PLAYERS to " + player.getUsername() + ":");
        Logger.info("    " + player.getUsername() + ": known=" + knownPlayers.size() + 
                    ", added=" + addedPlayers.size() + ", moved=" + movedPlayers.size() + 
                    ", removed=" + removedPlayers.size());
        
        // Build bit-packed packet
        Buffer out = new Buffer();
        out.putShort(Opcodes.Server.SV_REGION_PLAYERS.value);
        
        // Allocate buffer for bit-packed data (client has 500 byte max)
        // CRITICAL: GameConnection adds opcode byte at pdata[0]
        // Client reads starting at bit 8 (pdata[1], bit 0)
        // So we write starting at bit 0 here, which becomes bit 8 after opcode insertion
        byte[] bitData = new byte[500];
        int bitOffset = 0;  // Start at bit 0 - will become bit 8 in pdata after opcode byte added
        
        int localX = player.getX();
        int localY = player.getY();
        int localDir = player.getDirection();
        
        // Local player position (11 + 13 + 4 = 28 bits)
        NetHelper.setBitMask(bitData, bitOffset, 11, localX);
        bitOffset += 11;
        NetHelper.setBitMask(bitData, bitOffset, 13, localY);
        bitOffset += 13;
        NetHelper.setBitMask(bitData, bitOffset, 4, localDir);
        bitOffset += 4;
        
        // Known player count (8 bits)
        NetHelper.setBitMask(bitData, bitOffset, 8, knownPlayers.size());
        bitOffset += 8;
        
        Logger.debug("  Local player: X=" + localX + ", Y=" + localY + ", dir=" + localDir);
        Logger.debug("  Known player count: " + knownPlayers.size());
        
        // Process known players (moved or removed)
        for (Player knownPlayer : knownPlayers) {
            boolean hasMoved = movedPlayers.contains(knownPlayer);
            boolean isRemoving = removedPlayers.contains(knownPlayer);
            
            if (hasMoved || isRemoving) {
                // reqUpdate = 1 (has update)
                NetHelper.setBitMask(bitData, bitOffset, 1, 1);
                bitOffset += 1;
                
                if (isRemoving) {
                    // updateType = 1 (removing)
                    NetHelper.setBitMask(bitData, bitOffset, 1, 1);
                    bitOffset += 1;
                    Logger.debug("  Known player REMOVING: " + knownPlayer.getUsername());
                } else {
                    // updateType = 0 (moved)
                    NetHelper.setBitMask(bitData, bitOffset, 1, 0);
                    bitOffset += 1;
                    
                    // Direction (3 bits)
                    int dir = knownPlayer.getDirection();
                    NetHelper.setBitMask(bitData, bitOffset, 3, dir);
                    bitOffset += 3;
                    Logger.debug("  Known player MOVED: " + knownPlayer.getUsername() + " dir=" + dir);
                }
            } else {
                // reqUpdate = 0 (no change)
                NetHelper.setBitMask(bitData, bitOffset, 1, 0);
                bitOffset += 1;
            }
        }
        
        // Add new players
        Logger.info("  Adding " + addedPlayers.size() + " new players to packet");
        Logger.info("  BEFORE LOOP: addedPlayers.size() = " + addedPlayers.size());
        int loopCount = 0;
        for (Player newPlayer : addedPlayers) {
            loopCount++;
            Logger.info("  LOOP ITERATION " + loopCount + ": Processing " + newPlayer.getUsername());
            
            // Check if we have space (need 26 bits per new player)
            if ((bitOffset + 26) / 8 >= 500) {
                Logger.warn("  Out of space for new players!");
                break;
            }
            
            // serverIndex (11 bits) - player's index in server array
            int serverIndex = newPlayer.getServerId();
            NetHelper.setBitMask(bitData, bitOffset, 11, serverIndex);
            bitOffset += 11;
            
            // Offset from local player (-16 to +15 range)
            int offsetX = newPlayer.getX() - localX;
            int offsetY = newPlayer.getY() - localY;
            
            Logger.info("    NEW player " + newPlayer.getUsername() + ":");
            Logger.info("      serverIndex=" + serverIndex);
            Logger.info("      position: (" + newPlayer.getX() + ", " + newPlayer.getY() + ")");
            Logger.info("      local position: (" + localX + ", " + localY + ")");
            Logger.info("      raw offset: (" + offsetX + ", " + offsetY + ")");
            
            // Encode as 5-bit signed values
            if (offsetX < 0) offsetX += 32;
            if (offsetY < 0) offsetY += 32;
            
            Logger.info("      encoded offset: (" + offsetX + ", " + offsetY + ")");
            
            NetHelper.setBitMask(bitData, bitOffset, 5, offsetX);
            bitOffset += 5;
            NetHelper.setBitMask(bitData, bitOffset, 5, offsetY);
            bitOffset += 5;
            
            // Animation (4 bits)
            int anim = newPlayer.getDirection();
            NetHelper.setBitMask(bitData, bitOffset, 4, anim);
            bitOffset += 4;
            
            // Flag bit (always 0 for now)
            NetHelper.setBitMask(bitData, bitOffset, 1, 0);
            bitOffset += 1;
            
            Logger.info("      anim=" + anim + ", total bits used=" + bitOffset);
        }
        
        // Calculate how many bytes we actually used
        int byteCount = (bitOffset + 7) / 8;
        Logger.info("  Packet size: " + byteCount + " bytes (" + bitOffset + " bits)");
        
        // Write the bit-packed data (need to copy subset of array)
        byte[] packetData = new byte[byteCount];
        System.arraycopy(bitData, 0, packetData, 0, byteCount);
        
        // Debug: Print hex dump of packet data
        StringBuilder hexDump = new StringBuilder("  Packet hex dump: ");
        for (int i = 0; i < Math.min(byteCount, 20); i++) {
            hexDump.append(String.format("%02X ", packetData[i]));
        }
        if (byteCount > 20) hexDump.append("...");
        Logger.info(hexDump.toString());
        
        out.putBytes(packetData);
        
        // Send packet
        player.getSocket().getOutputStream().write(out.toArrayWithLen());
        player.getSocket().getOutputStream().flush();

        // After informing about new players, send their appearance details
        for (Player newPlayer : addedPlayers) {
            PlayerPacketSender.sendAppearance(player, newPlayer);
        }
        
        // Update tracking sets for next tick
        // Move added players to known
        knownPlayers.addAll(addedPlayers);
        
        // Remove removed players from known
        knownPlayers.removeAll(removedPlayers);
        
        // Clear all tracking sets
        addedPlayers.clear();
        movedPlayers.clear();
        removedPlayers.clear();
        
        Logger.info("  Update complete!");
    }
}
