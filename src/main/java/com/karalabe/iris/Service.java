/*
 * Copyright © 2014 Project Iris. All rights reserved.
 *
 * The current language binding is an official support library of the Iris cloud messaging framework, and as such, the same licensing terms apply.
 * For details please see http://iris.karalabe.com/downloads#License
 */

package com.karalabe.iris;

import com.karalabe.iris.protocol.Validators;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Service instance belonging to a particular cluster in the network. */
public class Service implements AutoCloseable {
    private final Connection connection; // Network connection to the local Iris relay

    /** Connects to the Iris network and registers a new service instance as a member of the specified service cluster, overriding the default quality of service limits. */
    Service(final int port, @NotNull final String cluster, @NotNull final ServiceHandler handler, @Nullable final ServiceLimits limits) {
        Validators.validateLocalClusterName(cluster);

        connection = new Connection(port, cluster, handler, limits);
        handler.init(connection); // TODO ugly, init should be replaced with constructor and affected fields made final. We should strive for immutable objects.
    }

    /** Unregisters the service instance from the Iris network. */
    @Override public void close() throws InterruptedException {
        connection.close();
    }
}
