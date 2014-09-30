// Copyright (c) 2014 Project Iris. All rights reserved.
//
// The current language binding is an official support library of the Iris
// cloud messaging framework, and as such, the same licensing terms apply.
// For details please see http://iris.karalabe.com/downloads#License
package com.karalabe.iris.schemes;

import com.karalabe.iris.ServiceHandler;
import com.karalabe.iris.ServiceLimits;
import com.karalabe.iris.common.BoundedThreadPool;
import com.karalabe.iris.common.ContextualLogger;
import com.karalabe.iris.exceptions.ClosedException;
import com.karalabe.iris.exceptions.RemoteException;
import com.karalabe.iris.exceptions.TimeoutException;
import com.karalabe.iris.protocol.RelayProtocol;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

// Implements the request/reply communication pattern.
public class RequestScheme {
    private static class PendingRequest {
        Thread  owner;
        boolean timeout;
        byte[]  reply;
        String  error;

        public PendingRequest() {
            owner = Thread.currentThread();
        }
    }

    private final RelayProtocol     protocol; // Network connection implementing the relay protocol
    private final ServiceHandler    handler;  // Callback handler for processing inbound requests
    private final ServiceLimits     limits;   // Service handler resource consumption allowance
    private final BoundedThreadPool workers;  // Thread pool for limiting the concurrent processing
    private final ContextualLogger  logger;   // Logger with connection id injected

    private final AtomicLong                nextId  = new AtomicLong();             // Unique identifier for the next request
    private final Map<Long, PendingRequest> pending = new ConcurrentHashMap<>(128); // Result objects for pending requests
    private final AtomicBoolean             closed  = new AtomicBoolean(false);     // Flag specifying if the connection was closed

    // Constructs a request/reply scheme implementation.
    public RequestScheme(final RelayProtocol protocol, final ServiceHandler handler, final ServiceLimits limits, final ContextualLogger logger) {
        this.protocol = protocol;
        this.handler = handler;
        this.limits = limits;
        this.logger = logger;

        if (limits != null) {
            this.workers = new BoundedThreadPool(limits.requestThreads, limits.requestMemory);
        } else {
            this.workers = null;
        }
    }

    // Relays a request to the local Iris node, waits for a reply or timeout and returns it.
    public byte[] request(final String cluster, final byte[] request, final long timeout) throws IOException, ClosedException, RemoteException, TimeoutException {
        // Ensure the connection hasn't been closed yet
        if (closed.get()) {
            throw new ClosedException("Connection already closed!");
        }
        // Fetch a unique ID for the request
        final long id = nextId.incrementAndGet();
        if (logger.isDebugEnabled()) {
            logger.loadContext();
            logger.debug("Sending new request", "cluster", cluster, "local_request", String.valueOf(id), "data", new String(logger.truncate(request)), "timeout", String.valueOf(timeout));
            logger.unloadContext();
        }
        // Create a temporary object to store the reply
        final PendingRequest operation = new PendingRequest();
        this.pending.put(id, operation);

        try {
            // Send the request and wait for the reply
            synchronized (operation) {
                protocol.sendRequest(id, cluster, request, timeout);
                try {
                    operation.wait();

                    // Thread notified, but clear any possible racy interrupts
                    Thread.interrupted();
                } catch (InterruptedException e) {
                    throw new ClosedException(e);
                }
            }

            if (operation.timeout) {
                throw new TimeoutException("Request timed out!");
            } else if (operation.error != null) {
                throw new RemoteException(operation.error);
            } else {
                return operation.reply;
            }
        } finally {
            // Make sure the pending operations are cleaned up
            this.pending.remove(id);
        }
    }

    // Schedules an application request for the service handler to process.
    public void handleRequest(final long id, final byte[] request, final long timeout) {
        final ContextualLogger logger = new ContextualLogger(this.logger, "remote_request", String.valueOf(id));
        if (logger.isDebugEnabled()) {
            logger.loadContext();
            logger.debug("Scheduling arrived request", "data", new String(logger.truncate(request)));
            logger.unloadContext();
        }

        final long start = System.nanoTime();
        if (!workers.schedule(() -> {
            logger.loadContext();

            // Ensure that expired tasks get dropped instead of executed
            final long elapsed = (System.nanoTime() - start) / 1000000;
            if (elapsed >= timeout) {
                logger.error("Dumping expired scheduled request",
                             "scheduled", String.valueOf(elapsed),
                             "timeout", String.valueOf(timeout),
                             "expired", String.valueOf(elapsed - timeout));
                return;
            }

            byte[] response = null;
            String error = null;

            // Execute the request and flatten any error
            try {
                logger.debug("Handling scheduled request");
                response = handler.handleRequest(request);
            } catch (RemoteException e) {
                error = e.getMessage();
            }
            // Try and send back the reply
            try {
                if (logger.isDebugEnabled()) {
                    if (response != null) {
                        logger.debug("Replying to scheduled request", "data", new String(logger.truncate(response)));
                    } else {
                        logger.debug("Replying to scheduled request", "error", error);
                    }
                }
                reply(id, response, error);
            } catch (IOException e) {
                logger.error("Failed to send reply", "reason", e.getMessage());
            }
        }, request.length)) {
            logger.loadContext();
            logger.error("Request exceeded memory allowance",
                         "limit", String.valueOf(limits.requestMemory),
                         "size", String.valueOf(request.length));
            logger.unloadContext();
        }
    }

    // Relays a reply to a request to the local Iris node.
    public void reply(final long id, final byte[] response, final String error) throws IOException {
        protocol.sendReply(id, response, error);
    }

    // Looks up a pending request and delivers the result.
    public void handleReply(final long id, final byte[] reply, final String error) {
        if (logger.isDebugEnabled()) {
            logger.loadContext();
            if (reply != null) {
                logger.debug("Request completed", "local_request", String.valueOf(id), "data", new String(logger.truncate(reply)));
            } else {
                logger.debug("Request completed", "local_request", String.valueOf(id), "error", error);
            }
            logger.unloadContext();
        }

        // Fetch the pending result
        final PendingRequest operation = this.pending.get(id);
        if (operation == null) {
            // Already dead? Thread got interrupted!
            return;
        }
        // Fill in the operation result and wake the origin thread
        synchronized (operation) {
            operation.timeout = ((reply == null) && (error == null));
            operation.reply = reply;
            operation.error = error;

            operation.notify();
        }
    }

    // Terminates the request/reply primitive.
    public void close() throws InterruptedException {
        // Make sure all new requests fail
        closed.set(true);

        // Interrupt all locally pending requests
        for (final PendingRequest operation : pending.values()) {
            synchronized (operation) {
                operation.owner.interrupt();
            }
        }
        // Interrupt all remote request processors
        if (workers != null) {
            workers.terminate(true);
        }
    }
}
