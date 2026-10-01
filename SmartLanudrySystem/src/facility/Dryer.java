package facility;                                       // This file belongs to the 'facility' package (folder)
import event.MachineType;
import event.SimulationBus;                             // Needed to notify the GUI when this dryer is busy
import util.SpeedController;                            // Applies the speed-multiplier from the GUI slider
import util.RandomUtil;
/**
 * Models ONE physical dryer.
 * The dryer itself does no synchronisation - it just models a single cycle.
 * The ResourceManager is responsible for ensuring only one customer uses
 * this dryer at a time (via its semaphore + queue).
 */
public class Dryer {
    private final int id;                               // This dryer's ID (1 to 4 in the default config)

    public Dryer(int id) { this.id = id; }              // Constructor - store the ID
    public int getId() { return id; }                   // Getter so ResourceManager and GUI can identify it

    /**
     * Runs one drying cycle for the given customer.
     *
     * @param customerId  the customer using this dryer
     * @param bus         the event bus, so we can tell the GUI "I am busy"
     */
    public void dry(int customerId, SimulationBus bus) throws InterruptedException {
        int duration = RandomUtil.dryDurationMillis();  // Pick a random duration (3000–5000 ms from config)
        bus.machineBusy(MachineType.DRYER, id, customerId, duration);  // Tell the GUI "this dryer is now busy"
        Thread.sleep(SpeedController.applySpeed(duration));            // Sleep for the duration (scaled by speed slider)
    }                                                                  // When the sleep ends, the method returns normally -
    // there is no failure path for a dryer in this simulation

    /**
     * Friendly name used in logs and by toString(). Println calls this automatically.
     */
    @Override
    public String toString() {
        return "Dryer-" + id;                           // e.g. "Dryer-3"
    }
}