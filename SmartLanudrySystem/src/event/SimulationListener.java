package event;                                          // This file belongs to the 'event' package (folder)

/**
 * The subscriber contract for the SimulationBus.
 *
 * Any class that wants to receive simulation events must implement
 * this interface and register itself with the bus via
 * bus.addListener(...).
 *
 * In our project, LaundryFrame (the Swing GUI) is the only class that
 * implements this interface. But the interface does not mention Swing
 * anywhere - that is intentional. It could equally be implemented by
 * a console logger, a CSV writer, or a test mock, because the concurrency
 * code should not depend on how events are displayed.
 *
 * This is the classic Observer design pattern:
 *   - SimulationBus  = the publisher
 *   - SimulationListener = the subscriber contract
 *   - onMachineBusy / onMachineIdle / etc. = the callbacks
 */
public interface SimulationListener {                   // 'interface' = a pure contract with no implementation

    /**
     * Called when a machine begins serving a customer.
     *
     * @param type         which kind of machine (WASHER, DRYER, KIOSK)
     * @param machineId    which specific machine (e.g. 3 for Washer-3)
     * @param customerId   the customer now using it (e.g. 7 for Customer-7)
     * @param durationMillis   how long the cycle will take (ms)
     *
     * Fired from WashingMachine.wash(), Dryer.dry(), PaymentKiosk.processPayment()
     * just BEFORE the Thread.sleep() that models the cycle.
     */
    void onMachineBusy(MachineType type, int machineId, int customerId, long durationMillis);

    /**
     * Called when a machine is released back to the pool.
     *
     * @param type         which kind of machine
     * @param machineId    which specific machine
     *
     * Fired from ResourceManager.releaseWasher() / releaseDryer() / releaseKiosk()
     * after the machine has been returned to its ConcurrentLinkedQueue and
     * its semaphore permit released.
     */
    void onMachineIdle(MachineType type, int machineId);

    /**
     * Called when a machine fails mid-cycle.
     *
     * @param type         which kind of machine
     * @param machineId    which specific machine failed
     * @param customerId   the customer who was using it
     *
     * Fired from WashingMachine.wash() / PaymentKiosk.processPayment()
     * right BEFORE the exception is thrown. This lets the GUI show the
     * red failure state on the tile even though the machine is about to
     * be released by the customer's finally block.
     */
    void onMachineFailed(MachineType type, int machineId, int customerId);

    /**
     * Called the FIRST time the payment queue reaches the congestion
     * threshold (30 customers) in the bonus scenario.
     *
     * Fired from ResourceManager.checkOwnerCall() - guarded by
     * AtomicBoolean.compareAndSet(false, true) so it fires exactly
     * once per run, even if dozens of threads race to trigger it.
     *
     * The GUI reacts by showing the red "OWNER CALLED IN!" banner.
     */
    void onCongestionStarted();

    /**
     * Called when congestion is resolved.
     *
     * In the current version this is not fired (the congested scenario
     * ends with all customers abandoning rather than kiosks coming
     * back online), but the method is kept for symmetry and future
     * extension. Implementations must still provide it - even as an
     * empty method - because Java interfaces require every declared
     * method to be implemented.
     */
    void onCongestionResolved();
}