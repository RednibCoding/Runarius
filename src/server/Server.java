import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    public static void main(String[] args) {
        List<String> arguments = new ArrayList<>(Arrays.asList(args));
        arguments.add("-noconsole");

        int launchResult = Util.launchFromTerminal(arguments);
        if (launchResult == 1) {
            return;
        }
        if (launchResult == -1) {
            System.err.println("Error launching terminal. Exiting.");
            return;
        }

        ServerConfig config = ServerConfig.builder().build();
        ServerContext.initialize(config);
        ServerContext context = ServerContext.get();
        context.getGameLoop().start();

        try (ServerSocket serverSocket = new ServerSocket(config.getPort())) {
            Logger.info("Server listening on port " + config.getPort());
            acceptConnections(serverSocket, context);
        } catch (IOException ex) {
            Logger.error("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            context.shutdown();
        }
    }

    private static void acceptConnections(ServerSocket serverSocket, ServerContext context) throws IOException {
        while (true) {
            Socket socket = serverSocket.accept();
            Logger.info("New client connected: " + socket.getRemoteSocketAddress());
            context.getWorkerPool().execute(new ClientHandler(context, socket));
        }
    }
}
