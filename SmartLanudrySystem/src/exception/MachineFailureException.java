package exception;

public class MachineFailureException extends Exception {
    public MachineFailureException(String message) {
        super(message);
    }
}