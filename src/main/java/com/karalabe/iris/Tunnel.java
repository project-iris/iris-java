// Copyright (c) 2014 Project Iris. All rights reserved.
//
// The current language binding is an official support library of the Iris
// cloud messaging framework, and as such, the same licensing terms apply.
// For details please see http://iris.karalabe.com/downloads#License
package com.karalabe.iris;

import com.karalabe.iris.exceptions.TimeoutException;
import com.karalabe.iris.schemes.TunnelScheme.TunnelBridge;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

// Communication stream between the local application and a remote endpoint. The
// ordered delivery of messages is guaranteed and the message flow between the
// peers is throttled.
public class Tunnel implements AutoCloseable {
    private final TunnelBridge bridge;

    public Tunnel(@NotNull final TunnelBridge bridge) {
        this.bridge = bridge;
    }

    // Sends a message over the tunnel to the remote pair, blocking until the local
    // Iris node receives the message.
    public void send(@NotNull final byte[] message) throws IOException, InterruptedException {
        try {
            bridge.send(message, 0);
        } catch (TimeoutException e) {
            throw new Error("Timeout on non-timed send operation", e);
        }
    }

    // Sends a message over the tunnel to the remote pair, blocking until the local
    // Iris node receives the message or the operation times out.
    public void send(@NotNull final byte[] message, final long timeout) throws IOException, TimeoutException, InterruptedException {
        bridge.send(message, timeout);
    }

    // Retrieves a message from the tunnel, blocking until one is available.
    public byte[] receive() throws IOException, InterruptedException {
        try {
            return bridge.receive(0);
        } catch (TimeoutException e) {
            throw new Error("Timeout on non-timed receive operation", e);
        }
    }

    // Retrieves a message from the tunnel, blocking until one is available or the
    // operation times out.
    public byte[] receive(final long timeout) throws IOException, TimeoutException, InterruptedException {
        return bridge.receive(timeout);
    }

    // Closes the tunnel between the pair. Any blocked read and write operation will be interrupted.
    //
    // The method blocks until the local relay node acknowledges the tear-down.
    @Override public void close() throws IOException, InterruptedException {
        bridge.close();
    }
}
