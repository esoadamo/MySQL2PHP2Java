package aho.util.mysql2PHP2Java.exceptions;

public class InvalidStateException extends Exception {
    private static final long serialVersionUID = -350865859881283569L;

    public InvalidStateException() {
	super();
    }

    public InvalidStateException(String message) {
	super(message);
    }

    public InvalidStateException(String message, Throwable cause) {
	super(message, cause);
    }

    public InvalidStateException(Throwable cause) {
	super(cause);
    }
}
