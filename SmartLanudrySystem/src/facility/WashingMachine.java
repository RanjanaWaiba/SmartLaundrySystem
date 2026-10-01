package facility;

import event.MachineType;
import event.SimulationBus;
import exception.MachineFailureException;
import util.ConfigLoader;
import util.Logger;
import util.RandomUtil;
import util.SpeedController;

/**
 * Represents one washing machine.
 * A washer can fail mid-cycle with the configured failure probability.
 */
public class WashingMachine {

    private final int id;

    public WashingMachine(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    /**
     * Performs a washing cycle and reports its state to the GUI.
     */
    public void wash(int customerId, SimulationBus bus)
            throws MachineFailureException, InterruptedException {

        int duration = RandomUtil.washDurationMillis();

        Logger.log("Customer-" + customerId
                + " START washing on Washer-" + id
                + " for approximately " + duration + " ms.");

        bus.machineBusy(MachineType.WASHER, id, customerId, duration);

        // Decide whether this cycle fails.
        boolean fail = RandomUtil.shouldFail(
                ConfigLoader.getWasherFailRate()
        );

        if (fail) {
            long halfDuration = Math.max(1, duration / 2);

            Thread.sleep(SpeedController.applySpeed(halfDuration));

            bus.machineFailed(MachineType.WASHER, id, customerId);

            Logger.log("Customer-" + customerId
                    + " FAILED mid-cycle on Washer-" + id + ".");

            throw new MachineFailureException(
                    "Washer-" + id
                            + " failed mid-cycle for Customer-"
                            + customerId
            );
        }

        // Normal washing completion.
        Thread.sleep(SpeedController.applySpeed(duration));

        Logger.log("Customer-" + customerId
                + " FINISHED washing on Washer-" + id + ".");
    }

    @Override
    public String toString() {
        return "Washer-" + id;
    }
}