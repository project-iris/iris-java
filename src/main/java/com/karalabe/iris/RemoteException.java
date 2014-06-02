// Copyright (c) 2014 Project Iris. All rights reserved.
//
// The current language binding is an official support library of the Iris
// cloud messaging framework, and as such, the same licensing terms apply.
// For details please see http://iris.karalabe.com/downloads#License
package com.karalabe.iris;

// Signals an exception that occurred in a remote node.
public class RemoteException extends Exception {
    public RemoteException(String message) {
        super(message);
    }
}
