// Copyright (c) 2014 Project Iris. All rights reserved.
//
// The current language binding is an official support library of the Iris
// cloud messaging framework, and as such, the same licensing terms apply.
// For details please see http://iris.karalabe.com/downloads#License
package com.karalabe.iris.schemes;

import com.karalabe.iris.ServiceHandler;
import com.karalabe.iris.Tunnel;
import com.karalabe.iris.protocol.RelayProtocol;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

// Implements the tunnel communication pattern.
public class TunnelScheme {
    private static class PendingBuild {
        boolean timeout;
        long    chunking;
    }

    private static final int DEFAULT_TUNNEL_BUFFER = 64 * 1024 * 1024; // Size of a tunnel's input buffer.

    private final RelayProtocol  protocol; // Network connection implementing the relay protocol
    private final ServiceHandler handler;  // Callback handler for processing inbound tunnels

    private final AtomicInteger           nextId  = new AtomicInteger();          // Unique identifier for the next tunnel
    private final Map<Long, PendingBuild> pending = new ConcurrentHashMap<>(128); // Result objects for pending tunnel
    private final Map<Long, TunnelBridge> active  = new ConcurrentHashMap<>(128); // Currently active tunnels

    // Constructs a tunnel scheme implementation.
    public TunnelScheme(final RelayProtocol protocol, final ServiceHandler handler) {
        this.protocol = protocol;
        this.handler = handler;
    }

    // Relays a tunnel construction request to the local Iris node, waits for a
    // reply or timeout and potentially returns a new tunnel.
    public TunnelBridge tunnel(final String cluster, final long timeout) throws IOException, InterruptedException, TimeoutException {
        // Fetch a unique ID for the tunnel
        final long id = nextId.addAndGet(1);

        // Create a temporary object to store the construction result
        final PendingBuild operation = new PendingBuild();
        pending.put(id, operation);

        try {
            // Send the construction request and wait for the reply
            synchronized (operation) {
                protocol.sendTunnelInit(id, cluster, timeout);
                operation.wait();
            }

            if (operation.timeout) {
                throw new TimeoutException("Tunnel construction timed out!");
            }
            TunnelBridge bridge = new TunnelBridge(this, protocol, id, operation.chunking);
            active.put(id, bridge);
            return bridge;
        } finally {
            // Make sure the pending operations are cleaned up
            pending.remove(id);
        }
    }

    // Opens a new local tunnel endpoint and binds it to the remote side.
    public void handleTunnelInit(final long initId, final long chunking) {
        final TunnelScheme self = this;

        new Thread(() -> {
            // Create the local tunnel endpoint
            final long id = nextId.addAndGet(1);

            TunnelBridge bridge = new TunnelBridge(self, protocol, id, chunking);
            active.put(id, bridge);

            // Confirm the tunnel creation to the relay node and send the allowance
            try {
                protocol.sendTunnelConfirm(initId, id);
                protocol.sendTunnelAllowance(id, DEFAULT_TUNNEL_BUFFER);
                handler.handleTunnel(new Tunnel(bridge));
            } catch (Exception e) {
                active.remove(id);
                e.printStackTrace();
            }
        }).start();
    }

    // Forwards the tunnel construction result to the requested tunnel.
    public void handleTunnelResult(final long id, final long chunking) {
        // Fetch the pending construction result
        final PendingBuild operation = pending.get(id);
        if (operation == null) {
            // Already dead? Thread got interrupted!
            return;
        }
        // Fill in the operation result
        operation.timeout = (chunking == 0);
        operation.chunking = chunking;

        // Wake the origin thread
        synchronized (operation) {
            operation.notify();
        }
    }

    // Forwards a tunnel data allowance to the requested tunnel.
    public void handleTunnelAllowance(final long id, final int space) {
        TunnelBridge bridge = active.get(id);
        if (bridge != null) {
            bridge.handleAllowance(space);
        }
    }

    // Forwards a message chunk transfer to the requested tunnel.
    public void handleTunnelTransfer(final long id, final int size, final byte[] chunk) {
        TunnelBridge bridge = active.get(id);
        if (bridge != null) {
            bridge.handleTransfer(size, chunk);
        }
    }

    // Terminates a tunnel, stopping all data transfers.
    public void handleTunnelClose(final long id, final String reason) throws IOException {
        TunnelBridge bridge = active.get(id);
        if (bridge != null) {
            bridge.handleClose(reason);
            active.remove(id);
        }
    }

    // Terminates the tunnel primitive.
    public void close() {
        // TODO: Nothing for now?
    }

    // Bridge between the scheme implementation and an API tunnel instance.
    public class TunnelBridge {
        private final long          id;       // Tunnel identifier for de/multiplexing
        private final TunnelScheme  scheme;   // Protocol executor to the local relay
        private final RelayProtocol protocol; // Network connection implementing the relay protocol

        // Chunking fields
        private final long   chunkLimit;  // Maximum length of a data payload
        private       byte[] chunkBuffer; // Current message being assembled

        // Quality of service fields
        private final Queue<byte[]> itoaBuffer; // Iris to application message buffer

        // Bookkeeping fields
        private Object exitLock   = new Object(); // Flag specifying whether the tunnel has been torn down
        private String exitStatus = null;         // Reason for termination, if not clean exit

        public TunnelBridge(final TunnelScheme scheme, final RelayProtocol protocol, final long id, final long chunking) {
            this.id = id;
            this.scheme = scheme;
            this.protocol = protocol;

            chunkLimit = chunking;

            itoaBuffer = new ConcurrentLinkedQueue<>();
        }

        public void handleAllowance(final int space) {

        }

        public void handleTransfer(final int size, final byte[] chunk) {

        }

        // Handles the graceful remote closure of the tunnel.
        public void handleClose(final String reason) {
            synchronized (exitLock) {
                exitStatus = reason;
                exitLock.notifyAll();
            }
        }

        // Requests the closure of the tunnel.
        public void close() throws IOException, InterruptedException {
            synchronized (exitLock) {
                // Send the tear-down request if still alive and wait until a reply arrives
                if (exitStatus == null) {
                    protocol.sendTunnelClose(id);
                    exitLock.wait();
                }
                // If a failure occurred, throw an exception
                if (exitStatus.length() != 0) {
                    throw new RemoteException("Remote close failed: " + exitStatus);
                }
            }
        }
    }
}