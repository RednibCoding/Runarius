import java.io.IOException;
import java.net.Socket;

/**
 * Handles privacy setting updates from the client.
 */
public class CL_PrivacySettingsHandler implements IPacketHandler {
    @Override
    public void handle(Socket socket, Buffer data) {
        try {
            byte blockChat = data.getByte();
            byte blockPM = data.getByte();
            byte blockTrades = data.getByte();
            byte blockDuels = data.getByte();
            
            // Find the player by socket
            PlayerRepository players = ServerContext.get().getPlayers();
            Player player = players.findBySocket(socket).orElse(null);
            if (player == null) {
                Logger.error("Privacy settings: player not found for socket");
                return;
            }
            
            // Update settings
            player.setBlockChat(blockChat != 0);
            player.setBlockPrivateMessages(blockPM != 0);
            player.setBlockTrade(blockTrades != 0);
            player.setBlockDuel(blockDuels != 0);
            
            // Send confirmation
            sendPrivacySettings(player);
            
            Logger.info(player.getUsername() + " updated privacy settings");
            
        } catch (IOException ex) {
            Logger.error("Privacy settings error: " + ex.getMessage());
        }
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
}
