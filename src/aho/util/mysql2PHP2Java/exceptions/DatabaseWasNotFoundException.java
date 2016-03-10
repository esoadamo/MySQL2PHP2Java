package aho.util.mysql2PHP2Java.exceptions;

public class DatabaseWasNotFoundException extends Exception {
    private static final long serialVersionUID = -350865859881283569L;

    public DatabaseWasNotFoundException() {
	super();
    }

    public DatabaseWasNotFoundException(String message) {
	super(message);
    }

    public DatabaseWasNotFoundException(String message, Throwable cause) {
	super(message, cause);
    }

    public DatabaseWasNotFoundException(Throwable cause) {
	super(cause);
    }
}
