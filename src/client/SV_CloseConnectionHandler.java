import java.net.Socket;

public class SV_CloseConnectionHandler implements IClientPacketHandler {
    @Override
    public void handle(GameConnection client, Socket socket, Buffer data) {
        client.closeConnection();
        Logger.debug("Server requested connection close");
    }
}