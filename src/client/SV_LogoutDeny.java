import java.net.Socket;

public class SV_LogoutDeny implements IClientPacketHandler {
    @Override
    public void handle(GameConnection client, Socket socket, Buffer data) {
        client.cantLogout();
        Logger.debug("Server denied logout request");
    }
}