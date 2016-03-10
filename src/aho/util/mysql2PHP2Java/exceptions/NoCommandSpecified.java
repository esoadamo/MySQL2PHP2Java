package aho.util.mysql2PHP2Java.exceptions;

public class NoCommandSpecified extends Exception {
    private static final long serialVersionUID = -350865859881283569L;

    public NoCommandSpecified() {
	super();
    }

    public NoCommandSpecified(String message) {
	super(message);
    }

    public NoCommandSpecified(String message, Throwable cause) {
	super(message, cause);
    }

    public NoCommandSpecified(Throwable cause) {
	super(cause);
    }
}
