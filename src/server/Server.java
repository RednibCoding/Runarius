import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static final int PORT = 43594;
    private static final int THREAD_POOL_SIZE = 10;

    public static void main(String[] args) {
        List<String> arguments = new ArrayList<>();
        arguments.addAll(Arrays.asList(args));
        arguments.add("-noconsole"); // TODO: Remove this line in release mode

        int launchResult = Util.launchFromTerminal(arguments);

        if (launchResult == 1) {
            return; // Launched successfully, exit current instance
        } else if (launchResult == -1) {
            System.err.println("Error launching terminal. Exiting.");
            return; // Error occurred, exit
        }

        // Proceed to run the server if not launched from terminal
        ExecutorService threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        // Start the game tick system
        GameTick.getInstance().start();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is listening on port " + PORT);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New client connected");

                threadPool.execute(new ClientHandler(socket));
            }
        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            GameTick.getInstance().stop();
            threadPool.shutdown();
        }
    }
}
