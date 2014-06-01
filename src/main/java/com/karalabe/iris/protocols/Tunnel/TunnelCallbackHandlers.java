package com.karalabe.iris.protocols.tunnel;

import com.karalabe.iris.callback.CallbackHandler;
import com.karalabe.iris.callback.InstanceCallbackHandler;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

// TODO should probably be segregated
public interface TunnelCallbackHandlers extends InstanceCallbackHandler, CallbackHandler {
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
}