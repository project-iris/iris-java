// Copyright (c) 2014 Project Iris. All rights reserved.
//
// The current language binding is an official support library of the Iris
// cloud messaging framework, and as such, the same licensing terms apply.
// For details please see http://iris.karalabe.com/downloads#License
package com.karalabe.iris.exceptions;

// Thrown if the user fails to initialize its own service.
public class InitializationException extends Exception {
    // Constructs an InitializationException with null as its error detail message.
    public InitializationException() {
    }

    // Constructs an InitializationException with the specified detail message.
    public InitializationException(String message) {
        super(message);
    }

    // Constructs an InitializationException with the specified detail message and cause.
    public InitializationException(String message, Throwable throwable) {
        super(message, throwable);
    }

    // Constructs an InitializationException with the specified cause and a detail
    // message of (cause==null ? null : cause.toString()) (which typically
    // contains the class and detail message of cause).
    public InitializationException(Throwable throwable) {
        super(throwable);
    }
}
