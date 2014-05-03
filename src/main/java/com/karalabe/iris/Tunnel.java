package com.karalabe.iris;

import org.jetbrains.annotations.NotNull;

public abstract class Tunnel implements AutoCloseable {
    /* Sends a message over the tunnel to the remote pair.
       The method blocks until the local relay node receives the message, or an error occurs, in which case an iris.Error is returned.
       The timeout unit is in milliseconds. Infinite timeouts are supported with the value 0. Setting anything in between will result in a panic! */
    public void send(@NotNull final byte[] message, final long timeoutMillis) throws Exception {
        throw new IllegalStateException("Not implemented!");
    }

    /* Retrieves a message from the tunnel, blocking until one is available. As with the Send method, Recv too returns an iris.Error in case of a failure.
       The timeout unit is in milliseconds. Infinite timeouts are supported with the value 0. Setting anything in between will result in a panic! */
    @NotNull public byte[] recv(final long timeoutMillis) throws Exception {
        throw new IllegalStateException("Not implemented!");
    }

    /* Closes the tunnel between the pair. Any blocked read and write operation will terminate with a failure.
       The method blocks until the connection is torn down or an error occurs, in which case an iris.Error is returned. */
    @Override public void close() throws Exception {
        throw new IllegalStateException("Not implemented!");
    }
}