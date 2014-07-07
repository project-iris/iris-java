/*
 * Copyright © 2014 Project Iris. All rights reserved.
 *
 * The current language binding is an official support library of the Iris cloud messaging framework, and as such, the same licensing terms apply.
 * For details please see http://iris.karalabe.com/downloads#License
 */

package com.karalabe.iris.protocol;

import com.karalabe.iris.ProtocolException;
import com.karalabe.iris.ServiceHandler;
import com.karalabe.iris.TunnelBridge;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.LongAdder;

public class TunnelExecutor extends ExecutorBase {
    // Tunnel async construction result.
    static class InitResult {
        boolean timeout;
        long    chunking;
    }

    // Tunnel async tear-down result.
    static class CloseResult {
        String reason;
    }

    private final ServiceHandler handler; // Callback handler for processing inbound tunnels

    private final LongAdder               nextId        = new LongAdder(); // Unique identifier for the next tunnel
    private final Map<Long, InitResult>   pendingInits  = new ConcurrentHashMap<>(); // Result objects for pending tunnel
    private final Map<Long, CloseResult>  pendingCloses = new ConcurrentHashMap<>(); // Result objects for pending tunnel
    private final Map<Long, TunnelBridge> active        = new ConcurrentHashMap<>(); // Currently active tunnels

    public TunnelExecutor(@NotNull final ProtocolBase protocol, @Nullable final ServiceHandler handler) {
        super(protocol);
        this.handler = handler;
    }

    @Override public void close() throws InterruptedException {}

    public TunnelBridge tunnel(final String cluster, final long timeout) throws InterruptedException, TimeoutException {
        // Fetch a unique ID for the request
        nextId.increment();
        final long id = nextId.longValue();

        // Create a temporary object to store the construction result
        final InitResult result = new InitResult();
        pendingInits.put(id, result);

        try {
            synchronized (result) {
                // Send the construction request
                protocol.send(OpCode.TUNNEL_BUILD, () -> {
                    protocol.sendVarint(id);
                    protocol.sendString(cluster);
                    protocol.sendVarint(timeout);
                });
                result.wait(); // Wait until a reply arrives
            }
            if (result.timeout) { throw new TimeoutException("Tunnel construction timed out!"); }

            final TunnelBridge bridge = new TunnelBridge(this, id, result.chunking);
            active.put(id, bridge);
            return bridge;
        } finally {
            pendingInits.remove(id);
        }
    }

    // Closes a particular tunnel instance.
    public void closeBridge(final long id) {
        try {
            final TunnelBridge bridge = active.get(id);
            if (bridge == null) { throw new ProtocolException("Non-existent tunnel"); }
            active.remove(id);

            // Create a temporary object to store the tear-down result
            final CloseResult result = new CloseResult();
            pendingCloses.put(id, result);

            try {
                synchronized (result) {
                    // Send the tear-down request
                    protocol.send(OpCode.TUNNEL_CLOSE, () -> protocol.sendVarint(id));
                    result.wait(); // Wait until a reply arrives
                }
                if (!result.reason.isEmpty()) { throw new ProtocolException("Remote close failed: " + result.reason); }
            } finally {
                pendingInits.remove(id);
            }
        } catch (InterruptedException e) { throw new ProtocolException(e); }
    }

    public void handleTunnelInit() {
        System.out.println("Tunnel init inbound"); // FIXME
        final long id = protocol.receiveVarint();
        final long chunking = protocol.receiveVarint();

        // Don't do anything for now.
    }

    public void handleTunnelConfirm() {
        // Read the request id and fetch the pending construction result
        final long id = protocol.receiveVarint();
        final InitResult result = pendingInits.get(id);
        if (result == null) { return; } // Already dead? Thread got interrupted!

        // Read the rest of the response and fill the result accordingly
        result.timeout = protocol.receiveBoolean();
        if (!result.timeout) {
            result.chunking = protocol.receiveVarint();
        }
        System.out.println(result.timeout + " " + result.chunking); // FIXME

        // Wake the origin thread
        synchronized (result) {
            result.notify();
        }
    }

    public void sendTunnelConfirm(final long buildId, final long tunnelId) {
        protocol.send(OpCode.TUNNEL_CONFIRM, () -> {
            protocol.sendVarint(buildId);
            protocol.sendVarint(tunnelId);
        });
    }
   /*

    public void sendTunnelTransfer(final long tunnelId, @NotNull final byte... message)  {
        protocol.send(OpCode.TUNNEL_TRANSFER, () -> {
            protocol.sendVarint(tunnelId);
            protocol.sendBinary(message);
        });
    }

    public void handleTunnelTransfer()  {
        try {
            final long id = protocol.receiveVarint();
            final byte[] message = protocol.receiveBinary();

            final TunnelCallbackHandlers handler = callbacks.useCallbackHandler(id);
            handler.handleTunnelData(id, message);
        }
        catch (IllegalArgumentException e) {
            System.err.printf("No %s found!%n", TunnelCallbackHandlers.class.getSimpleName());
        }
    }

    public void sendTunnelAllow(final long tunnelId)  {
        protocol.send(OpCode.TUNNEL_ALLOW, () -> protocol.sendVarint(tunnelId));
    }

    public void handleTunnelAllow()  {
        try {
            final long id = protocol.receiveVarint();

            final TunnelCallbackHandlers handler = callbacks.useCallbackHandler(id);
            handler.handleTunnelAck(id);
        }
        catch (IllegalArgumentException e) {
            System.err.printf("No %s found!%n", TunnelCallbackHandlers.class.getSimpleName());
        }
    }

    public void sendTunnelClose(final long tunnelId)  {
        protocol.send(OpCode.TUNNEL_CLOSE, () -> protocol.sendVarint(tunnelId));
    }

    public void handleTunnelClose()  {
        try {
            final long id = protocol.receiveVarint();

            final TunnelCallbackHandlers handler = callbacks.useCallbackHandler(id);
            handler.handleTunnelClose(id);
        }
        catch (IllegalArgumentException e) {
            System.err.printf("No %s found!%n", TunnelCallbackHandlers.class.getSimpleName());
        }
    }*/
}