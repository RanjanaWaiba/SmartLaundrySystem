

package util;                                                // Same 'util' package

import java.time.LocalDateTime;                              // Java 8 time class
import java.time.format.DateTimeFormatter;                   // Formats date/time as text

public final class TimeUtil {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    // Creates a formatter that outputs like "16:47:45.578"
    // HH = 24-hour hour, mm = minute, ss = second, SSS = millisecond

    /**
     * Returns the current time as a formatted string, e.g. "16:47:45.578".
     * Used by Logger.log() for timestamps.
     */
    public static String timestamp() {
        return LocalDateTime.now().format(FORMATTER);        // Get current time, format with the pattern above
    }

    /**
     * Returns the current time in milliseconds since 1970.
     * Used for computing elapsed durations.
     */
    public static long nowMillis() {
        return System.currentTimeMillis();                   // Raw millisecond value
    }
}