package event;                                          // This file belongs to the 'event' package (folder)

import java.util.List;                                  // The List interface - ordered collection of items
import java.util.concurrent.CopyOnWriteArrayList;       // A thread-safe List implementation (see explanation below)

/**
 * Publishes simulation events to all registered listeners.
 * Classes can subscribe via addListener() and will be notified
 * whenever a machine becomes busy, idle, failed, or when
 * congestion starts or resolves.
 *
 * The SimulationBus knows nothing about Swing - it just forwards
 * events. This keeps the concurrency-critical classes free of any
 * GUI dependency.
 */
public class SimulationBus {                            // Public class - any package can use it

    // A list of every object that wants to receive events.
    // CopyOnWriteArrayList is chosen because:
    //   - Listeners are added ONCE at startup (rare writes)
    //   - Events are fired MANY times per second (frequent reads)
    //   - CopyOnWriteArrayList gives lock-free iteration for readers,
    //     so iterating while a new listener is added is impossible to break
    private final List<SimulationListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * Registers a new listener.
     * Called once from Main after the GUI is built:
     *     bus.addListener(frame);
     */
    public void addListener(SimulationListener listener) {
        listeners.add(listener);                        // Add to the internal thread-safe list
    }

    /**
     * Fires a "machine busy" event to every registered listener.
     * Called from WashingMachine, Dryer, and PaymentKiosk when a
     * customer starts using a machine.
     */
    public void machineBusy(MachineType type, int machineId, int customerId, long duration) {
        for (SimulationListener l : listeners) {        // Loop over every listener (thread-safe iteration)
            l.onMachineBusy(type, machineId, customerId, duration);   // Call the listener's callback
        }
    }

    /**
     * Fires a "machine idle" event when a machine is released.
     * Called from ResourceManager.releaseWasher/releaseDryer/releaseKiosk.
     */
    public void machineIdle(MachineType type, int machineId) {
        for (SimulationListener l : listeners) {        // Same pattern as above
            l.onMachineIdle(type, machineId);
        }
    }

    /**
     * Fires a "machine failed" event when a machine throws an exception.
     * Called from WashingMachine.wash() and PaymentKiosk.processPayment()
     * right before the exception is thrown.
     */
    public void machineFailed(MachineType type, int machineId, int customerId) {
        for (SimulationListener l : listeners) {
            l.onMachineFailed(type, machineId, customerId);
        }
    }

    /**
     * Fires a "congestion started" event (bonus scenario).
     * Called from ResourceManager.checkOwnerCall() the FIRST time
     * the payment queue reaches 30. Guarded by AtomicBoolean so it
     * only fires once per run.
     */
    public void congestionStarted() {
        for (SimulationListener l : listeners) {
            l.onCongestionStarted();
        }
    }

    /**
     * Fires a "congestion resolved" event.
     * Currently not called in this version (the congested scenario
     * ends with all customers abandoning rather than kiosks being
     * repaired), but the method is kept for symmetry and future use.
     */
    public void congestionResolved() {
        for (SimulationListener l : listeners) {
            l.onCongestionResolved();
        }
    }
}