/*
 * Copyright © 2014 Project Iris. All rights reserved.
 *
 * The current language binding is an official support library of the Iris cloud messaging framework, and as such, the same licensing terms apply.
 * For details please see http://iris.karalabe.com/downloads#License
 */

package com.karalabe.iris;

import com.karalabe.iris.protocol.TunnelExecutor;
import org.jetbrains.annotations.NotNull;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/** Bridge between the executor and az API tunnel instance. */
public class TunnelBridge implements AutoCloseable {
    private static final int TUNNEL_BUFFER = 64 * 1024 * 1024; // Size of a tunnel's input buffer.

    private final long           id;       // Tunnel identifier for de/multiplexing
    private final TunnelExecutor tunneler; // Protocol executor to the local relay

    /** Chunking fields */
    private final long   chunkLimit;  // Maximum length of a data payload
    private       byte[] chunkBuffer; // Current message being assembled

    /** Quality of service fields */
    private final Queue<byte[]> itoaBuffer = new ConcurrentLinkedQueue<>(); // Iris to application message buffer

    public TunnelBridge(@NotNull final TunnelExecutor tunneler, final long id, final long chunking) {
        this.tunneler = tunneler;
        this.id = id;
        chunkLimit = chunking;
    }

    @Override public void close() { tunneler.closeBridge(id); }
}
