package util;                                                // This file belongs to the 'util' package (folder)

import java.io.FileInputStream;                              // For reading a file from the filesystem
import java.io.InputStream;                                  // The general "stream of bytes" type
import java.util.Properties;                                 // Stores key=value pairs (Java's built-in config class)

/**
 * Reads config.properties ONCE at startup.
 * Provides typed static getters for every setting.
 * If the file is not found, falls back to hard-coded defaults.
 */
public class ConfigLoader {
    private static final Properties props = new Properties();  // One shared Properties object for the whole program
    private static boolean loaded = false;                     // Flag - have we successfully loaded the file yet?

    // Static block - runs ONCE when this class is first referenced.
    // We try THREE different locations because Java programs can be run
    // from an IDE, from a JAR, or from the command line - each has a
    // different notion of "current directory".
    static {
        try {
            // ---- Attempt 1: look on the classpath (works for JAR files) ----
            InputStream input = ConfigLoader.class.getClassLoader()
                    .getResourceAsStream("config.properties");  // Ask the classloader to find the resource
            if (input != null) {                                // If found...
                props.load(input);                              // Read all key=value pairs into props
                loaded = true;                                  // Mark as loaded
                System.err.println("✅ config.properties loaded from classpath.");
                input.close();                                  // Close the stream
            }

            // ---- Attempt 2 & 3: look at file-system paths ----
            if (!loaded) {                                      // Only try if attempt 1 failed
                try {
                    InputStream fileInput = new FileInputStream("src/config.properties");  // IDE layout
                    props.load(fileInput);
                    loaded = true;
                    System.err.println("✅ config.properties loaded from file system (src/).");
                    fileInput.close();
                } catch (Exception e2) {
                    try {
                        InputStream fileInput2 = new FileInputStream("config.properties");  // Working directory
                        props.load(fileInput2);
                        loaded = true;
                        System.err.println("✅ config.properties loaded from current directory.");
                        fileInput2.close();
                    } catch (Exception e3) {
                        // Ignore - we'll warn below
                    }
                }
            }

            if (!loaded) {                                      // Still not found after all three attempts
                System.err.println("⚠️ config.properties not found. Using defaults.");
            }
        } catch (Exception e) {                                 // Any unexpected error
            e.printStackTrace();                                // Print stack trace
        }
    }

    /**
     * Helper - reads a key and converts it to int, or returns the default if missing.
     */
    private static int getInt(String key, int defaultValue) {
        String val = props.getProperty(key);                    // Look up the key
        return val != null ? Integer.parseInt(val) : defaultValue;  // Parse if present, else default
    }

    // ---- Public getters - one per config setting ----

    public static int getWasherCount() { return getInt("washer.count", 6); }         // 6 washers by default
    public static int getDryerCount() { return getInt("dryer.count", 4); }           // 4 dryers
    public static int getKioskCount() { return getInt("kiosk.count", 2); }           // 2 kiosks
    public static int getCustomerCount() { return getInt("customer.count", 50); }    // 50 customers

    public static int getWashMin() { return getInt("wash.min", 4000); }              // Wash min 4000 ms
    public static int getWashMax() { return getInt("wash.max", 6000); }              // Wash max 6000 ms
    public static int getDryMin() { return getInt("dry.min", 3000); }                // Dry min 3000 ms
    public static int getDryMax() { return getInt("dry.max", 5000); }                // Dry max 5000 ms
    public static int getPaymentMin() { return getInt("payment.min", 1000); }        // Payment min 1000 ms
    public static int getPaymentMax() { return getInt("payment.max", 2000); }        // Payment max 2000 ms
    public static int getArrivalMin() { return getInt("arrival.min", 0); }           // Arrival gap min 0 ms
    public static int getArrivalMax() { return getInt("arrival.max", 3000); }        // Arrival gap max 3000 ms
    public static int getWasherFailRate() { return getInt("washer.fail.rate", 5); }  // 5% washer failure
    public static int getPaymentFailRate() { return getInt("payment.fail.rate", 5); } // 5% payment failure
    public static int getRetryDelay() { return getInt("retry.delay", 2000); }        // Retry after 2000 ms
    public static int getSimulationSeconds() { return getInt("simulation.seconds", 60); } // Target 60 s

    // ---- Bonus congested-scenario settings ----
    public static int getCongestionThreshold() { return getInt("congestion.threshold", 30); }     // Owner called at 30
    public static int getCongestionAbandonDelay() { return getInt("congestion.abandon.delay", 5000); } // Wait 5 s more
}