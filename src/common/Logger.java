// TODO: implement Logger properly

public class Logger {
    public static boolean debug = true;

    public static void debug(String msg) {
        if (debug) System.out.println(msg);
    }

    public static void info(String msg) {
        System.out.println(msg);
    }

    public static void warn(String msg) {
        System.out.println(msg);
    }

    public static void error(String msg) {
        System.out.println(msg);
    }

    
}
