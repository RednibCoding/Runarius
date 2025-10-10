import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class CL_LoginHandler implements IPacketHandler {
    @Override
    public void handle(Socket socket, Buffer data) {
        try {
            InputStream inStream = socket.getInputStream();
            OutputStream outStream = socket.getOutputStream();
            
            int clientVersion = data.getInt();
            long sessionId = data.getLong();
            String username = data.getString();
            String password = data.getString();

            Logger.info("Login attempt: " + username + " (session: " + sessionId + ", version: " + clientVersion + ")");

            // TODO: Validate credentials against database
            // For now, accept all logins
            
            // Get pending player by session ID
            Player player = GameWorld.getInstance().getPendingPlayer(sessionId);
            
            if (player == null) {
                Logger.error("No pending session found for " + username + " (session: " + sessionId + ")");
                
                Buffer out = new Buffer();
                out.putInt(LoginResponse.SERVER_REJECTED_SESSION.getCode());
                outStream.write(out.toArray());
                outStream.flush();
                return;
            }
            
            Logger.info("Found pending player: " + player.getUsername());
            
            // Add player to world
            GameWorld.getInstance().addPlayer(player);
            
            // Send success response
            Buffer out = new Buffer();
            out.putInt(LoginResponse.SUCCESS.getCode());
            outStream.write(out.toArray());
            outStream.flush();
            
            // Send world info
            sendWorldInfo(player);
            
            // Send player stats
            sendPlayerStats(player);
            
            // Send inventory
            sendInventory(player);
            
            // Send friend list
            sendFriendList(player);
            
            // Send ignore list
            sendIgnoreList(player);
            
            // Send privacy settings
            sendPrivacySettings(player);
            
            // Send region data (required for client to render world)
            sendRegionPlayers(player);
            sendRegionObjects(player);
            sendRegionWallObjects(player);
            sendRegionGroundItems(player);
            sendRegionNPCs(player);
            
            // Send player appearance with detailed debugging
            sendPlayerAppearanceUpdate(player);
            
            Logger.info("Player " + username + " logged in successfully");
            
        } catch (IOException ex) {
            Logger.error("Login error: " + ex.getMessage());
        }
    }
    
    private void sendWorldInfo(Player player) throws IOException {
        Buffer out = new Buffer();
        out.putShort(Opcodes.Server.SV_WORLD_INFO.value);
        out.putShort((short) player.getServerId());
        out.putShort((short) GameWorld.getInstance().getPlaneWidth());
        out.putShort((short) GameWorld.getInstance().getPlaneHeight());
        out.putShort((short) player.getPlaneIndex());
        out.putShort((short) GameWorld.getInstance().getPlaneMultiplier());
        
        player.getSocket().getOutputStream().write(out.toArrayWithLen());
        player.getSocket().getOutputStream().flush();
    }
    
    private void sendPlayerStats(Player player) throws IOException {
        Buffer out = new Buffer();
        out.putShort(Opcodes.Server.SV_PLAYER_STAT_LIST.value);
        
        // Current stats
        for (int i = 0; i < 18; i++) {
            out.putByte((byte) player.getCurrentStats()[i]);
        }
        
        // Base stats
        for (int i = 0; i < 18; i++) {
            out.putByte((byte) player.getBaseStats()[i]);
        }
        
        // Experience
        for (int i = 0; i < 18; i++) {
            out.putInt(player.getExperience()[i]);
        }
        
        // Quest points
        out.putByte((byte) player.getQuestPoints());
        
        player.getSocket().getOutputStream().write(out.toArrayWithLen());
        player.getSocket().getOutputStream().flush();
    }
    
    private void sendInventory(Player player) throws IOException {
        Buffer out = new Buffer();
        out.putShort(Opcodes.Server.SV_INVENTORY_ITEMS.value);
        out.putByte((byte) player.getInventory().size());
        
        for (Item item : player.getInventory()) {
            out.putShort((short) item.getId());
            if (item.isStackable()) {
                out.putInt(item.getAmount());
            }
        }
        
        player.getSocket().getOutputStream().write(out.toArrayWithLen());
        player.getSocket().getOutputStream().flush();
    }
    
    private void sendFriendList(Player player) throws IOException {
        Buffer out = new Buffer();
        out.putShort(Opcodes.Server.SV_FRIEND_LIST.value);
        out.putByte((byte) player.getFriendList().size());
        
        for (long friendHash : player.getFriendList()) {
            out.putLong(friendHash);
            out.putByte((byte) (GameWorld.getInstance().isPlayerOnline(friendHash) ? 1 : 0));
        }
        
        player.getSocket().getOutputStream().write(out.toArrayWithLen());
        player.getSocket().getOutputStream().flush();
    }
    
    private void sendIgnoreList(Player player) throws IOException {
        Buffer out = new Buffer();
        out.putShort(Opcodes.Server.SV_IGNORE_LIST.value);
        out.putByte((byte) player.getIgnoreList().size());
        
        for (long ignoreHash : player.getIgnoreList()) {
            out.putLong(ignoreHash);
        }
        
        player.getSocket().getOutputStream().write(out.toArrayWithLen());
        player.getSocket().getOutputStream().flush();
    }
    
    private void sendPrivacySettings(Player player) throws IOException {
        Buffer out = new Buffer();
        out.putShort(Opcodes.Server.SV_PRIVACY_SETTINGS.value);
        out.putByte((byte) (player.isBlockChat() ? 1 : 0));
        out.putByte((byte) (player.isBlockPrivateMessages() ? 1 : 0));
        out.putByte((byte) (player.isBlockTrade() ? 1 : 0));
        out.putByte((byte) (player.isBlockDuel() ? 1 : 0));
        
        player.getSocket().getOutputStream().write(out.toArrayWithLen());
        player.getSocket().getOutputStream().flush();
    }
    
    private void sendRegionPlayers(Player player) throws IOException {
        // For now, send empty player region data
        // TODO: Send actual nearby players when implemented
        Buffer out = new Buffer();
        out.putShort(Opcodes.Server.SV_REGION_PLAYERS.value);
        
        // This packet uses bit-packed data
        // Client expects pdata[0] = opcode, then reads bit-packed data starting at bit 8 (which is pdata[1])
        // So we need: regionX (11 bits), regionY (13 bits), anim (4 bits), playerCount (8 bits)
        // Total: 11 + 13 + 4 + 8 = 36 bits = 5 bytes (rounded up)
        
        // Create bit buffer (5 bytes to hold data starting at bit 0)
        byte[] bitData = new byte[5];
        int bitOffset = 0; // Start at bit 0 of our buffer
        
        // Region X (11 bits) - player's X coordinate
        int regionX = player.getX();
        NetHelper.setBitMask(bitData, bitOffset, 11, regionX);
        bitOffset += 11;
        
        // Region Y (13 bits) - player's Y coordinate  
        int regionY = player.getY();
        NetHelper.setBitMask(bitData, bitOffset, 13, regionY);
        bitOffset += 13;
        
        // Animation (4 bits) - 0 = standing
        NetHelper.setBitMask(bitData, bitOffset, 4, 0);
        bitOffset += 4;
        
        // Known player count (8 bits) - no other players for now
        NetHelper.setBitMask(bitData, bitOffset, 8, 0);
        
        // Write the bit-packed data
        out.putBytes(bitData);
        
        player.getSocket().getOutputStream().write(out.toArrayWithLen());
        player.getSocket().getOutputStream().flush();
        Logger.debug("Sent region players data for position " + regionX + ", " + regionY);
    }
    
    private void sendRegionObjects(Player player) throws IOException {
        // Send empty objects list for now
        Buffer out = new Buffer();
        out.putShort(Opcodes.Server.SV_REGION_OBJECTS.value);
        out.putShort((short) 0); // No objects
        
        player.getSocket().getOutputStream().write(out.toArrayWithLen());
        player.getSocket().getOutputStream().flush();
        Logger.debug("Sent region objects data");
    }
    
    private void sendRegionWallObjects(Player player) throws IOException {
        // Send empty wall objects list for now
        Buffer out = new Buffer();
        out.putShort(Opcodes.Server.SV_REGION_WALL_OBJECTS.value);
        out.putShort((short) 0); // No wall objects
        
        player.getSocket().getOutputStream().write(out.toArrayWithLen());
        player.getSocket().getOutputStream().flush();
        Logger.debug("Sent region wall objects data");
    }
    
    private void sendRegionGroundItems(Player player) throws IOException {
        // Send empty ground items list for now
        Buffer out = new Buffer();
        out.putShort(Opcodes.Server.SV_REGION_GROUND_ITEMS.value);
        out.putShort((short) 0); // No ground items
        
        player.getSocket().getOutputStream().write(out.toArrayWithLen());
        player.getSocket().getOutputStream().flush();
        Logger.debug("Sent region ground items data");
    }
    
    private void sendRegionNPCs(Player player) throws IOException {
        // Send empty NPC list for now
        Buffer out = new Buffer();
        out.putShort(Opcodes.Server.SV_REGION_NPCS.value);
        out.putShort((short) 0); // No NPCs
        
        player.getSocket().getOutputStream().write(out.toArrayWithLen());
        player.getSocket().getOutputStream().flush();
        Logger.debug("Sent region NPCs data");
    }
    
    private void sendPlayerAppearanceUpdate(Player player) throws IOException {
        // Send SV_REGION_PLAYER_UPDATE with updateType 5 (appearance)
        // This sends the player's appearance to themselves so they can see their character
        Buffer out = new Buffer();
        out.putShort(Opcodes.Server.SV_REGION_PLAYER_UPDATE.value);
        
        // Update size - we're sending 1 player update (our own player)
        out.putShort((short) 1);
        
        // Player server index - use the player's server ID
        out.putShort((short) player.getServerId());
        
        // Update type 5 = appearance
        out.putByte((byte) 5);
        
        // Server ID (again, as per protocol)
        out.putShort((short) player.getServerId());
        
        // Username hash
        out.putLong(player.getUsernameHash());
        
        // Equipped items count - RSC always sends 12 slots for player appearance
        // Slots represent different body parts and equipment
        // For a "naked" player, we send default body part sprite IDs
        out.putByte((byte) 12);
        
        // Build the 12 equipment slots
        // Slots 0-11 map to different body parts/equipment pieces
        // For now, send basic male body parts based on headGender/headType/bodyGender
        int[] equippedItems = new int[12];
        
        // Slot 0: unused or cape (0 = nothing)
        equippedItems[0] = 0;
        
        // Slot 1: Hair/head - use headType (1 for male head style 1)
        equippedItems[1] = player.getHeadType();
        
        // Slot 2: Body (torso+arms) - use bodyGender (2 for male body)
        equippedItems[2] = player.getBodyGender() + 1; // Adding 1 because sprites start at 1
        
        // Slot 3: Legs - use bodyGender + 2 for matching legs (3 for male legs)
        equippedItems[3] = player.getBodyGender() + 2;
        
        // Slots 4-11: Other equipment (0 = nothing equipped)
        for (int i = 4; i < 12; i++) {
            equippedItems[i] = 0;
        }
        
        // Write all 12 equipment slots
        for (int i = 0; i < 12; i++) {
            out.putByte((byte) equippedItems[i]);
        }
        
        // Appearance colors
        out.putByte((byte) player.getHairColor());      // Hair color
        out.putByte((byte) player.getTopColor());       // Top color
        out.putByte((byte) player.getBottomColor());    // Bottom color  
        out.putByte((byte) player.getSkinColor());      // Skin color
        
        // Combat level
        out.putByte((byte) player.getCombatLevel());
        
        // Skull visible (0 = no skull, 1 = skull visible for PKing)
        out.putByte((byte) 0);
        
        player.getSocket().getOutputStream().write(out.toArrayWithLen());
        player.getSocket().getOutputStream().flush();
    }
}
