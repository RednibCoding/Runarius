import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

public class CL_SessionHandler implements IPacketHandler {
    @Override
    public void handle(Socket socket, Buffer data) {
        try {
            OutputStream outStream = socket.getOutputStream();
            
            String username = data.getString();
            long sessionId = genSessionId(username);
            
            // Create player object for this session
            Player player = new Player(socket, username, sessionId);
            
            // Add to pending players (will be moved to active players on login)
            GameWorld.getInstance().addPendingPlayer(player);
            
            Buffer out = new Buffer();
            out.putLong(sessionId);
            
            outStream.write(out.toArray());
            outStream.flush();
            
            Logger.info("Session created for " + username + " (session: " + sessionId + ")");
        } catch (IOException ex) {
            Logger.error(ex.getMessage());
        }
    }

    private long genSessionId(String data) {
        return System.currentTimeMillis();
    }
}
