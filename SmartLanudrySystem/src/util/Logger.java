package util;                                                // Same 'util' package

/**Keep console log messages from interfering with each other.
 * Thread-safe console logger.
 * This is the ONLY place in the whole project that uses synchronized.
 */
public class Logger {
    private static final Object lock = new Object();         // One shared lock for every log call

    /**
     * Prints a single log line, protected from interleaving with other threads.
     */
    public static void log(String message) {
        synchronized (lock) {                                // Only ONE thread may be inside at a time
            System.out.println("[" + TimeUtil.timestamp() + "] " +           // [HH:mm:ss.SSS]
                    Thread.currentThread().getName() + " | " + message);      // threadName | message
        }                                                    // Lock released automatically on block exit
    }
}