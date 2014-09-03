package com.karalabe.iris;

import com.karalabe.iris.schemes.TunnelScheme.TunnelBridge;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class Tunnel implements AutoCloseable {
    private final TunnelBridge bridge;

    public Tunnel(@NotNull final TunnelBridge bridge) {
        this.bridge = bridge;
    }

    // Sends a message over the tunnel to the remote pair, blocking until the local
    // Iris node receives the message or the operation times out.
    //
    // Infinite blocking is supported with by setting the timeout to zero (0).
    public void send(@NotNull final byte[] message, final long timeout) throws IOException, TimeoutException, InterruptedException {
        bridge.send(message, timeout);
    }

    // Retrieves a message from the tunnel, blocking until one is available or the
    // operation times out.
    //
    // Infinite blocking is supported with by setting the timeout to zero (0).
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
