package com.karalabe.iris;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

// TODO should probably be segregated
public interface ConnectionHandler {
    /* Handles a message broadcasted to all applications of the local type.*/
    default void handleBroadcast(@NotNull byte[] message) {
        System.err.println("No broadcast handler provided!"); // FIXME change to logging
    }

    /* Handles a request (message), returning the reply that should be forwarded back to the caller.
     * If the method crashes, nothing is returned and the caller will eventually time out.*/
    default @NotNull byte[] handleRequest(@NotNull byte[] request) {
        throw new IllegalStateException("No request handler provided!");
    }

    /* Handles the request to open a direct tunnel.*/
    default void handleTunnel(@NotNull Tunnel tunnel) {
        try {
            System.err.println("No tunnel handler provided!"); // FIXME change to logging
            tunnel.close();
        }
        catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /* Handles the unexpected termination of the relay connection.*/
    default void handleDrop(@NotNull IOException reason) {
        throw new IllegalStateException("No drop handler provided!");
    }
}