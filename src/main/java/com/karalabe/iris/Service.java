// Copyright (c) 2014 Project Iris. All rights reserved.
//
// The current language binding is an official support library of the Iris
// cloud messaging framework, and as such, the same licensing terms apply.
// For details please see http://iris.karalabe.com/downloads#License
package com.karalabe.iris;

import com.karalabe.iris.protocols.Validators;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

// Service instance belonging to a particular cluster in the network.
public class Service implements AutoCloseable {
    private Connection conn; // Network connection to the local Iris relay

    // Connects to the Iris network and registers a new service instance as a member
    // of the specified service cluster.
    public Service(final int port, @NotNull final String cluster, @NotNull final ServiceHandler handler, final ServiceLimits limits) throws IOException {
        Validators.validateLocalClusterName(cluster);

        conn = new Connection(port, cluster, handler, limits);
        try {
            handler.init(conn);
        } catch (Exception e) {
            conn.close();
        }
    }

    // Unregisters the service instance from the Iris network.
    @Override public void close() throws IOException {
        conn.close();
    }
}
