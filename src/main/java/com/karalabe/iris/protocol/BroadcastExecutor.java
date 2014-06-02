// Copyright (c) 2014 Project Iris. All rights reserved.
//
// The current language binding is an official support library of the Iris
// cloud messaging framework, and as such, the same licensing terms apply.
// For details please see http://iris.karalabe.com/downloads#License
package com.karalabe.iris.protocol;

import com.karalabe.iris.ServiceHandler;
import com.karalabe.iris.ServiceLimits;
import com.karalabe.iris.common.BoundedThreadPool;

import java.io.IOException;

public class BroadcastExecutor extends ExecutorBase {
    private final ServiceHandler    handler; // Callback handler for processing inbound broadcasts
    private final BoundedThreadPool workers; // Thread pool for limiting the concurrent processing

    public BroadcastExecutor(final ProtocolBase protocol, final ServiceHandler handler, final ServiceLimits limits) {
        super(protocol);

        this.handler = handler;
        this.workers = new BoundedThreadPool(limits.broadcastThreads, limits.broadcastMemory);
    }

    public void broadcast(final String cluster, final byte[] message) throws IOException {
        protocol.send(OpCode.BROADCAST, () -> {
            protocol.sendString(cluster);
            protocol.sendBinary(message);
        });
    }

    public void handleBroadcast() throws IOException {
        final byte[] message = protocol.receiveBinary();
        workers.schedule(() -> {
            handler.handleBroadcast(message);
        }, message.length);
    }

    @Override public void close() throws Exception {
        workers.terminate(true);
    }
}
