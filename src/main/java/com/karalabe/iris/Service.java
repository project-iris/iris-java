// Copyright (c) 2014 Project Iris. All rights reserved.
//
// The current language binding is an official support library of the Iris
// cloud messaging framework, and as such, the same licensing terms apply.
// For details please see http://iris.karalabe.com/downloads#License
package com.karalabe.iris;

import com.karalabe.iris.common.ContextualLogger;
import com.karalabe.iris.exceptions.InitializationException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

// Service instance belonging to a particular cluster in the network.
public class Service implements AutoCloseable {
    private static final AtomicInteger nextServId = new AtomicInteger(); // Id to assign to the next service

    private final Connection connection; // Network connection to the local Iris relay

    // Connects to the Iris network and registers a new service instance as a member of the
    // specified service cluster, overriding the default quality of service limits.
    Service(final int port, @NotNull final String cluster, @NotNull final ServiceHandler handler, @NotNull final ServiceLimits limits) throws IOException, InterruptedException, InitializationException {
        final ContextualLogger logger = new ContextualLogger(LoggerFactory.getLogger(Iris.class.getPackage().getName()),
                                                             "service", String.valueOf(nextServId.incrementAndGet()));

        try {
            // Inject the logger context and try to execute the registration
            logger.loadContext();
            logger.info("Registering new service",
                        "relay_port", String.valueOf(port), "cluster", cluster,
                        "broadcast_limits", String.format("%dT|%dB", limits.broadcastThreads, limits.broadcastMemory),
                        "request_limits", String.format("%dT|%dB", limits.requestThreads, limits.requestMemory));

            connection = new Connection(port, cluster, handler, limits, logger);
            try {
                handler.init(connection);
                logger.info("Service registration completed");
            } catch (InitializationException e) {
                logger.warn("User failed to initialize service", "reason", e.getMessage());
                connection.close();
                throw e;
            }
        } catch (IOException | InterruptedException e) {
            logger.warn("Failed to register new service", "reason", e.getMessage());
            throw e;
        } finally {
            // Ensure the caller isn't polluted with the service context
            logger.unloadContext();
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
