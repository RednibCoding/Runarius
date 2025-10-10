import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

public class ClientHandler implements Runnable {
    private final ServerContext context;
    private final Socket socket;
    private final PacketDispatcher dispatcher;

    public ClientHandler(ServerContext context, Socket socket) {
        this.context = context;
        this.socket = socket;
        this.dispatcher = context.getPacketDispatcher();
    }

    @Override
    public void run() {
        try (InputStream inputStream = socket.getInputStream(); OutputStream outputStream = socket.getOutputStream()) {
            Logger.info("ClientHandler started for " + socket.getRemoteSocketAddress());

            while (!socket.isClosed()) {
                short length = readShort(inputStream);
                short opcode = readShort(inputStream);

                if (length <= 0) {
                    Logger.warn("Invalid packet length " + length + " from " + socket.getRemoteSocketAddress());
                    break;
                }

                byte[] payload = readFully(inputStream, length - 2);
                if (payload == null) {
                    Logger.warn("Connection closed while reading payload from " + socket.getRemoteSocketAddress());
                    break;
                }

                Logger.debug(">>> Packet received: opcode=" + opcode + ", length=" + length);

                Buffer data = new Buffer(payload);
                IPacketHandler handler = dispatcher.get(opcode);
                if (handler == null) {
                    Logger.warn("No handler registered for opcode=" + opcode);
                    continue;
                }

                try {
                    handler.handle(socket, data);
                } catch (Exception handlerException) {
                    Logger.error("Handler failure for opcode=" + opcode + ": " + handlerException.getMessage());
                    handlerException.printStackTrace();
                }
            }
        } catch (IOException ex) {
            Logger.warn("Client disconnected: " + ex.getMessage());
        } finally {
            cleanup();
        }
    }

    private short readShort(InputStream inputStream) throws IOException {
        byte[] bytes = readFully(inputStream, 2);
        if (bytes == null) {
            throw new IOException("Unexpected end of stream");
        }
        return ByteBuffer.wrap(bytes).getShort();
    }

    private byte[] readFully(InputStream inputStream, int length) throws IOException {
        byte[] buffer = new byte[length];
        int offset = 0;
        while (offset < length) {
            int read = inputStream.read(buffer, offset, length - offset);
            if (read == -1) {
                return null;
            }
            offset += read;
        }
        return buffer;
    }

    private void cleanup() {
        try {
            socket.close();
        } catch (IOException ex) {
            Logger.warn("Failed to close socket: " + ex.getMessage());
        }

        PlayerRepository players = context.getPlayers();
        players.findBySocket(socket).ifPresent(player -> {
            context.getVisibilityService().handlePlayerRemoval(player);
            players.removePlayer(player);
        });

        players.findPendingBySocket(socket).ifPresent(players::removePending);
        Logger.info("Client disconnected: " + socket.getRemoteSocketAddress());
    }
}