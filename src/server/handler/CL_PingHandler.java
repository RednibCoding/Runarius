import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

public class CL_PingHandler implements IPacketHandler {
    @Override
    public void handle(Socket socket, Buffer data) {
        try {
            OutputStream outStream = socket.getOutputStream();

            Buffer out = new Buffer();
            out.putShort(Opcodes.Server.SV_MESSAGE.value);
            out.putString("Hi from the server");
            outStream.write(out.toArrayWithLen());
            outStream.flush();

        } catch (IOException ex) {
            Logger.error(ex.getMessage());
        }
    }
}
