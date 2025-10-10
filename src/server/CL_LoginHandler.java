import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;

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
            
            // Detect nearby players (based on server-js login flow)
            // This populates the addedPlayers set for the first region packet
            updateNearbyPlayers(player);
            
            Logger.info("Login: " + player.getUsername() + " has " + 
                       player.getAddedPlayers().size() + " nearby players");
            
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
            
            // Send player appearance to themselves
            PlayerPacketSender.sendAppearance(player, player);

            // Send existing nearby players' appearance to the new player
            for (Player nearby : player.getAddedPlayers()) {
                PlayerPacketSender.sendAppearance(player, nearby);
            }

            // Inform nearby players about the new player's appearance
            for (Player nearby : player.getAddedPlayers()) {
                PlayerPacketSender.sendAppearance(nearby, player);
            }
            
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
        // Use the full multiplayer-aware packet sender
        CL_WalkHandler.sendRegionPlayersUpdate(player);
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
    
    /**
     * Update nearby players during login (based on server-js updateNearby).
     * Detects all players currently in range and adds them to addedPlayers set.
     */
    private void updateNearbyPlayers(Player player) {
        GameWorld world = GameWorld.getInstance();
        
        // Find all nearby players
        for (Player nearbyPlayer : world.getNearbyPlayers(player.getX(), player.getY(), 16)) {
            if (nearbyPlayer != player) {
                // Add to this player's added set
                player.getAddedPlayers().add(nearbyPlayer);
                
                // ALSO add this player to the nearby player's added set
                // (mutual visibility)
                nearbyPlayer.getAddedPlayers().add(player);
                
                Logger.info("Login: Mutual add " + player.getUsername() + " <-> " + nearbyPlayer.getUsername());
            }
        }
    }
}
