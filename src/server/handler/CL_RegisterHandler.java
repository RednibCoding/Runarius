import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class CL_RegisterHandler implements IPacketHandler {
    @Override
    public void handle(Socket socket, Buffer data) {
        try {

            InputStream inStream = socket.getInputStream();
            OutputStream outStream = socket.getOutputStream();
            
            int clientVersion = data.getInt();
            Long sessionID = data.getLong();
            String username = data.getString();
            String password = data.getString();

            // TODO: do actual registration here

            Buffer out = new Buffer();
            out.putInt(RegistrationResponse.SUCCESS.getCode());
            outStream.write(out.toArray());
            outStream.flush();
        } catch (IOException ex) {
            Logger.error(ex.getMessage());
        }
    }
}
