// Copyright (c) 2014 Project Iris. All rights reserved.
//
// The current language binding is an official support library of the Iris
// cloud messaging framework, and as such, the same licensing terms apply.
// For details please see http://iris.karalabe.com/downloads#License
package com.karalabe.iris.exceptions;

// Thrown whenever an error is returned from a remote service.
public class RemoteException extends Exception {
    // Constructs a RemoteException with null as its error detail message.
    public RemoteException() {
    }

    // Constructs a RemoteException with the specified detail message.
    public RemoteException(String message) {
        super(message);
    }

    // Constructs a RemoteException with the specified detail message and cause.
    public RemoteException(String message, Throwable throwable) {
        super(message, throwable);
    }

    // Constructs a RemoteException with the specified cause and a detail
    // message of (cause==null ? null : cause.toString()) (which typically
    // contains the class and detail message of cause).
    public RemoteException(Throwable throwable) {
        super(throwable);
    }
}
