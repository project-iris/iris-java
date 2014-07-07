/*
 * Copyright © 2014 Project Iris. All rights reserved.
 *
 * The current language binding is an official support library of the Iris cloud messaging framework, and as such, the same licensing terms apply.
 * For details please see http://iris.karalabe.com/downloads#License
 */

package com.karalabe.iris;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeoutException;

public class Tunnel implements AutoCloseable {
    private final TunnelBridge bridge;

    Tunnel(@NotNull final TunnelBridge bridge) {
        this.bridge = bridge;
    }

    /**
     * Sends a message over the tunnel to the remote pair, blocking until the local Iris node receives the message or the operation times out.
     * Infinite blocking is supported with by setting the timeout to zero (0).
     */
    public void send(final long timeout, @NotNull final byte... message) throws TimeoutException, InterruptedException {}

    /**
     * Retrieves a message from the tunnel, blocking until one is available or the operation times out.
     * Infinite blocking is supported with by setting the timeout to zero (0).
     */
    public byte[] receive(final long timeout) throws TimeoutException, InterruptedException {
        return null;
    }

    /**
     * Closes the tunnel between the pair. Any blocked read and write operation will be interrupted.
     * The method blocks until the local relay node acknowledges the tear-down.
     */
    @Override public void close() { bridge.close(); }
}
