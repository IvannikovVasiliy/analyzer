package ru.neoflex.scammertracking.analyzer.error.exception;

public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }

    public NotFoundException(Throwable cause) {
        super(cause);
    }

    @Override
    public String toString() {
        return "NotFoundException: " + getMessage();
    }

}
