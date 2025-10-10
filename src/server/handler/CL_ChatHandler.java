import java.net.Socket;

/**
 * Handles player chat messages.
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

        } catch (Exception ex) {
            Logger.error("Chat error: " + ex.getMessage());
        }
    }
}
