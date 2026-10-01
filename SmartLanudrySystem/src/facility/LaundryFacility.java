package facility;                                       // Same facility package

import util.ConfigLoader;                               // Reads counts from config.properties (6, 4, 2)

import java.util.ArrayList;                             // A standard growable list implementation
import java.util.List;                                  // The List interface (ordered collection)

/**
 * Builds and owns the physical equipment of the laundromat:
 *   - All washing machines
 *   - All dryers
 *   - All payment kiosks
 *
 * It is built ONCE at startup. The ResourceManager takes the lists returned
 * by the getters and turns them into thread-safe pools.
 */
public class LaundryFacility {
    private final List<WashingMachine> washers = new ArrayList<>();   // Holds WashingMachine objects
    private final List<Dryer> dryers = new ArrayList<>();             // Holds Dryer objects
    private final List<PaymentKiosk> kiosks = new ArrayList<>();      // Holds PaymentKiosk objects

    /**
     * Constructor - creates the machines based on the counts in config.properties.
     * Called once by ResourceManager.
     */
    public LaundryFacility() {
        for (int i = 1; i <= ConfigLoader.getWasherCount(); i++) {     // Loop from 1 to 6 (default)
            washers.add(new WashingMachine(i));                        // Create Washer-1 through Washer-6
        }
        for (int i = 1; i <= ConfigLoader.getDryerCount(); i++) {      // Loop from 1 to 4
            dryers.add(new Dryer(i));                                  // Create Dryer-1 through Dryer-4
        }
        for (int i = 1; i <= ConfigLoader.getKioskCount(); i++) {      // Loop from 1 to 2
            kiosks.add(new PaymentKiosk(i));                           // Create Kiosk-1 and Kiosk-2
        }
    }

    // ---- Getters ----
    // ResourceManager uses these once at startup to fill its three ConcurrentLinkedQueues.

    public List<WashingMachine> getWashers() { return washers; }      // Return the list of washers
    public List<Dryer> getDryers() { return dryers; }                 // Return the list of dryers
    public List<PaymentKiosk> getKiosks() { return kiosks; }          // Return the list of kiosks
}