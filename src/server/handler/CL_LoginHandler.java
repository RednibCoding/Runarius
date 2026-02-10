import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

public class CL_LoginHandler implements IPacketHandler {
    @Override
    public void handle(Socket socket, Buffer data) {
        try {
            ServerContext context = ServerContext.get();
            PlayerRepository players = context.getPlayers();
            WorldService world = context.getWorldService();
            VisibilityService visibility = context.getVisibilityService();

            OutputStream outStream = socket.getOutputStream();
            
            int clientVersion = data.getInt();
            long sessionId = data.getLong();
            String username = data.getString();
            data.getString(); // password - TODO: validate credentials

            Logger.info("Login attempt: " + username + " (session: " + sessionId + ", version: " + clientVersion + ")");

            // TODO: Validate credentials against database
            // For now, accept all logins
            
            Player player = players.consumePending(sessionId).orElse(null);
            
            if (player == null) {
                Logger.error("No pending session found for " + username + " (session: " + sessionId + ")");
                
                Buffer out = new Buffer();
                out.putInt(LoginResponse.SERVER_REJECTED_SESSION.getCode());
                outStream.write(out.toArray());
                outStream.flush();
                return;
            }
            
            Logger.info("Found pending player: " + player.getUsername());
            
            world.spawnPlayer(player);
            players.addPlayer(player);
            visibility.establishMutualVisibility(player);
            
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
    out.putShort((short) ServerContext.get().getWorldService().getWorldWidth());
    out.putShort((short) ServerContext.get().getWorldService().getWorldHeight());
        out.putShort((short) player.getPlaneIndex());
    out.putShort((short) ServerContext.get().getWorldService().getPlaneMultiplier());
        
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
            out.putByte((byte) (ServerContext.get().getPlayers().isOnline(friendHash) ? 1 : 0));
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
        PlayerPacketSender.sendRegionObjects(player);
    }
    
    private void sendRegionWallObjects(Player player) throws IOException {
        PlayerPacketSender.sendRegionWallObjects(player);
    }
    
    private void sendRegionGroundItems(Player player) throws IOException {
        PlayerPacketSender.sendRegionGroundItems(player);
    }
    
    private void sendRegionNPCs(Player player) throws IOException {
        PlayerPacketSender.sendRegionNpcs(player);
    }
    
}
