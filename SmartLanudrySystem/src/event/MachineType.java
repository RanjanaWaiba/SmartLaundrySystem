package event;                                          // This file belongs to the 'event' package (folder)

/**
 * An enum listing the three types of machine in our simulation.
 * Enums are a fixed list of named constants - you cannot create new ones at runtime.
 * We use it so that WASHER, DRYER and KIOSK are the only valid machine types,
 * which prevents typos like "WASHERR" or "Kisk" from ever compiling.
 */
public enum MachineType {                               // 'enum' = a special class whose only instances are the ones listed below

    WASHER("🧺"), DRYER("🌀"), KIOSK("💳");              // The three possible values.
    // Each value calls the private constructor below with its icon string.
    // The semicolon ';' is required after the last constant when
    // there is more code (fields/methods) after it.

    private final String icon;                           // Each enum constant stores one icon (emoji).
    // 'private' = only this enum can read it
    // 'final' = can never be reassigned after construction

    MachineType(String icon) {                           // The enum's constructor.
        // It runs ONCE for each constant - so 3 times total:
        //   1st call: WASHER with "🧺"
        //   2nd call: DRYER  with "🌀"
        //   3rd call: KIOSK  with "💳"
        // Note: enum constructors are implicitly private.
        this.icon = icon;                                // Store the argument into the field
    }

    public String getIcon() { return icon; }             // Getter - lets other classes read the icon.
    // Example: MachineType.WASHER.getIcon() returns "🧺"
}