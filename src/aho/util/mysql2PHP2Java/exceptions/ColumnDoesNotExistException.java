package aho.util.mysql2PHP2Java.exceptions;

public class ColumnDoesNotExistException extends Exception {
    private static final long serialVersionUID = -350865859881283569L;

    public ColumnDoesNotExistException() {
	super();
    }

    public ColumnDoesNotExistException(String message) {
	super(message);
    }

    public ColumnDoesNotExistException(String message, Throwable cause) {
	super(message, cause);
    }

    public ColumnDoesNotExistException(Throwable cause) {
	super(cause);
    }
}
