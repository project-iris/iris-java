// Copyright (c) 2014 Project Iris. All rights reserved.
//
// The current language binding is an official support library of the Iris
// cloud messaging framework, and as such, the same licensing terms apply.
// For details please see http://iris.karalabe.com/downloads#License

package com.karalabe.iris;

import com.karalabe.iris.schemes.TunnelExecutor;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

// Bridge between the executor and az API tunnel instance.
public class TunnelBridge implements AutoCloseable {
    // Size of a tunnel's input buffer.
    private static final int TUNNEL_BUFFER = 64 * 1024 * 1024;

    private final long           id;       // Tunnel identifier for de/multiplexing
    private final TunnelExecutor tunneler; // Protocol executor to the local relay

    // Chunking fields
    private final long   chunkLimit;  // Maximum length of a data payload
    private       byte[] chunkBuffer;// Current message being assembled

    // Quality of service fields
    private final Queue<byte[]> itoaBuffer; // Iris to application message buffer

    public TunnelBridge(@NotNull final TunnelExecutor tunneler, final long id, final long chunking) {
        this.id = id;
        this.tunneler = tunneler;

        chunkLimit = chunking;

        itoaBuffer = new ConcurrentLinkedQueue<>();
    }

    @Override public void close() throws IOException, InterruptedException {
        //tunneler.closeBridge(id);
    }
}
