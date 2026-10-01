package util;                                                // This file belongs to the 'util' package

import java.util.concurrent.ThreadLocalRandom;               // Thread-safe random number generator

/**
 * Utility class for all random values used in the simulation.
 *
 * Uses ThreadLocalRandom instead of Random so that multiple threads
 * can generate random numbers without contending on a shared lock.
 */
public final class RandomUtil {

    /** Private constructor - prevents instantiation. */
    private RandomUtil() {}

    /** Random wash duration (4000-6000 ms by default). */
    public static int washDurationMillis() {
        return ThreadLocalRandom.current().nextInt(
                ConfigLoader.getWashMin(),
                ConfigLoader.getWashMax() + 1
        );
    }

    /** Random drying duration (3000-5000 ms by default). */
    public static int dryDurationMillis() {
        return ThreadLocalRandom.current().nextInt(
                ConfigLoader.getDryMin(),
                ConfigLoader.getDryMax() + 1
        );
    }

    /** Random payment duration (1000-2000 ms by default). */
    public static int paymentDurationMillis() {
        return ThreadLocalRandom.current().nextInt(
                ConfigLoader.getPaymentMin(),
                ConfigLoader.getPaymentMax() + 1
        );
    }

    /** Random arrival gap (0-3000 ms by default). */
    public static int arrivalGapMillis() {
        return ThreadLocalRandom.current().nextInt(
                ConfigLoader.getArrivalMin(),
                ConfigLoader.getArrivalMax() + 1
        );
    }

    /** Returns true approximately 'percent' percent of the time. */
    public static boolean shouldFail(int percent) {
        return ThreadLocalRandom.current().nextInt(100) < percent;
    }
}