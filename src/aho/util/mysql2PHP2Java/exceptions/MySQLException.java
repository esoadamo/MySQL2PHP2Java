package aho.util.mysql2PHP2Java.exceptions;

public class MySQLException extends Exception {
    private static final long serialVersionUID = -350865859881284569L;

    public MySQLException() {
	super();
    }

    public MySQLException(String message) {
	super(message);
    }

    public MySQLException(String message, Throwable cause) {
	super(message, cause);
    }

    public MySQLException(Throwable cause) {
	super(cause);
    }
}
