package customer;

import event.SimulationBus;
import exception.KiosksClosedException;
import exception.MachineFailureException;
import exception.PaymentFailureException;
import facility.Dryer;
import facility.PaymentKiosk;
import facility.WashingMachine;
import manager.ResourceManager;
import manager.StatisticsManager;
import util.ConfigLoader;
import util.Logger;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents one customer task executed by the thread pool.
 * Workflow: arrive -> wash -> dry -> pay -> complete.
 *
 * Resources are acquired and released one at a time to avoid
 * holding one resource while waiting for another.
 */
public class Customer implements Runnable {
    private final int id;
    private final ResourceManager resources;
    private final StatisticsManager stats;
    private final SimulationBus bus;
    private final AtomicInteger completedCount;
    private final int totalCustomers;
    private volatile boolean completed = false;
    public Customer(int id, ResourceManager resources, StatisticsManager stats, SimulationBus bus,
                    AtomicInteger completedCount,
                    int totalCustomers
    ) {this.id = id;this.resources = resources;this.stats = stats;this.bus = bus;
        this.completedCount = completedCount;this.totalCustomers = totalCustomers;}
    @Override
    public void run() {long arrivalTime = System.currentTimeMillis();
        stats.recordArrival();Logger.log("Customer-" + id + " arrived.");

        try {

            // ------------------------------------------------------
            // STAGE 1: WASH
            // ------------------------------------------------------

            boolean washed = false;
            while (!washed) {
                WashingMachine washer =
                        resources.acquireWasher();
                try {
                    washer.wash(id, bus);
                    washed = true;
                } catch (MachineFailureException e) {
                    Logger.log(
                            "Customer-" + id
                                    + " wash failed on "
                                    + washer
                                    + ", retrying.");
                    stats.recordWasherFailure();
                } finally {
                    resources.releaseWasher(washer);}
                if (!washed) {
                    Thread.sleep(
                            ConfigLoader.getRetryDelay());}}

            // ------------------------------------------------------
            // STAGE 2: DRY
            // ------------------------------------------------------

            Dryer dryer =
                    resources.acquireDryer();

            try {

                dryer.dry(id, bus);

            } finally {

                resources.releaseDryer(dryer);
            }

            // ------------------------------------------------------
            // STAGE 3: PAYMENT
            // ------------------------------------------------------

            boolean paid = false;
            boolean gaveUp = false;

            while (!paid && !gaveUp) {

                PaymentKiosk kiosk = null;

                try {

                    kiosk = resources.acquireKiosk();

                    kiosk.processPayment(id, bus);

                    paid = true;

                } catch (KiosksClosedException e) {

                    gaveUp = true;

                    stats.recordAbandoned();

                    Logger.log(
                            "Customer-" + id
                                    + " gave up waiting for payment - "
                                    + "kiosks are down for the day."
                    );

                } catch (PaymentFailureException e) {

                    Logger.log(
                            "Customer-" + id
                                    + " payment failed on "
                                    + kiosk
                                    + ", retrying."
                    );

                    stats.recordPaymentFailure();

                } finally {

                    if (kiosk != null) {
                        resources.releaseKiosk(kiosk);
                    }
                }

                if (!paid && !gaveUp) {
                    Thread.sleep(
                            ConfigLoader.getRetryDelay()
                    );
                }
            }

            // ------------------------------------------------------
            // STAGE 4: COMPLETION
            // ------------------------------------------------------

            if (paid && !completed) {

                completed = true;

                long totalTime =
                        System.currentTimeMillis()
                                - arrivalTime;

                stats.recordCompletion(totalTime);

                Logger.log(
                        "Customer-" + id
                                + " done in "
                                + totalTime
                                + " ms."
                );

                int completedNow =
                        completedCount.incrementAndGet();

                if (completedNow == totalCustomers) {

                    Logger.log(
                            "All "
                                    + totalCustomers
                                    + " customers have completed!"
                    );
                }
            }

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            Logger.log(
                    "Customer-" + id + " interrupted."
            );
        }
    }
}