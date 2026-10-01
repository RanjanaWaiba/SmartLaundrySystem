package manager;                                              // This file belongs to the 'manager' package (folder)

import event.SimulationBus;                                   // Needed to fire GUI events
import exception.KiosksClosedException;                       // Thrown when kiosks are permanently closed
import facility.Dryer;                                        // Physical dryer class
import facility.LaundryFacility;                              // Builds all physical machines
import facility.PaymentKiosk;                                 // Physical payment kiosk class
import facility.WashingMachine;                               // Physical washer class
import util.ConfigLoader;                                     // Reads counts and thresholds from config.properties
import util.Logger;                                           // Thread-safe console logger
import event.MachineType;
import java.util.concurrent.ConcurrentLinkedQueue;            // Thread-safe FIFO queue for machine pools
import java.util.concurrent.Semaphore;                        // Counting permit lock (capacity guard)
import java.util.concurrent.atomic.AtomicBoolean;             // Thread-safe one-shot flag (owner-called)
import java.util.concurrent.atomic.AtomicInteger;             // Thread-safe counter (payment queue size)

/**
 * (class Javadoc from the original file...)
 */
public class ResourceManager {                                // Public class - Main creates one shared instance

    // --- Semaphores: enforce capacity for each resource type ---
    private final Semaphore washerSemaphore;                  // 6 permits - only 6 customers can wash at once
    private final Semaphore dryerSemaphore;                   // 4 permits - only 4 customers can dry at once
    private final Semaphore kioskSemaphore;                   // 2 permits - only 2 customers can pay at once

    // --- Pools: track WHICH specific machine is currently free ---
    private final ConcurrentLinkedQueue<WashingMachine> washerPool;   // Free washers waiting to be handed out
    private final ConcurrentLinkedQueue<Dryer> dryerPool;             // Free dryers
    private final ConcurrentLinkedQueue<PaymentKiosk> kioskPool;      // Free kiosks

    // --- Shared references injected by Main ---
    private final StatisticsManager stats;                    // Shared statistics (atomic counters)
    private final SimulationBus bus;                          // Shared event bus for GUI notifications

    // ---- Bonus congested-scenario state ----
    private final boolean congestedMode;                                    // True if launched with --congested
    private final AtomicInteger paymentQueueCount = new AtomicInteger(0);   // How many customers are currently at payment
    private final AtomicBoolean ownerCalled = new AtomicBoolean(false);     // One-shot flag - flips false→true exactly once

    /**
     * Constructor - runs ONCE at startup (in Main).
     * Creates the machines, builds three fair semaphores, fills three queues.
     */
    public ResourceManager(StatisticsManager stats, SimulationBus bus, boolean congestedMode) {
        this.stats = stats;                                   // Store reference to statistics
        this.bus = bus;                                       // Store reference to event bus
        this.congestedMode = congestedMode;                   // Store mode flag

        LaundryFacility facility = new LaundryFacility();     // Build 6 washers, 4 dryers, 2 kiosks

        // Create FAIR semaphores (true = FIFO order, avoids starvation)
        washerSemaphore = new Semaphore(ConfigLoader.getWasherCount(), true);  // 6 permits, fair
        dryerSemaphore  = new Semaphore(ConfigLoader.getDryerCount(), true);   // 4 permits, fair
        kioskSemaphore  = new Semaphore(ConfigLoader.getKioskCount(), true);   // 2 permits, fair

        // Fill the pools with the freshly built machines
        washerPool = new ConcurrentLinkedQueue<>(facility.getWashers());       // Queue holds all 6 washers
        dryerPool  = new ConcurrentLinkedQueue<>(facility.getDryers());        // Queue holds all 4 dryers
        kioskPool  = new ConcurrentLinkedQueue<>(facility.getKiosks());        // Queue holds both kiosks

        if (congestedMode) {                                   // If congested mode requested...
            Logger.log("Both payment kiosks are OUT OF ORDER today (congested scenario).");
        }
    }

    // ============================================================
    // WASHERS - acquire and release
    // ============================================================

    /**
     * Blocks until a washer permit is free, then hands out a specific washer.
     * Called from Customer.run() inside a try/finally - the matching
     * releaseWasher() is in the finally block.
     */
    public WashingMachine acquireWasher() throws InterruptedException {
        long start = System.currentTimeMillis();              // Note the time before we block
        washerSemaphore.acquire();                            // BLOCKS if all 6 permits are held
        long wait = System.currentTimeMillis() - start;       // Compute how long we waited
        stats.recordWasherWait(wait);                         // Record this wait sample (atomic)

        WashingMachine wm = washerPool.poll();                // Take one washer from the queue (thread-safe)
        while (wm == null) {                                  // Defensive: pool should never be empty here...
            Thread.sleep(10);                                 // ...but if it is, wait 10 ms and retry
            wm = washerPool.poll();
        }
        stats.washerAcquired();                               // Atomic increment of "current washers in use"
        return wm;                                            // Return the specific washer to the customer
    }

    public void releaseWasher(WashingMachine machine) {
        if (machine != null) {                                // Defensive: never try to release null
            washerPool.offer(machine);                        // Put the washer back in the pool
            stats.washerReleased();                           // Atomic decrement of "current washers in use"
            bus.machineIdle(MachineType.WASHER, machine.getId()); // Tell GUI "this washer is now idle"
            washerSemaphore.release();                        // Return the permit - unblocks a waiting customer
        }
    }

    // ============================================================
    // DRYERS - identical to washers, but 4 permits instead of 6
    // ============================================================

    public Dryer acquireDryer() throws InterruptedException {
        long start = System.currentTimeMillis();
        dryerSemaphore.acquire();                             // BLOCKS if all 4 permits are held
        long wait = System.currentTimeMillis() - start;
        stats.recordDryerWait(wait);                          // Record this wait sample

        Dryer d = dryerPool.poll();                           // Take one dryer from the pool
        while (d == null) {                                   // Defensive retry loop
            Thread.sleep(10);
            d = dryerPool.poll();
        }
        stats.dryerAcquired();                                // Atomic increment
        return d;                                             // Return the specific dryer
    }

    public void releaseDryer(Dryer machine) {
        if (machine != null) {
            dryerPool.offer(machine);                         // Put the dryer back
            stats.dryerReleased();                            // Atomic decrement
            bus.machineIdle(MachineType.DRYER, machine.getId());
            dryerSemaphore.release();                         // Return the permit
        }
    }

    // ============================================================
    // KIOSKS - normal mode uses the semaphore; congested mode uses
    // the payment queue counter instead
    // ============================================================

    public PaymentKiosk acquireKiosk() throws InterruptedException, KiosksClosedException {
        int waiting = paymentQueueCount.incrementAndGet();    // Atomically increment the payment queue counter
        Logger.log("Payment queue size: " + waiting);         // Log the current queue size

        if (congestedMode) {                                  // In congested mode, kiosks never open
            try {
                checkOwnerCall(waiting);                      // Check if we should fire the owner-call event
                while (!ownerCalled.get()) {                  // Wait until the owner has been called
                    Thread.sleep(200);                        // Poll every 200 ms
                }
                Thread.sleep(ConfigLoader.getCongestionAbandonDelay());  // Wait 5 more seconds
                throw new KiosksClosedException("Kiosks are out of order for the day");  // Give up
            } finally {
                paymentQueueCount.decrementAndGet();          // Always decrement before leaving
            }
        }

        // ---- Normal mode: standard semaphore acquire + poll ----
        long start = System.currentTimeMillis();
        kioskSemaphore.acquire();                             // BLOCKS if both kiosks are held
        long wait = System.currentTimeMillis() - start;
        stats.recordKioskWait(wait);                          // Record this wait sample

        PaymentKiosk k = kioskPool.poll();                    // Take a kiosk
        while (k == null) {                                   // Defensive retry
            kioskSemaphore.release();                         // ...give the permit back
            Thread.sleep(50);                                 // ...wait 50 ms
            kioskSemaphore.acquire();                         // ...try to take it again
            k = kioskPool.poll();                             // ...retry poll
        }

        paymentQueueCount.decrementAndGet();                  // Decrement queue counter before returning
        return k;                                             // Return the specific kiosk
    }

    /**
     * Fires the owner-call event exactly once.
     * compareAndSet(false, true) is atomic - if 30 threads call this
     * simultaneously, only ONE of them succeeds in flipping the flag.
     */
    private void checkOwnerCall(int waiting) {
        if (waiting >= ConfigLoader.getCongestionThreshold()               // Have we reached 30?
                && ownerCalled.compareAndSet(false, true)) {              // AND can we flip false→true?
            Logger.log("CONGESTION: " + waiting + " customers waiting - OWNER HAS BEEN CALLED IN.");
            bus.congestionStarted();                          // Notify the GUI
        }
    }

    public void releaseKiosk(PaymentKiosk machine) {
        if (machine != null) {
            kioskPool.offer(machine);                         // Put kiosk back
            bus.machineIdle(MachineType.KIOSK, machine.getId());
            kioskSemaphore.release();                         // Return the permit
        }
    }

    // ============================================================
    // GUI QUERIES - these are called periodically from LaundryFrame
    // to update the labels. They never block.
    // ============================================================

    public int getWasherWaitCount() { return washerSemaphore.getQueueLength(); }  // How many threads are BLOCKED on washerSemaphore
    public int getDryerWaitCount()  { return dryerSemaphore.getQueueLength(); }   // How many are BLOCKED on dryerSemaphore

    /**
     * FIX: In congested mode, customers wait on paymentQueueCount (they
     * never touch kioskSemaphore), so returning the semaphore queue length
     * would always show 0 on the GUI. Return the actual payment-queue
     * counter in that mode.
     */
    public int getKioskWaitCount() {
        return congestedMode
                ? paymentQueueCount.get()                     // Congested mode - use our custom counter
                : kioskSemaphore.getQueueLength();            // Normal mode - use the semaphore's queue
    }

    public int getPaymentQueueSize() { return paymentQueueCount.get(); }  // Raw counter accessor
    public boolean isCongestedMode() { return congestedMode; }            // True if launched with --congested
    public boolean isOwnerCalled() { return ownerCalled.get(); }          // True once owner has been called
}