package se.transport.bus.exception;

public class InternalServerError extends RuntimeException {

    public InternalServerError(String message) {
        super(message);
    }
}
