// Copyright (c) 2014 Project Iris. All rights reserved.
//
// The current language binding is an official support library of the Iris
// cloud messaging framework, and as such, the same licensing terms apply.
// For details please see http://iris.karalabe.com/downloads#License
package com.karalabe.iris.exceptions;

/**
 * Thrown if the user fails to initialize its own service.
 */
public class InitializationException extends Exception {
    /** Constructs an InitializationException with null as its error detail message. */
    public InitializationException() {
    }

    /**
     * Constructs an InitializationException with the specified detail message.
     * @param message detail message (which is saved for later retrieval by the Throwable.getMessage() method)
     */
    public InitializationException(String message) {
        super(message);
    }

    /**
     * Constructs an InitializationException with the specified detail message and cause.
     * @param message detail message (which is saved for later retrieval by the Throwable.getMessage() method)
     * @param cause   cause (which is saved for later retrieval by the Throwable.getCause() method). (A null value is permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public InitializationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs an InitializationException with the specified cause and a detail
     * message of (cause==null ? null : cause.toString()) (which typically
     * contains the class and detail message of cause).
     * @param cause cause (which is saved for later retrieval by the Throwable.getCause() method). (A null value is permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public InitializationException(Throwable cause) {
        super(cause);
    }
}
