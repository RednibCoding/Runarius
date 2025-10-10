import java.net.Socket;

/**
 * Handles when client closes connection.
 */
public class CL_CloseConnectionHandler implements IPacketHandler {
    @Override
    public void handle(Socket socket, Buffer data) {
        try {
            ServerContext context = ServerContext.get();
            Player player = context.getPlayers().findBySocket(socket).orElse(null);
            if (player == null) {
                Logger.error("Close connection: player not found for socket");
                return;
            }

            Logger.info(player.getUsername() + " closed connection");

            context.getVisibilityService().handlePlayerRemoval(player);
            context.getPlayers().removePlayer(player);

            socket.close();
        } catch (Exception ex) {
            Logger.error("Close connection error: " + ex.getMessage());
        }
    }
}
