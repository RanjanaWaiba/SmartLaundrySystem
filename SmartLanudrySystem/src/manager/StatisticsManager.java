package manager;                                              // Same 'manager' package

import util.Logger;                                           // For the "exported to" log line

import java.io.File;                                          // To create the CSV file object
import java.io.IOException;                                   // For catching file-write errors
import java.io.PrintWriter;                                   // For writing text to the CSV file
import java.util.concurrent.atomic.AtomicInteger;             // Thread-safe int counter
import java.util.concurrent.atomic.AtomicLong;                // Thread-safe long counter

/**
 * Thread-safe statistics manager.
 * Every field is an AtomicInteger or AtomicLong so that 50 threads
 * can update them concurrently without losing updates.
 * No synchronized, no locks - just lock-free atomic instructions.
 */
public class StatisticsManager {

    // ---- Basic counters ----
    private final AtomicInteger arrived = new AtomicInteger(0);       // How many customers have arrived
    private final AtomicInteger served = new AtomicInteger(0);        // How many finished successfully
    private final AtomicLong totalTime = new AtomicLong(0);           // Sum of every completion's total time

    // ---- Concurrency tracking ----
    private final AtomicInteger currentWashers = new AtomicInteger(0); // Washers in use right now
    private final AtomicInteger maxWashers = new AtomicInteger(0);     // Peak washers in use (running max)
    private final AtomicInteger currentDryers = new AtomicInteger(0);  // Dryers in use right now
    private final AtomicInteger maxDryers = new AtomicInteger(0);      // Peak dryers in use (running max)

    // ---- Failure counters ----
    private final AtomicInteger washerFailures = new AtomicInteger(0);   // How many washer failures occurred
    private final AtomicInteger paymentFailures = new AtomicInteger(0);  // How many payment failures
    private final AtomicInteger abandoned = new AtomicInteger(0);        // How many gave up in congested mode

    // ---- Wait-time accumulators (for averages) ----
    private final AtomicLong totalWasherWait = new AtomicLong(0);     // Sum of all washer wait times (ms)
    private final AtomicLong totalDryerWait = new AtomicLong(0);      // Sum of all dryer wait times
    private final AtomicLong totalKioskWait = new AtomicLong(0);      // Sum of all kiosk wait times
    private final AtomicInteger washerWaitCount = new AtomicInteger(0); // How many washer wait samples
    private final AtomicInteger dryerWaitCount = new AtomicInteger(0);  // How many dryer wait samples
    private final AtomicInteger kioskWaitCount = new AtomicInteger(0);  // How many kiosk wait samples

    // ============================================================
    // Simple one-line atomic increments - called from Customer threads
    // ============================================================

    public void recordArrival() { arrived.incrementAndGet(); }         // +1 to arrived
    public void recordCompletion(long ms) {                            // Called when a customer finishes
        served.incrementAndGet();                                      // +1 to served
        totalTime.addAndGet(ms);                                       // Add ms to total time
    }
    public void recordWasherFailure() { washerFailures.incrementAndGet(); }   // +1 washer failure
    public void recordPaymentFailure() { paymentFailures.incrementAndGet(); } // +1 payment failure
    public void recordAbandoned() { abandoned.incrementAndGet(); }            // +1 abandoned

    // ============================================================
    // Wait-time recording - called from ResourceManager.acquire*
    // ============================================================

    public void recordWasherWait(long ms) {
        totalWasherWait.addAndGet(ms);                                 // Add ms to washer wait total
        washerWaitCount.incrementAndGet();                             // +1 washer wait sample
    }
    public void recordDryerWait(long ms) {
        totalDryerWait.addAndGet(ms);                                  // Add ms to dryer wait total
        dryerWaitCount.incrementAndGet();                              // +1 dryer wait sample
    }
    public void recordKioskWait(long ms) {
        totalKioskWait.addAndGet(ms);                                  // Add ms to kiosk wait total
        kioskWaitCount.incrementAndGet();                              // +1 kiosk wait sample
    }

    // ============================================================
    // Concurrency tracking - called on acquire/release
    // ============================================================

    public void washerAcquired() {
        int cur = currentWashers.incrementAndGet();                    // +1 to current washers, capture the new value
        updateMax(maxWashers, cur);                                    // If cur > max, update max (via CAS loop)
    }
    public void washerReleased() { currentWashers.decrementAndGet(); } // -1 to current washers

    public void dryerAcquired() {
        int cur = currentDryers.incrementAndGet();                     // +1 to current dryers
        updateMax(maxDryers, cur);                                     // Update max if new peak
    }
    public void dryerReleased() { currentDryers.decrementAndGet(); }   // -1 to current dryers

    /**
     * Lock-free maximum update via compare-and-swap.
     * This is NOT the same as a simple `if (current > max) max = current;`
     * because that would be a check-then-act race.
     */
    private void updateMax(AtomicInteger maxField, int current) {
        int oldMax;
        do {
            oldMax = maxField.get();                                   // Read current max
            if (current <= oldMax) break;                              // Our value isn't bigger - done
        } while (!maxField.compareAndSet(oldMax, current));            // Try to swap. If someone else changed it, loop.
    }

    // ============================================================
    // Getters - called from the GUI to display statistics
    // ============================================================

    public int getArrived() { return arrived.get(); }                   // Snapshot of arrived
    public int getServed() { return served.get(); }                     // Snapshot of served
    public double getAverageTime() {
        int s = served.get();                                           // How many completed
        return s == 0 ? 0 : totalTime.get() / (double) s / 1000.0;      // Guard against /0; convert msâ†’s
    }
    public int getCurrentWashers() { return currentWashers.get(); }     // Current washers in use
    public int getMaxWashers() { return maxWashers.get(); }             // Peak washers in use
    public int getCurrentDryers() { return currentDryers.get(); }       // Current dryers in use
    public int getMaxDryers() { return maxDryers.get(); }               // Peak dryers in use
    public int getWasherFailures() { return washerFailures.get(); }     // Washer failure count
    public int getPaymentFailures() { return paymentFailures.get(); }   // Payment failure count
    public int getAbandoned() { return abandoned.get(); }               // Abandoned count

    public double getAverageWasherWait() {
        int c = washerWaitCount.get();                                  // How many wait samples
        return c == 0 ? 0 : totalWasherWait.get() / (double) c / 1000.0; // Average wait, msâ†’s
    }
    public double getAverageDryerWait() {
        int c = dryerWaitCount.get();
        return c == 0 ? 0 : totalDryerWait.get() / (double) c / 1000.0;
    }
    public double getAverageKioskWait() {
        int c = kioskWaitCount.get();
        return c == 0 ? 0 : totalKioskWait.get() / (double) c / 1000.0;
    }

    // ============================================================
    // Export to CSV (called once at the end of Main)
    // ============================================================

    public void exportCSV(String filename) {
        try (PrintWriter w = new PrintWriter(new File(filename))) {     // try-with-resources auto-closes
            w.println("Metric,Value");                                  // CSV header
            w.println("Total Customers Arrived," + getArrived());
            w.println("Total Customers Served," + getServed());
            w.println("Average Time Per Customer (s)," + String.format("%.2f", getAverageTime()));
            w.println("Max Concurrent Washers," + getMaxWashers());
            w.println("Max Concurrent Dryers," + getMaxDryers());
            w.println("Average Washer Wait (s)," + String.format("%.2f", getAverageWasherWait()));
            w.println("Average Dryer Wait (s)," + String.format("%.2f", getAverageDryerWait()));
            w.println("Average Kiosk Wait (s)," + String.format("%.2f", getAverageKioskWait()));
            w.println("Washer Failures," + getWasherFailures());
            w.println("Payment Failures," + getPaymentFailures());
            w.println("Customers Abandoned (congested scenario)," + getAbandoned());
            Logger.log("Statistics exported to: " + filename);         // Log the export
        } catch (IOException e) {                                       // If file write fails...
            e.printStackTrace();                                        // Print the error
        }
    }

    // ============================================================
    // Print human-readable report to console (called at the end)
    // ============================================================

    public void printReport() {
        System.out.println("\n===== FINAL STATISTICS =====");
        System.out.println("Total customers arrived : " + getArrived());
        System.out.println("Total customers served  : " + getServed());
        System.out.println("Average time per customer: " + String.format("%.2f s", getAverageTime()));
        System.out.println("Max concurrent washers  : " + getMaxWashers());
        System.out.println("Max concurrent dryers   : " + getMaxDryers());
        System.out.println("Avg Washer Wait (s)     : " + String.format("%.2f", getAverageWasherWait()));
        System.out.println("Avg Dryer Wait (s)      : " + String.format("%.2f", getAverageDryerWait()));
        System.out.println("Avg Kiosk Wait (s)      : " + String.format("%.2f", getAverageKioskWait()));
        System.out.println("Washer failures         : " + getWasherFailures());
        System.out.println("Payment failures        : " + getPaymentFailures());
        if (getAbandoned() > 0) {                                       // Only print abandoned line if nonzero
            System.out.println("Customers abandoned     : " + getAbandoned() + " (congested scenario)");
        }
        System.out.println("============================\n");
    }
}