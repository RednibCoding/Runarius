import java.net.Socket;

@FunctionalInterface
public interface IClientPacketHandler {
    public void handle(GameConnection client, Socket socket, Buffer data);
}
