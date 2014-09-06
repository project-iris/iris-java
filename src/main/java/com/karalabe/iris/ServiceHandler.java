// Copyright (c) 2014 Project Iris. All rights reserved.
//
// The current language binding is an official support library of the Iris
// cloud messaging framework, and as such, the same licensing terms apply.
// For details please see http://iris.karalabe.com/downloads#License
package com.karalabe.iris;

import com.karalabe.iris.exceptions.InitializationException;
import com.karalabe.iris.exceptions.RemoteException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

// Callback interface for processing inbound messages designated to a particular
// service instance.
public interface ServiceHandler {
    // Called once after the service is registered in the Iris network, but before
    // and handlers are activated. Its goal is to initialize any internal state
    // dependent on the connection.
    default void init(@NotNull final Connection connection) throws InitializationException {}

    // Callback invoked whenever a broadcast message arrives designated to the
    // cluster of which this particular service instance is part of.
    default void handleBroadcast(@NotNull final byte[] message) {}

    // Callback invoked whenever a request designated to the service's cluster is
    // load-balanced to this particular service instance.
    //
    // The method should service the request and return either a reply or the
    // error encountered, which will be delivered to the request originator.
    //
    // Returning nil for both or none of the results will result in a panic. Also,
    // since the requests cross language boundaries, only the error string gets
    // delivered remotely (any associated type information is effectively lost).
    default byte[] handleRequest(@NotNull final byte[] request) throws RemoteException {
        throw new RemoteException("Not implemented!");
    }

    // Callback invoked whenever a tunnel designated to the service's cluster is
    // constructed from a remote node to this particular instance.
    default void handleTunnel(@NotNull final Tunnel tunnel) {
        try {
            tunnel.close();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    // Callback notifying the service that the local relay dropped its connection.
    default void handleDrop(@NotNull final Exception e) {
        throw new RuntimeException(e);
    }
}
