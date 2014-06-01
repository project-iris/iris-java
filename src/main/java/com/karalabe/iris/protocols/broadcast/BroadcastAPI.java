package com.karalabe.iris.protocols.broadcast;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public interface BroadcastAPI {
    void broadcast(@NotNull final String clusterName, @NotNull final byte[] message) throws IOException;
}
