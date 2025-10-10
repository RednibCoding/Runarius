import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

public class ClientHandler implements Runnable {
    private Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (InputStream inputStream = socket.getInputStream(); OutputStream outputStream = socket.getOutputStream()) {
            Logger.info("ClientHandler started for connection: " + socket.getRemoteSocketAddress());
            int loopCount = 0;
            
            while (true) {
                loopCount++;
                if (loopCount % 100 == 0) {
                    Logger.debug("ClientHandler loop iteration: " + loopCount);
                }
                
                // Check if at least 4 bytes are available to read the length and opcode
                if (inputStream.available() >= 4) {
                    byte[] lengthOpcodeBuffer = inputStream.readNBytes(4);
                    ByteBuffer headerBuffer = ByteBuffer.wrap(lengthOpcodeBuffer);
                    short length = headerBuffer.getShort();
                    short opcode = headerBuffer.getShort();

                    Logger.info(">>> Packet received: opcode=" + opcode + ", length=" + length + ", dataLength=" + (length - 2));

                    // Ensure that the full packet data is available
                    if (inputStream.available() >= length - 4) {
                        byte[] dataBuffer = inputStream.readNBytes(length - 2); // read length without opcode bytes (2)
                        Buffer data = new Buffer(dataBuffer);

                        IPacketHandler handler = ServerSidePacketHandlers.getHandlerByOpcode(opcode);

                        if (handler != null) {
                            Logger.debug("Calling handler for opcode=" + opcode);
                            handler.handle(socket, data);
                        } else {
                            Logger.warn("Unknown opcode: " + opcode + " (length=" + length + ")");
                        }
                    } else {
                        Thread.sleep(50); // Wait for more data to arrive
                    }
                } else {
                    Thread.sleep(50); // Wait for more data to arrive
                }
            }
        } catch (IOException ex) {
            System.out.println("Client disconnected or connection reset: " + ex.getMessage());
        } catch (BufferUnderflowException ex) {
            System.out.println("Got invalid packet: " + ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            System.out.println("Thread interrupted: " + ex.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException ex) {
                System.out.println("Failed to close socket: " + ex.getMessage());
            }
            System.out.println("Client disconnected");
        }
    }
}