// Copyright (c) 2014 Project Iris. All rights reserved.
//
// The current language binding is an official support library of the Iris
// cloud messaging framework, and as such, the same licensing terms apply.
// For details please see http://iris.karalabe.com/downloads#License
package com.karalabe.iris.schemes;

import com.karalabe.iris.ServiceHandler;
import com.karalabe.iris.ServiceLimits;
import com.karalabe.iris.common.BoundedThreadPool;
import com.karalabe.iris.protocol.RelayProtocol;

import java.io.IOException;

// Implements the broadcast communication pattern.
public class BroadcastScheme {
    private final RelayProtocol     protocol; // Network connection implementing the relay protocol
    private final ServiceHandler    handler;  // Callback handler for processing inbound broadcasts
    private final BoundedThreadPool workers;  // Thread pool for limiting the concurrent processing

    // Constructs a broadcast scheme implementation.
    public BroadcastScheme(final RelayProtocol protocol, final ServiceHandler handler, final ServiceLimits limits) {
        this.protocol = protocol;
        this.handler = handler;
        this.workers = new BoundedThreadPool(limits.broadcastThreads, limits.broadcastMemory);
    }

    // Relays a broadcast operation to the local Iris node.
    public void broadcast(final String cluster, final byte[] message) throws IOException {
        protocol.sendBroadcast(cluster, message);
    }

    // Schedules an application broadcast message for the service handler to process.
    public void handleBroadcast(final byte[] message) {
        workers.schedule(() -> handler.handleBroadcast(message), message.length);
    }

    // Terminates the broadcast primitive.
    public void close() throws InterruptedException {
        workers.terminate(true);
    }
}
