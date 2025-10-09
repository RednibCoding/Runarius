import java.net.Socket;

public class SV_IgnoreList implements IClientPacketHandler {
    @Override
    public void handle(GameConnection client, Socket socket, Buffer data) {
        // Read ignore list from server
        byte count = data.getByte();
        client.ignoreListCount = count;
        
        for (int i = 0; i < count; i++) {
            long usernameHash = data.getLong();
            client.ignoreList[i] = usernameHash;
        }
        
        Logger.debug("Ignore list: " + count + " players");
    }
}