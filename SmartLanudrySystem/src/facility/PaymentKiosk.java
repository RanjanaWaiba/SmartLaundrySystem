
        package facility;

import event.MachineType;
import event.SimulationBus;
import exception.PaymentFailureException;
import util.ConfigLoader;
import util.Logger;
import util.RandomUtil;
import util.SpeedController;

/**
 * Represents one physical payment kiosk.
 *
 * The ResourceManager controls mutual exclusion using a Semaphore.
 * Therefore only the configured number of kiosks can be used
 * concurrently.
 *
 * Each payment has a 5% probability of failure.
 * Customer.run() catches the exception and retries after 2 seconds.
 */
public class PaymentKiosk {

    private final int id;

    public PaymentKiosk(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    /**
     * Performs one payment operation.
     */
    public void processPayment(
            int customerId,
            SimulationBus bus)
            throws PaymentFailureException, InterruptedException {

        int duration =
                RandomUtil.paymentDurationMillis();

        boolean fail =
                RandomUtil.shouldFail(
                        ConfigLoader.getPaymentFailRate()
                );

        Logger.log(
                "Customer-" + customerId
                        + " START payment at Kiosk-" + id
                        + " (" + duration + " ms)."
        );

        bus.machineBusy(
                MachineType.KIOSK,
                id,
                customerId,
                duration
        );

        /*
         * Simulate payment processing.
         */
        Thread.sleep(
                SpeedController.applySpeed(
                        duration
                )
        );

        /*
         * Simulate the 5% kiosk/payment failure.
         */
        if (fail) {

            bus.machineFailed(
                    MachineType.KIOSK,
                    id,
                    customerId
            );

            Logger.log(
                    "Customer-" + customerId
                            + " PAYMENT FAILED at Kiosk-" + id
                            + "."
            );

            throw new PaymentFailureException(
                    "Kiosk-" + id
                            + " payment failure for Customer-"
                            + customerId
            );
        }

        Logger.log(
                "Customer-" + customerId
                        + " PAYMENT SUCCESS at Kiosk-" + id + "."
        );
    }

    @Override
    public String toString() {
        return "Kiosk-" + id;
    }
}

