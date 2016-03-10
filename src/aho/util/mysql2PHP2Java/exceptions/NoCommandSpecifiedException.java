package aho.util.mysql2PHP2Java.exceptions;

public class NoCommandSpecifiedException extends Exception {
    private static final long serialVersionUID = -350865859881283569L;

    public NoCommandSpecifiedException() {
	super();
    }

    public NoCommandSpecifiedException(String message) {
	super(message);
    }

    public NoCommandSpecifiedException(String message, Throwable cause) {
	super(message, cause);
    }

    public NoCommandSpecifiedException(Throwable cause) {
	super(cause);
    }
}
