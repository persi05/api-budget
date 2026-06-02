package pl.patryk.budget_api.common;

public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
