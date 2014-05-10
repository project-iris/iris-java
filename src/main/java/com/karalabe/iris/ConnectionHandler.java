package com.karalabe.iris;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

// TODO should probably be segregated
public interface ConnectionHandler {
    /* Handles a message broadcasted to all applications of the local type.*/
    default void handleBroadcast(@NotNull byte[] message) throws IOException {
        System.err.println("No broadcast handler provided!"); // FIXME change to logging
    }

    /* Handles a request (message), returning the reply that should be forwarded back to the caller.
     * If the method crashes, nothing is returned and the caller will eventually time out.*/
    default @NotNull byte[] handleRequest(long requestId, @NotNull byte[] request) {
        throw new IllegalStateException("No request handler provided!");
    }

    // TODO the following should go to separate handler classes
    default @NotNull byte[] handleReply(long requestId, @NotNull byte[] reply) throws IOException {
        throw new IllegalStateException("No reply handler provided!");
    }

    default @NotNull byte[] handlePublish(@NotNull String topic, @NotNull byte[] message) throws IOException {
        throw new IllegalStateException("No publish handler provided!");
    }

    default @NotNull byte[] handleDrop() throws IOException {
        throw new IllegalStateException("No drop handler provided!");
    }

    default @NotNull byte[] handleTunnelRequest(long tmpId, long bufferSize) throws IOException {
        throw new IllegalStateException("No tunnel request handler provided!");
    }

    default @NotNull byte[] handleTunnelReply(long tunnelId, long bufferSize, boolean hasTimedOut) throws IOException {
        throw new IllegalStateException("No tunnel reply handler provided!");
    }

    default @NotNull byte[] handleTunnelAck(long tunnelId) throws IOException {
        throw new IllegalStateException("No tunnel ack handler provided!");
    }

    default @NotNull byte[] handleTunnelData(long tunnelId, @NotNull byte[] message) throws IOException {
        throw new IllegalStateException("No tunnel data handler provided!");
    }

    default @NotNull byte[] handleTunnelClose(long tunnelId) throws IOException {
        throw new IllegalStateException("No tunnel close handler provided!");
    }

    /* Handles the request to open a direct tunnel.*/
    default void handleTunnel(@NotNull Tunnel tunnel) throws IOException {
        try {
            System.err.println("No tunnel handler provided!"); // FIXME change to logging
            tunnel.close();
        }
        catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /* Handles the unexpected termination of the relay connection.*/
    default void handleDrop(@NotNull IOException reason) throws IOException {
        throw new IllegalStateException("No drop handler provided!");
    }
}