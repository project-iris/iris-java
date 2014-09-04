// Copyright (c) 2014 Project Iris. All rights reserved.
//
// The current language binding is an official support library of the Iris
// cloud messaging framework, and as such, the same licensing terms apply.
// For details please see http://iris.karalabe.com/downloads#License
package com.karalabe.iris;

import com.karalabe.iris.schemes.Validators;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

// Service instance belonging to a particular cluster in the network.
public class Service implements AutoCloseable {
    private final Connection connection; // Network connection to the local Iris relay

    // Connects to the Iris network and registers a new service instance as a member of the
    // specified service cluster, overriding the default quality of service limits.
    Service(final int port, @NotNull final String cluster, @NotNull final ServiceHandler handler, @Nullable final ServiceLimits limits) throws IOException, InterruptedException {
        Validators.validateLocalClusterName(cluster);

        connection = new Connection(port, cluster, handler, limits);
        try {
            handler.init(connection);
        } catch (Exception e) {
            connection.close();
        }
    }

    // Unregisters the service instance from the Iris network, removing all
    // subscriptions and closing all active tunnels.
    //
    // The call blocks until the tear-down is confirmed by the Iris node.
    @Override public void close() throws IOException, InterruptedException {
        connection.close();
    }
}
