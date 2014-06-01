package com.karalabe.iris;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public interface ServiceHandler {
    default void init(Connection conn) { }

    default void handleBroadcast(byte[] message) {}

    default byte[] handleRequest(byte[] request) throws RuntimeException {
        throw new NotImplementedException();
    }

    default void handleTunnel(Tunnel tunnel) {
        tunnel.Close();
    }

    default void handleDrop(RuntimeException e) {
        throw e;
    }
}