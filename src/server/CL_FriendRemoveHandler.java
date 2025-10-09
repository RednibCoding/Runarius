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
            Player player = findPlayerBySocket(socket);
            if (player == null) {
                Logger.error("Friend remove: player not found for socket");
                return;
            }
            
            // Remove from friend list
            player.getFriendList().remove(usernameHash);
            
            // Send updated friend list
            sendFriendList(player);
            
            Logger.info(player.getUsername() + " removed friend: " + usernameHash);
            
        } catch (IOException ex) {
            Logger.error("Friend remove error: " + ex.getMessage());
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
}
