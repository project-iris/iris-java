// Copyright (c) 2014 Project Iris. All rights reserved.
//
// The current language binding is an official support library of the Iris
// cloud messaging framework, and as such, the same licensing terms apply.
// For details please see http://iris.karalabe.com/downloads#License
package com.karalabe.iris.protocol;

import com.karalabe.iris.RemoteException;
import com.karalabe.iris.ServiceHandler;
import com.karalabe.iris.ServiceLimits;
import com.karalabe.iris.common.BoundedThreadPool;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

public class RequestExecutor extends ExecutorBase {
    private class Result {
        public boolean timeout;
        public byte[]  reply;
        public String  error;
    }

    private final ServiceHandler    handler; // Callback handler for processing inbound requests
    private final BoundedThreadPool workers; // Thread pool for limiting the concurrent processing

    private final AtomicLong        nextId  = new AtomicLong(0); // Unique identifier for the next request
    private final Map<Long, Result> pending = new ConcurrentHashMap<>(0); // Result objects for pending requests

    public RequestExecutor(final ProtocolBase protocol, final ServiceHandler handler, final ServiceLimits limits) {
        super(protocol);

        this.handler = handler;
        this.workers = new BoundedThreadPool(limits.requestThreads, limits.requestMemory);
    }

    public byte[] request(final String cluster, byte[] request, long timeoutMillis) throws IOException, InterruptedException, RemoteException, TimeoutException {
        // Fetch a unique ID for the request
        final Long id = nextId.addAndGet(1);

        // Create a temporary object to store the reply
        final Result result = new Result();
        pending.put(id, result);

        try {
            // Send the request
            protocol.send(OpCode.REQUEST, () -> {
                protocol.sendVarint(id);
                protocol.sendString(cluster);
                protocol.sendBinary(request);
                protocol.sendVarint(timeoutMillis);
            });
            // Wait until a reply arrives
            synchronized (result) {
                result.wait();
            }
            if (result.timeout) {
                throw new TimeoutException("Request timed out!");
            } else if (result.error != null) {
                throw new RemoteException(result.error);
            } else {
                return result.reply;
            }
        }
        finally {
            pending.remove(id);
        }
    }

    public void reply(final long id, final byte[] response, final String error) throws IOException {
        protocol.send(OpCode.REPLY, () -> {
            protocol.sendVarint(id);
            protocol.sendBoolean(error == null);
            if (error == null) {
                protocol.sendBinary(response);
            } else {
                protocol.sendString(error);
            }
        });
    }

    public void handleRequest() throws IOException {
        final long id = protocol.receiveVarint();
        final byte[] request = protocol.receiveBinary();
        final long timeout = protocol.receiveVarint();

        workers.schedule(() -> {
            byte[] response = null;
            String error = null;

            // Execute the request and flatten any error
            try {
                response = handler.handleRequest(request);
            }
            catch (Exception e) {
                error = e.toString();
            }
            // Try and send back the reply
            try {
                reply(id, response, error);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }, request.length, (int) timeout);
    }

    public void handleReply() throws IOException {
        // Read the request id and fetch the pending result
        final long id = protocol.receiveVarint();
        final Result result = pending.get(id);
        if (result == null) {
            // Already dead? Thread got interrupted!
            return;
        }
        // Read the rest of the response and fill the result accordingly
        result.timeout = protocol.receiveBoolean();
        if (!result.timeout) {
            final boolean success = protocol.receiveBoolean();
            if (success) {
                result.reply = protocol.receiveBinary();
            } else {
                result.error = protocol.receiveString();
            }
        }
        // Wake the origin thread
        synchronized (result) {
            result.notify();
        }
    }

    @Override public void close() throws Exception {
        workers.terminate(true);
    }
}
