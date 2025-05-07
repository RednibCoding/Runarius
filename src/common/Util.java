import java.io.IOException;
import java.util.List;

public class Util {
    public static int launchFromTerminal(List<String> args) {
        boolean wasLaunched = false;
        boolean noConsole = false;

        for (String arg : args) {
            if (arg.equals("-waslaunched")) {
                wasLaunched = true;
            }
            if (arg.equals("-noconsole")) {
                noConsole = true;
            }
        }

        if (wasLaunched || noConsole) {
            return 0; // Already launched from terminal or no console needed
        }

        String os = System.getProperty("os.name").toLowerCase();
        ProcessBuilder processBuilder = new ProcessBuilder();

        try {
            if (os.contains("win")) {
                // Windows command to open a new command prompt and run the server
                processBuilder.command("cmd.exe", "/c", "start", "cmd.exe", "/k", "java -jar server.jar -waslaunched");
            } else if (os.contains("mac")) {
                // macOS command to open a new terminal and run the server
                processBuilder.command("osascript", "-e",
                        "tell application \"Terminal\" to do script \"java -jar server.jar -waslaunched\"");
            } else if (os.contains("nix") || os.contains("nux")) {
                // Linux command to open a new terminal and run the server
                processBuilder.command("x-terminal-emulator", "-e", "java -jar server.jar -waslaunched");
            } else {
                System.out.println("Unsupported OS: " + os);
                return -1; // Unsupported OS
            }

            processBuilder.start();
            return 1; // Launched successfully
        } catch (IOException e) {
            e.printStackTrace();
            return -1; // Error occurred
        }
    }
}
