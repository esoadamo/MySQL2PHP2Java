package aho.util.mysql2PHP2Java.exceptions;

public class InvalidPasswordException extends Exception {
    private static final long serialVersionUID = -350865859881283569L;

    public InvalidPasswordException() {
	super();
    }

    public InvalidPasswordException(String message) {
	super(message);
    }

    public InvalidPasswordException(String message, Throwable cause) {
	super(message, cause);
    }

    public InvalidPasswordException(Throwable cause) {
	super(cause);
    }
}
