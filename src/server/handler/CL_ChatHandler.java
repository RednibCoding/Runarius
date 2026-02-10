import java.io.IOException;
import java.net.Socket;

/**
 * Handles player chat messages.
 * Receives scrambled chat bytes from the client and broadcasts them
 * to all nearby players via SV_REGION_PLAYER_UPDATE (updateType=1).
 */
public class CL_ChatHandler implements IPacketHandler {
    @Override
    public void handle(Socket socket, Buffer data) {
        try {
            int messageLength = data.remaining();
            byte[] message = new byte[messageLength];
            for (int i = 0; i < messageLength; i++) {
                message[i] = data.getByte();
            }

            PlayerRepository players = ServerContext.get().getPlayers();
            Player player = players.findBySocket(socket).orElse(null);
            if (player == null) {
                Logger.error("Chat: player not found for socket");
                return;
            }

            if (player.isBlockChat()) {
                return;
            }

            Logger.info(player.getUsername() + " says: [" + message.length + " bytes]");

            // Broadcast chat to all nearby players
            VisibilityService visibility = ServerContext.get().getVisibilityService();
            players.forEachOnline(viewer -> {
                if (viewer == player) return;
                if (!visibility.isWithinRange(viewer, player)) return;

                try {
                    PlayerPacketSender.sendChat(viewer, player, message);
                } catch (IOException ex) {
                    Logger.error("Failed to send chat to " + viewer.getUsername() + ": " + ex.getMessage());
                }
            });

        } catch (Exception ex) {
            Logger.error("Chat error: " + ex.getMessage());
        }
    }
}
