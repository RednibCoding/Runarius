import java.io.IOException;
import java.net.Socket;

/**
 * Handles when a player removes a friend from their friend list.
 */
public class CL_FriendRemoveHandler implements IPacketHandler {
    @Override
    public void handle(Socket socket, Buffer data) {
        try {
            long usernameHash = data.getLong();
            
            // Find the player by socket
            ServerContext context = ServerContext.get();
            PlayerRepository players = context.getPlayers();
            Player player = players.findBySocket(socket).orElse(null);
            if (player == null) {
                Logger.error("Friend remove: player not found for socket");
                return;
            }
            
            // Remove from friend list
            player.getFriendList().remove(usernameHash);
            
            // Send updated friend list
            sendFriendList(context, player);
            
            Logger.info(player.getUsername() + " removed friend: " + usernameHash);
            
        } catch (IOException ex) {
            Logger.error("Friend remove error: " + ex.getMessage());
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
}
