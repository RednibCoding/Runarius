import java.net.Socket;

public class SV_MessageHandler implements IClientPacketHandler {
    @Override
    public void handle(GameConnection client, Socket socket, Buffer data) {
        String message = data.getString();
        client.showServerMessage(message);
        Logger.debug("Server message: " + message);
    }
}
