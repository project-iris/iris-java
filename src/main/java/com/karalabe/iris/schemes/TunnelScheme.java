// Copyright (c) 2014 Project Iris. All rights reserved.
//
// The current language binding is an official support library of the Iris
// cloud messaging framework, and as such, the same licensing terms apply.
// For details please see http://iris.karalabe.com/downloads#License
package com.karalabe.iris.schemes;

import com.karalabe.iris.ServiceHandler;
import com.karalabe.iris.Tunnel;
import com.karalabe.iris.common.ContextualLogger;
import com.karalabe.iris.exceptions.ClosedException;
import com.karalabe.iris.exceptions.TimeoutException;
import com.karalabe.iris.protocol.RelayProtocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

// Implements the tunnel communication pattern.
public class TunnelScheme {
    private static class PendingBuild {
        Thread  owner;
        boolean timeout;
        long    chunking;

        public PendingBuild() {
            owner = Thread.currentThread();
        }
    }

    private static final int DEFAULT_TUNNEL_BUFFER = 64 * 1024 * 1024; // Size of a tunnel's input buffer.

    private final RelayProtocol                  protocol; // Network connection implementing the relay protocol
    private final Function<TunnelBridge, Tunnel> builder;  // Factory method to wrap a tunnel bridge into a tunnel
    private final ServiceHandler                 handler;  // Callback handler for processing inbound tunnels
    private final ContextualLogger               logger;   // Logger with connection id injected

    private final AtomicInteger           nextId  = new AtomicInteger();          // Unique identifier for the next tunnel
    private final Map<Long, PendingBuild> pending = new ConcurrentHashMap<>(128); // Result objects for pending tunnel
    private final Map<Long, TunnelBridge> active  = new ConcurrentHashMap<>(128); // Currently active tunnels
    private final AtomicBoolean           closed  = new AtomicBoolean(false);     // Flag specifying if the connection was closed

    private final ExecutorService throttler = Executors.newSingleThreadExecutor(); // Executor for sending back async tunnel allowances

    // Constructs a tunnel scheme implementation.
    public TunnelScheme(final RelayProtocol protocol, final ServiceHandler handler, final ContextualLogger logger,
                        final Function<TunnelBridge, Tunnel> builder) {
        this.protocol = protocol;
        this.builder = builder;
        this.handler = handler;
        this.logger = logger;
    }

    // Relays a tunnel construction request to the local Iris node, waits for a
    // reply or timeout and potentially returns a new tunnel.
    public Tunnel tunnel(final String cluster, final long timeout) throws IOException, ClosedException, TimeoutException {
        // Ensure the connection hasn't been closed yet
        if (closed.get()) {
            throw new ClosedException("Connection already closed!");
        }
        // Fetch a unique ID for the tunnel
        final long id = nextId.incrementAndGet();

        // Create a temporary object to store the construction result
        final PendingBuild operation = new PendingBuild();
        pending.put(id, operation);

        // Create the potential tunnel (needs pre-creation due to activation race)
        final TunnelBridge bridge = new TunnelBridge(id, 0, logger);
        active.put(id, bridge);

        try {
            // Send the construction request and wait for the reply
            synchronized (operation) {
                bridge.logger.loadContext();
                bridge.logger.info("Constructing outbound tunnel",
                                   "cluster", cluster, "timeout", String.valueOf(timeout));

                protocol.sendTunnelInit(id, cluster, timeout);
                operation.wait();
            }

            if (operation.timeout) {
                throw new TimeoutException("Tunnel construction timed out!");
            }
            bridge.chunkLimit = (int) operation.chunking;

            // Send the data allowance and return the active tunnel
            protocol.sendTunnelAllowance(id, DEFAULT_TUNNEL_BUFFER);
            bridge.logger.info("Tunnel construction completed", "chunk_limit", String.valueOf(bridge.chunkLimit));

            return builder.apply(bridge);
        } catch (IOException | InterruptedException | TimeoutException e) {
            bridge.logger.warn("Tunnel construction failed", "reason", e.getMessage());

            // Make sure the half initialized tunnel is discarded
            active.remove(id);

            try {
                throw e;
            } catch (InterruptedException ex) {
                throw new ClosedException(ex);
            }
        } finally {
            // Make sure the pending operations are cleaned up
            bridge.logger.unloadContext();
            pending.remove(id);
        }
    }

    // Opens a new local tunnel endpoint and binds it to the remote side.
    public void handleTunnelInit(final long initId, final long chunking) {
        new Thread(() -> {
            // Create the local tunnel endpoint
            final long id = nextId.addAndGet(1);

            TunnelBridge bridge = new TunnelBridge(id, (int) chunking, logger);
            active.put(id, bridge);

            bridge.logger.loadContext();
            bridge.logger.info("Accepting inbound tunnel", "chunk_limit", String.valueOf(chunking));

            // Confirm the tunnel creation to the relay node and send the allowance
            try {
                protocol.sendTunnelConfirm(initId, id);
                protocol.sendTunnelAllowance(id, DEFAULT_TUNNEL_BUFFER);

                bridge.logger.info("Tunnel acceptance completed");
                handler.handleTunnel(builder.apply(bridge));
            } catch (IOException e) {
                bridge.logger.warn("Tunnel acceptance failed", "reason", e.getMessage());
                active.remove(id);
            } finally {
                bridge.logger.unloadContext();
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
        // Fill in the operation result and wake teh origin thread
        synchronized (operation) {
            operation.timeout = (chunking == 0);
            operation.chunking = chunking;

            operation.notify();
        }
    }

    // Forwards a tunnel data allowance to the requested tunnel.
    public void handleTunnelAllowance(final long id, final int space) {
        final TunnelBridge bridge = active.get(id);
        if (bridge != null) {
            bridge.handleAllowance(space);
        }
    }

    // Forwards a message chunk transfer to the requested tunnel.
    public void handleTunnelTransfer(final long id, final int size, final byte[] chunk) {
        final TunnelBridge bridge = active.get(id);
        if (bridge != null) {
            bridge.handleTransfer(size, chunk);
        }
    }

    // Terminates a tunnel, stopping all data transfers.
    public void handleTunnelClose(final long id, final String reason) throws IOException {
        final TunnelBridge bridge = active.get(id);
        if (bridge != null) {
            bridge.handleClose(reason);
            active.remove(id);
        }
    }

    // Terminates the tunnel primitive.
    public void close() {
        // Make sure all new requests fail
        closed.set(true);

        // Interrupt all locally pending tunnel constructions
        for (final PendingBuild operation : pending.values()) {
            synchronized (operation) {
                operation.owner.interrupt();
            }
        }
        // Interrupt all active tunnels
        for (final TunnelBridge bridge : active.values()) {
            bridge.handleClose("Connection closed");
        }
        // Close the acknowledgement sender
        throttler.shutdownNow();
    }

    // Bridge between the scheme implementation and an API tunnel instance.
    public class TunnelBridge {
        private final long             id;     // Tunnel identifier for de/multiplexing
        private final ContextualLogger logger; // Logger with connection and tunnel id injected

        // Chunking fields
        private int                   chunkLimit;    // Maximum length of a data payload
        private ByteArrayOutputStream chunkBuffer;   // Current message being assembled
        private int                   chunkCapacity; // Size of the message being assembled

        // Quality of service fields
        private final Queue<byte[]> itoaBuffer = new LinkedBlockingQueue<>(); // Iris to application message buffer
        private       Thread        itoaThread = null;                        // Thread currently waiting to receive a message

        private       long   atoiSpace  = 0;            // Application to Iris space allowance
        private final Object atoiLock   = new Object(); // Protects the allowance and doubles as a signaller
        private       Thread atoiThread = null;         // Thread currently waiting to send a message

        // Bookkeeping fields
        private final Object        exitLock   = new Object();             // Tear-down synchronizer
        private       String        exitStatus = null;                     // Reason for termination, if not clean exit
        private final AtomicBoolean closed     = new AtomicBoolean(false); // Flag specifying if the tunnel was closed

        public TunnelBridge(final long id, final int chunking, final ContextualLogger logger) {
            this.id = id;
            this.logger = new ContextualLogger(logger, "tunnel", String.valueOf(id));

            chunkLimit = chunking;
        }

        // Requests the closure of the tunnel.
        public void close() throws IOException, ClosedException {
            synchronized (exitLock) {
                // Send the tear-down request if still alive and wait until a reply arrives
                if (exitStatus == null) {
                    try {
                        logger.loadContext();
                        logger.info("Closing tunnel");

                        protocol.sendTunnelClose(id);
                        exitLock.wait();
                    } catch (InterruptedException e) {
                        throw new ClosedException(e);
                    } finally {
                        logger.unloadContext();
                    }
                }
            }
        }

        // Sends a message over the tunnel to the remote pair, blocking until the local
        // Iris node receives the message or the operation times out.
        public void send(final byte[] message, final long timeout) throws IOException, TimeoutException, ClosedException {
            // Ensure the connection hasn't been closed yet
            if (closed.get()) {
                throw new ClosedException("Tunnel already closed!");
            }
            if (logger.isDebugEnabled()) {
                logger.loadContext();
                logger.debug("Sending message", "data", new String(logger.truncate(message)), "timeout", String.valueOf(timeout));
                logger.unloadContext();
            }
            // Calculate the deadline for the operation to finish
            final long deadline = System.nanoTime() + timeout * 10000000;

            // Split the original message into bounded chunks
            for (int pos = 0; pos < message.length; pos += chunkLimit) {
                final int end = Math.min(pos + chunkLimit, message.length);
                final int sizeOrCont = ((pos == 0) ? message.length : 0);
                final byte[] chunk = Arrays.copyOfRange(message, pos, end);

                // Wait for enough space allowance
                synchronized (atoiLock) {
                    while (atoiSpace < chunk.length) {
                        try {
                            atoiThread = Thread.currentThread();
                            if (timeout == 0) {
                                atoiLock.wait();
                            } else {
                                final long sleep = (deadline - System.nanoTime()) / 10000000;
                                if (sleep <= 0) {
                                    throw new TimeoutException("");
                                }
                                atoiLock.wait(sleep);
                            }
                        } catch (InterruptedException e) {
                            throw new ClosedException(e);
                        } finally {
                            atoiThread = null;
                        }
                    }
                    atoiSpace -= chunk.length;
                }
                protocol.sendTunnelTransfer(id, sizeOrCont, chunk);
            }
        }

        // Retrieves a message from the tunnel, blocking until one is available or the
        // operation times out.
        public byte[] receive(final long timeout) throws ClosedException, TimeoutException {
            // Ensure the connection hasn't been closed yet
            if (closed.get()) {
                throw new ClosedException("Tunnel already closed!");
            }
            synchronized (itoaBuffer) {
                // Wait for a message to arrive if none is available
                if (itoaBuffer.isEmpty()) {
                    try {
                        itoaThread = Thread.currentThread();
                        if (timeout > 0) {
                            itoaBuffer.wait(timeout);
                        } else {
                            itoaBuffer.wait();
                        }
                    } catch (InterruptedException e) {
                        throw new ClosedException(e);
                    } finally {
                        itoaThread = null;
                    }
                    if (itoaBuffer.isEmpty()) {
                        throw new TimeoutException("");
                    }
                }
                // Fetch the pending message and send a remote allowance
                final byte[] message = itoaBuffer.remove();
                if (logger.isDebugEnabled()) {
                    logger.loadContext();
                    logger.debug("Fetching queued message", "data", new String(logger.truncate(message)));
                    logger.unloadContext();
                }
                throttler.submit(() -> {
                    try {
                        protocol.sendTunnelAllowance(id, message.length);
                    } catch (IOException ignored) {}
                });
                return message;
            }
        }

        // Increases the available data allowance of the remote endpoint.
        public void handleAllowance(final int space) {
            synchronized (atoiLock) {
                atoiSpace += space;
                atoiLock.notify();
            }
        }

        // Adds the chunk to the currently building message and delivers it upon
        // completion. If a new message starts, the old is discarded.
        public void handleTransfer(final int size, final byte[] chunk) {
            // If a new message is arriving, dump anything stored before
            if (size != 0) {
                if (chunkBuffer != null) {
                    logger.loadContext();
                    logger.warn("Incomplete message discarded",
                                "size", String.valueOf(chunkCapacity),
                                "arrived", String.valueOf(chunkBuffer.size()));
                    logger.unloadContext();

                    // A large transfer timed out, new started, grant the partials allowance
                    final int allowance = chunkBuffer.size();
                    throttler.submit(() -> {
                        try {
                            protocol.sendTunnelAllowance(id, allowance);
                        } catch (IOException ignored) {}
                    });
                }
                chunkCapacity = size;
                chunkBuffer = new ByteArrayOutputStream(chunkCapacity);
            }
            // Append the new chunk and check completion
            try {
                chunkBuffer.write(chunk);
            } catch (IOException ignored) {}

            if (chunkBuffer.size() == chunkCapacity) {
                // Transfer the completed message into the inbound queue
                final byte[] message = chunkBuffer.toByteArray();
                if (logger.isDebugEnabled()) {
                    logger.loadContext();
                    logger.debug("Queuing arrived message", "data", new String(logger.truncate(message)));
                    logger.unloadContext();
                }
                synchronized (itoaBuffer) {
                    itoaBuffer.add(message);
                    chunkBuffer = null;
                    chunkCapacity = 0;

                    // Wake up any thread waiting for inbound data
                    itoaBuffer.notify();
                }
            }
        }

        // Handles the graceful remote closure of the tunnel.
        public void handleClose(final String reason) {
            // Make sure all new operations fail
            closed.set(true);

            // Save the exit reason and log if failure
            synchronized (exitLock) {
                try {
                    logger.loadContext();
                    if (reason.length() != 0) {
                        logger.warn("Tunnel dropped", "reason", reason);
                    } else {
                        logger.info("Tunnel closed gracefully");
                    }
                    exitStatus = reason;
                    exitLock.notifyAll();
                } finally {
                    logger.unloadContext();
                }
            }
            // Interrupt any pending send and receive
            synchronized (atoiLock) {
                if (atoiThread != null) {
                    atoiThread.interrupt();
                }
            }
            synchronized (itoaBuffer) {
                if (itoaThread != null) {
                    itoaThread.interrupt();
                }
            }
        }
    }
}