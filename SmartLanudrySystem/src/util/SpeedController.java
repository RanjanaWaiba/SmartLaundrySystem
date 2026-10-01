
package util;

/**
 * Controls the global simulation speed.
 *
 * The value is volatile because it can be modified by the Swing
 * Event Dispatch Thread while customer worker threads read it.
 *
 * Example:
 *
 * 1.0x -> normal speed
 * 2.0x -> twice as fast
 * 0.5x -> half speed
 */
public final class SpeedController {

    /*
     * volatile guarantees visibility between the GUI thread and
     * customer worker threads.
     */
    private static volatile double speedMultiplier = 1.0;

    private SpeedController() {
        // Utility class - no objects should be created.
    }

    /**
     * Returns the current simulation speed.
     */
    public static double getSpeed() {
        return speedMultiplier;
    }

    /**
     * Changes the simulation speed.
     *
     * Minimum speed is 0.1x.
     */
    public static void setSpeed(double multiplier) {

        speedMultiplier =
                Math.max(0.1, multiplier);
    }

    /**
     * Converts a normal duration into a speed-adjusted duration.
     *
     * Example:
     *
     * 4000 ms at 1.0x = 4000 ms
     * 4000 ms at 2.0x = 2000 ms
     * 4000 ms at 0.5x = 8000 ms
     */
    public static long applySpeed(long millis) {

        return Math.max(
                1L,
                (long) (millis / speedMultiplier)
        );
    }
}
