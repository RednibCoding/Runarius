import java.io.IOException;
import java.net.Socket;

/**
 * Handles when a player adds a friend to their friend list.
 */
public class CL_FriendAddHandler implements IPacketHandler {
    @Override
    public void handle(Socket socket, Buffer data) {
        try {
            long usernameHash = data.getLong();
            
            // Find the player by socket
            ServerContext context = ServerContext.get();
            PlayerRepository players = context.getPlayers();
            Player player = players.findBySocket(socket).orElse(null);
            if (player == null) {
                Logger.error("Friend add: player not found for socket");
                return;
            }
            
            // Check if already in friend list
            if (player.getFriendList().contains(usernameHash)) {
                sendMessage(player, "That player is already on your friend list");
                return;
            }
            
            // Add to friend list
            player.getFriendList().add(usernameHash);
            
            // Send updated friend list
            sendFriendList(context, player);
            
            Logger.info(player.getUsername() + " added friend: " + usernameHash);
            
        } catch (IOException ex) {
            Logger.error("Friend add error: " + ex.getMessage());
        }
    }
    
    private void sendFriendList(ServerContext context, Player player) throws IOException {
        Buffer out = new Buffer();
        out.putShort(Opcodes.Server.SV_FRIEND_LIST.value);
        out.putByte((byte) player.getFriendList().size());
        
        for (long friendHash : player.getFriendList()) {
            out.putLong(friendHash);
            out.putByte((byte) (context.getPlayers().isOnline(friendHash) ? 1 : 0));
        }
        
        player.getSocket().getOutputStream().write(out.toArrayWithLen());
        player.getSocket().getOutputStream().flush();
    }
    
    private void sendMessage(Player player, String message) throws IOException {
        Buffer out = new Buffer();
        out.putShort(Opcodes.Server.SV_MESSAGE.value);
        out.putString(message);
        
        player.getSocket().getOutputStream().write(out.toArrayWithLen());
        player.getSocket().getOutputStream().flush();
    }
}
