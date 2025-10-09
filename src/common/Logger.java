/**
 * Simple logging utility with configurable log levels.
 * Set LOG_LEVEL to control which messages are displayed.
 */
public class Logger {
    
    // Log levels (higher number = more verbose)
    public static final int LEVEL_NONE = 0;
    public static final int LEVEL_ERROR = 1;
    public static final int LEVEL_WARN = 2;
    public static final int LEVEL_INFO = 3;
    public static final int LEVEL_DEBUG = 4;
    
    // Current log level - change this to control verbosity
    // LEVEL_INFO (3) shows errors, warnings, and info messages (recommended for production)
    // LEVEL_DEBUG (4) shows everything including debug messages (recommended for development)
    private static int LOG_LEVEL = LEVEL_INFO;
    
    /**
     * Set the current log level.
     * @param level One of LEVEL_NONE, LEVEL_ERROR, LEVEL_WARN, LEVEL_INFO, LEVEL_DEBUG
     */
    public static void setLogLevel(int level) {
        LOG_LEVEL = level;
    }
    
    /**
     * Get the current log level.
     */
    public static int getLogLevel() {
        return LOG_LEVEL;
    }
    
    /**
     * Log a debug message (only shown when LOG_LEVEL >= LEVEL_DEBUG)
     */
    public static void debug(String msg) {
        if (LOG_LEVEL >= LEVEL_DEBUG) {
            System.out.println("[DEBUG] " + msg);
        }
    }
    
    /**
     * Log an info message (only shown when LOG_LEVEL >= LEVEL_INFO)
     */
    public static void info(String msg) {
        if (LOG_LEVEL >= LEVEL_INFO) {
            System.out.println("[INFO] " + msg);
        }
    }
    
    /**
     * Log a warning message (only shown when LOG_LEVEL >= LEVEL_WARN)
     */
    public static void warn(String msg) {
        if (LOG_LEVEL >= LEVEL_WARN) {
            System.out.println("[WARN] " + msg);
        }
    }
    
    /**
     * Log an error message (only shown when LOG_LEVEL >= LEVEL_ERROR)
     */
    public static void error(String msg) {
        if (LOG_LEVEL >= LEVEL_ERROR) {
            System.err.println("[ERROR] " + msg);
        }
    }
}
