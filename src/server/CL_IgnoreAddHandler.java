import java.io.IOException;
import java.net.Socket;

/**
 * Handles when a player adds someone to their ignore list.
 */
public class CL_IgnoreAddHandler implements IPacketHandler {
    @Override
    public void handle(Socket socket, Buffer data) {
        try {
            long usernameHash = data.getLong();
            
            // Find the player by socket
            Player player = findPlayerBySocket(socket);
            if (player == null) {
                Logger.error("Ignore add: player not found for socket");
                return;
            }
            
            // Check if already in ignore list
            if (player.getIgnoreList().contains(usernameHash)) {
                sendMessage(player, "That player is already on your ignore list");
                return;
            }
            
            // Add to ignore list
            player.getIgnoreList().add(usernameHash);
            
            // Send updated ignore list
            sendIgnoreList(player);
            
            Logger.info(player.getUsername() + " added ignore: " + usernameHash);
            
        } catch (IOException ex) {
            Logger.error("Ignore add error: " + ex.getMessage());
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
    
    private void sendMessage(Player player, String message) throws IOException {
        Buffer out = new Buffer();
        out.putShort(Opcodes.Server.SV_MESSAGE.value);
        out.putString(message);
        
        player.getSocket().getOutputStream().write(out.toArrayWithLen());
        player.getSocket().getOutputStream().flush();
    }
}
