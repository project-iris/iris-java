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

public final class Iris {
    private static final AtomicInteger nextConnId = new AtomicInteger(); // Id to assign to the next connection

    private Iris() {}

    // Connects to the Iris network as a simple client.
    public static Connection connect(final int port) throws IOException {
        final ContextualLogger logger = new ContextualLogger(LoggerFactory.getLogger(Iris.class.getPackage().getName()),
                                                             "client", String.valueOf(nextConnId.incrementAndGet()));

        try {
            // Inject the logger context and try to establish the connection
            logger.loadContext();
            logger.info("Connecting new client", "relay_port", String.valueOf(port));

            final Connection conn = new Connection(port, "", null, null, logger);
            logger.info("Client connection established");

            return conn;
        } catch (IOException e) {
            logger.warn("Failed to connect new client", "reason", e.getMessage());
            throw e;
        } finally {
            // Ensure the caller isn't polluted with the client context
            logger.unloadContext();
        }
    }

    // Connects to the Iris network and registers a new service instance as a
    // member of the specified service cluster.
    public static Service register(final int port, @NotNull final String cluster, @NotNull final ServiceHandler handler) throws IOException, InterruptedException, InitializationException {
        return new Service(port, cluster, handler, new ServiceLimits());
    }

    // Connects to the Iris network and registers a new service instance as a
    // member of the specified service cluster, overriding the default quality
    // of service limits.
    public static Service register(final int port, @NotNull final String cluster, @NotNull final ServiceHandler handler, @NotNull final ServiceLimits limits) throws IOException, InterruptedException, InitializationException {
        return new Service(port, cluster, handler, limits);
    }
}
