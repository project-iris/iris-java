// Copyright (c) 2014 Project Iris. All rights reserved.
//
// The current language binding is an official support library of the Iris
// cloud messaging framework, and as such, the same licensing terms apply.
// For details please see http://iris.karalabe.com/downloads#License
package com.karalabe.iris.exceptions;

/**
 * Thrown whenever an operation is requested on- or interrupted by a closed
 * connection or tunnel.
 */
public class ClosedException extends Exception {
    /** Constructs a ClosedException with null as its error detail message. */
    public ClosedException() {
    }

    /**
     * Constructs a ClosedException with the specified detail message.
     * @param message detail message (which is saved for later retrieval by the Throwable.getMessage() method)
     */
    public ClosedException(String message) {
        super(message);
    }

    /**
     * Constructs a ClosedException with the specified detail message and cause.
     * @param message detail message (which is saved for later retrieval by the Throwable.getMessage() method)
     * @param cause   cause (which is saved for later retrieval by the Throwable.getCause() method). (A null value is permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public ClosedException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a ClosedException with the specified cause and a detail
     * message of (cause==null ? null : cause.toString()) (which typically
     * contains the class and detail message of cause).
     * @param cause cause (which is saved for later retrieval by the Throwable.getCause() method). (A null value is permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public ClosedException(Throwable cause) {
        super(cause);
    }
}
