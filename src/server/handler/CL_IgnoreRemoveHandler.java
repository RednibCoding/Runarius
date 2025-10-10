import java.io.IOException;
import java.net.Socket;

/**
 * Handles when a player removes someone from their ignore list.
 */
public class CL_IgnoreRemoveHandler implements IPacketHandler {
    @Override
    public void handle(Socket socket, Buffer data) {
        try {
            long usernameHash = data.getLong();
            
            // Find the player by socket
            PlayerRepository players = ServerContext.get().getPlayers();
            Player player = players.findBySocket(socket).orElse(null);
            if (player == null) {
                Logger.error("Ignore remove: player not found for socket");
                return;
            }
            
            // Remove from ignore list
            player.getIgnoreList().remove(usernameHash);
            
            // Send updated ignore list
            sendIgnoreList(player);
            
            Logger.info(player.getUsername() + " removed ignore: " + usernameHash);
            
        } catch (IOException ex) {
            Logger.error("Ignore remove error: " + ex.getMessage());
        }
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
}
