// Copyright (c) 2014 Project Iris. All rights reserved.
//
// The current language binding is an official support library of the Iris
// cloud messaging framework, and as such, the same licensing terms apply.
// For details please see http://iris.karalabe.com/downloads#License
package com.karalabe.iris.schemes;

public class TunnelExecutor {
  /*  // Tunnel async construction result.
    private static class InitResult {
        boolean timeout;
        long    chunking;
    }

    // Tunnel async tear-down result.
    private static class CloseResult {
        String reason;
    }

    private final RelayProtocol  protocol; // Network connection implementing the relay protocol
    private final ServiceHandler handler;  // Callback handler for processing inbound tunnels

    private final LongAdder               nextId        = new LongAdder(); // Unique identifier for the next tunnel
    private final Map<Long, InitResult>   pendingInits  = new ConcurrentHashMap<>(128); // Result objects for pending tunnel
    private final Map<Long, CloseResult>  pendingCloses = new ConcurrentHashMap<>(128); // Result objects for pending tunnel
    private final Map<Long, TunnelBridge> active        = new ConcurrentHashMap<>(128); // Currently active tunnels

    public TunnelExecutor(@NotNull final RelayProtocol protocol, @Nullable final ServiceHandler handler) {
        this.protocol = protocol;
        this.handler = handler;
    }

    public void close() throws Exception {}

    public TunnelBridge tunnel(final String cluster, final long timeout) throws IOException, InterruptedException, TimeoutException {
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
                // Wait until a reply arrives
                result.wait();
            }
            if (result.timeout) {
                throw new TimeoutException("Tunnel construction timed out!");
            }
            TunnelBridge bridge = new TunnelBridge(this, id, result.chunking);
            active.put(id, bridge);
            return bridge;
        }
        finally {
            pendingInits.remove(id);
        }
    }

    // Closes a particular tunnel instance.
    public void closeBridge(final long id) throws IOException, InterruptedException {
        TunnelBridge bridge = active.get(id);
        if (bridge == null) {
            throw new IllegalStateException("Non-existent tunnel");
        }
        active.remove(id);

        // Create a temporary object to store the tear-down result
        final CloseResult result = new CloseResult();
        pendingCloses.put(id, result);

        try {
            synchronized (result) {
                // Send the tear-down request
                protocol.send(OpCode.TUNNEL_CLOSE, () -> {
                    protocol.sendVarint(id);
                });
                // Wait until a reply arrives
                result.wait();
            }
            if (!result.reason.isEmpty()) {
                throw new RemoteException("Remote close failed: " + result.reason);
            }
        }
        finally {
            pendingInits.remove(id);
        }
    }

    public void handleTunnelInit() throws IOException {
        System.out.println("Tunnel init inbound");
        final long id = protocol.receiveVarint();
        final long chunking = protocol.receiveVarint();

        // Don't do anything for now.
    }

    public void handleTunnelConfirm() throws IOException {
        // Read the request id and fetch the pending construction result
        final long id = protocol.receiveVarint();
        final InitResult result = pendingInits.get(id);
        if (result == null) {
            // Already dead? Thread got interrupted!
            return;
        }
        // Read the rest of the response and fill the result accordingly
        result.timeout = protocol.receiveBoolean();
        if (!result.timeout) {
            result.chunking = protocol.receiveVarint();
        }
        System.out.println(result.timeout + " " + result.chunking);

        // Wake the origin thread
        synchronized (result) {
            result.notify();
        }
    }

    public void sendTunnelConfirm(final long buildId, final long tunnelId) throws IOException {
        protocol.send(OpCode.TUNNEL_CONFIRM, () -> {
            protocol.sendVarint(buildId);
            protocol.sendVarint(tunnelId);
        });
    }

*/
   /*

    public void handleTunnelConfirm() throws IOException {
        try {
            final long id = protocol.receiveVarint();
            final boolean hasTimedOut = protocol.receiveBoolean();

            final TunnelCallbackHandlers handler = callbacks.useCallbackHandler(id);
            if (hasTimedOut) {
                handler.handleTunnelReply(id, 0, true);
            } else {

                final long bufferSize = protocol.receiveVarint();
                handler.handleTunnelReply(id, bufferSize, false);
            }
        }
        catch (IllegalArgumentException e) {
            System.err.printf("No %s found!%n", TunnelCallbackHandlers.class.getSimpleName());
        }
    }

    public void sendTunnelTransfer(final long tunnelId, @NotNull final byte[] message) throws IOException {
        protocol.send(OpCode.TUNNEL_TRANSFER, () -> {
            protocol.sendVarint(tunnelId);
            protocol.sendBinary(message);
        });
    }

    public void handleTunnelTransfer() throws IOException {
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

    public void sendTunnelAllow(final long tunnelId) throws IOException {
        protocol.send(OpCode.TUNNEL_ALLOW, () -> protocol.sendVarint(tunnelId));
    }

    public void handleTunnelAllow() throws IOException {
        try {
            final long id = protocol.receiveVarint();

            final TunnelCallbackHandlers handler = callbacks.useCallbackHandler(id);
            handler.handleTunnelAck(id);
        }
        catch (IllegalArgumentException e) {
            System.err.printf("No %s found!%n", TunnelCallbackHandlers.class.getSimpleName());
        }
    }

    public void sendTunnelClose(final long tunnelId) throws IOException {
        protocol.send(OpCode.TUNNEL_CLOSE, () -> protocol.sendVarint(tunnelId));
    }

    public void handleTunnelClose() throws IOException {
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