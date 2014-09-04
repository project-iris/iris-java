// Copyright (c) 2014 Project Iris. All rights reserved.
//
// The current language binding is an official support library of the Iris
// cloud messaging framework, and as such, the same licensing terms apply.
// For details please see http://iris.karalabe.com/downloads#License
package com.karalabe.iris.exceptions;

// Thrown whenever a time-limited operation expires.
public class TimeoutException extends Exception {
    // Constructs a TimeoutException with null as its error detail message.
    public TimeoutException() {
    }

    // Constructs a TimeoutException with the specified detail message.
    public TimeoutException(String message) {
        super(message);
    }

    // Constructs a TimeoutException with the specified detail message and cause.
    public TimeoutException(String message, Throwable throwable) {
        super(message, throwable);
    }

    // Constructs a TimeoutException with the specified cause and a detail
    // message of (cause==null ? null : cause.toString()) (which typically
    // contains the class and detail message of cause).
    public TimeoutException(Throwable throwable) {
        super(throwable);
    }
}
