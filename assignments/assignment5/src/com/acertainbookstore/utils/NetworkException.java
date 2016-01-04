package com.acertainbookstore.utils;

/**
 * Exception to signal a book store NETWORK error
 */
public class NetworkException extends Exception {
    private static final long serialVersionUID = 1L;

    public NetworkException() {
        super();
    }

    public NetworkException(String message) {
        super(message);
    }

    public NetworkException(String message, Throwable cause) {
        super(message, cause);
    }

    public NetworkException(Throwable ex) {
        super(ex);
    }
}