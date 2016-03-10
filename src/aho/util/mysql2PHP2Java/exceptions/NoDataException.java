package aho.util.mysql2PHP2Java.exceptions;

public class NoDataException extends Exception {
    private static final long serialVersionUID = -350865859881283569L;

    public NoDataException() {
	super();
    }

    public NoDataException(String message) {
	super(message);
    }

    public NoDataException(String message, Throwable cause) {
	super(message, cause);
    }

    public NoDataException(Throwable cause) {
	super(cause);
    }
}
