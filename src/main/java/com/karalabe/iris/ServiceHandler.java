package com.karalabe.iris;

import org.jetbrains.annotations.NotNull;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public interface ServiceHandler {
    default void init(@NotNull final Connection conn) { }

    default void handleBroadcast(@NotNull final byte[] message) {}

    default byte[] handleRequest(@NotNull final byte[] request) throws RuntimeException {
        throw new NotImplementedException();
    }

    default void handleTunnel(@NotNull final Tunnel tunnel) {
        tunnel.Close();
    }

    default void handleDrop(@NotNull final RuntimeException e) {
        throw e;
    }
}
