import customer.Customer;
import event.SimulationBus;
import gui.LaundryFrame;
import manager.ResourceManager;
import manager.StatisticsManager;
import util.ConfigLoader;
import util.Logger;
import util.RandomUtil;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Entry point for the Smart Laundry Facility simulation.
 *
 * Customers are admitted for the configured arrival window and
 * execute concurrently using an ExecutorService.
 */
public class Main {

    public static void main(String[] args) {

        // Detect bonus congested mode.
        boolean congested = false;

        for (String arg : args) {
            if ("--congested".equalsIgnoreCase(arg)) {
                congested = true;
                break;
            }
        }

        long startTime = System.currentTimeMillis();

        StatisticsManager stats =
                new StatisticsManager();

        SimulationBus bus =
                new SimulationBus();

        ResourceManager resources =
                new ResourceManager(
                        stats,
                        bus,
                        congested
                );

        LaundryFrame frame =
                buildGUI(
                        stats,
                        resources,
                        startTime,
                        congested
                );

        bus.addListener(frame);

        int maximumCustomers =
                ConfigLoader.getCustomerCount();

        int simulationSeconds =
                ConfigLoader.getSimulationSeconds();

        /*
         * The arrival deadline controls admission only.
         * Customers already admitted are allowed to finish.
         */
        long arrivalDeadline = startTime + simulationSeconds * 1000L;
        ExecutorService executor = Executors.newCachedThreadPool();
        AtomicInteger completedCount = new AtomicInteger(0);
        Logger.log("Simulation starting with up to " + maximumCustomers + " customers." + (congested
                        ? " [CONGESTED SCENARIO]" : ""));
        Logger.log("Customer arrival window: " + simulationSeconds + " seconds.");
        Logger.log("Arrival interval: " + ConfigLoader.getArrivalMin() + "-" + ConfigLoader.getArrivalMax() + " ms.");
        // Admit customers until the population or arrival window is reached.
        int customerId = 1;
        while (customerId <= maximumCustomers && System.currentTimeMillis() < arrivalDeadline) {
            Customer customer = new Customer(customerId, resources, stats, bus, completedCount, maximumCustomers);
            executor.execute(customer);
            Logger.log("ENTRY GATE -> Customer-" + customerId + " entered the laundromat.");

            customerId++;

            if (customerId <= maximumCustomers) {

                long remainingTime =
                        arrivalDeadline
                                - System.currentTimeMillis();

                if (remainingTime <= 0) {
                    break;
                }

                long arrivalDelay =
                        RandomUtil.arrivalGapMillis();

                arrivalDelay =
                        Math.min(
                                arrivalDelay,
                                remainingTime
                        );

                try {

                    Thread.sleep(
                            arrivalDelay
                    );

                } catch (InterruptedException e) {

                    Thread.currentThread().interrupt();

                    Logger.log(
                            "Customer arrival process interrupted."
                    );

                    break;
                }
            }
        }

        int customersCreated =
                customerId - 1;

        Logger.log(
                "================================================"
        );

        Logger.log(
                "CUSTOMER ARRIVAL WINDOW CLOSED"
        );

        Logger.log(
                "Customers arrived: "
                        + customersCreated
                        + " / maximum "
                        + maximumCustomers
        );

        Logger.log(
                "No more customers will enter the facility."
        );

        Logger.log(
                "Customers already inside will continue until "
                        + "their process is completed."
        );

        Logger.log(
                "================================================"
        );

        /*
         * shutdown() prevents new tasks but allows submitted
         * customer tasks to continue running.
         */
        executor.shutdown();

        try {

            boolean finished =
                    executor.awaitTermination(
                            60,
                            TimeUnit.SECONDS
                    );

            if (!finished) {

                Logger.log(
                        "Some customers did not finish within "
                                + "the completion period."
                );

                executor.shutdownNow();
            }

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            Logger.log(
                    "Main thread interrupted while waiting "
                            + "for customers to finish."
            );

            executor.shutdownNow();
        }

        long totalRuntime =
                System.currentTimeMillis()
                        - startTime;

        Logger.log(
                "Simulation finished in "
                        + (totalRuntime / 1000.0)
                        + " seconds."
        );

        // Print and export final statistics.
        stats.printReport();

        String filename =
                "laundry_stats_"
                        + System.currentTimeMillis()
                        + ".csv";

        stats.exportCSV(filename);

        SwingUtilities.invokeLater(
                frame::refreshStats
        );

        Logger.log(
                "EXIT GATE -> Simulation completed."
        );

        Logger.log(
                "Done."
        );

        System.exit(0);
    }

    /**
     * Creates the GUI on the Swing Event Dispatch Thread.
     */
    private static LaundryFrame buildGUI(
            StatisticsManager stats,
            ResourceManager resources,
            long startTime,
            boolean congested
    ) {

        final LaundryFrame[] holder =
                new LaundryFrame[1];

        try {

            SwingUtilities.invokeAndWait(() -> {

                LaundryFrame frame =
                        new LaundryFrame(
                                stats,
                                resources,
                                startTime,
                                congested
                        );

                frame.setDefaultCloseOperation(
                        JFrame.EXIT_ON_CLOSE
                );

                frame.setVisible(true);

                holder[0] = frame;
            });

        } catch (Exception e) {

            Logger.log(
                    "Unable to create GUI: "
                            + e.getMessage()
            );

            e.printStackTrace();
        }

        return holder[0];
    }
}