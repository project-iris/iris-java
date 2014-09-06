// Copyright (c) 2014 Project Iris. All rights reserved.
//
// The current language binding is an official support library of the Iris
// cloud messaging framework, and as such, the same licensing terms apply.
// For details please see http://iris.karalabe.com/downloads#License
package com.karalabe.iris.schemes;

import com.karalabe.iris.ServiceHandler;
import com.karalabe.iris.ServiceLimits;
import com.karalabe.iris.common.BoundedThreadPool;
import com.karalabe.iris.common.ContextualLogger;
import com.karalabe.iris.protocol.RelayProtocol;

import java.io.IOException;

// Implements the broadcast communication pattern.
public class BroadcastScheme {
    private final RelayProtocol     protocol; // Network connection implementing the relay protocol
    private final ServiceHandler    handler;  // Callback handler for processing inbound broadcasts
    private final ServiceLimits     limits;   // Service handler resource consumption allowance
    private final BoundedThreadPool workers;  // Thread pool for limiting the concurrent processing
    private final ContextualLogger  logger;   // Logger with connection id injected

    // Constructs a broadcast scheme implementation.
    public BroadcastScheme(final RelayProtocol protocol, final ServiceHandler handler, final ServiceLimits limits, final ContextualLogger logger) {
        this.protocol = protocol;
        this.handler = handler;
        this.limits = limits;
        this.logger = logger;

        if (limits != null) {
            this.workers = new BoundedThreadPool(limits.broadcastThreads, limits.broadcastMemory);
        } else {
            this.workers = null;
        }
    }

    // Relays a broadcast operation to the local Iris node.
    public void broadcast(final String cluster, final byte[] message) throws IOException {
        protocol.sendBroadcast(cluster, message);
    }

    // Schedules an application broadcast message for the service handler to process.
    public void handleBroadcast(final byte[] message) {
        if (!workers.schedule(() -> handler.handleBroadcast(message), message.length)) {
            logger.loadContext();
            logger.error("Broadcast exceeded memory allowance",
                         "limit", String.valueOf(limits.broadcastMemory),
                         "size", String.valueOf(message.length));
            logger.unloadContext();
        }
    }

    // Terminates the broadcast primitive.
    public void close() throws InterruptedException {
        if (workers != null) {
            workers.terminate(true);
        }
    }
}
