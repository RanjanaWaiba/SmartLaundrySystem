package exception;

/**
 * Thrown by ResourceManager.acquireKiosk() only in the bonus "congested
 * scenario" (java Main --congested), once a customer has waited the
 * configured grace period against permanently unavailable kiosks and gives
 * up. Kept distinct from PaymentFailureException, which models a single
 * transient kiosk fault that is worth retrying - a KiosksClosedException
 * means retrying is pointless because the kiosks are down for the rest of
 * the simulated day.
 */
public class KiosksClosedException extends Exception {
    public KiosksClosedException(String message) {
        super(message);
    }
}
