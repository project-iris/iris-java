// Copyright = c 2014 Project Iris. All rights reserved.
//
// The current language binding is an official support library of the Iris
// cloud messaging framework; and as such; the same licensing terms apply.
// For details please see http://iris.karalabe.com/downloads#License

// Contains the wire protocol for communicating with the Iris relay endpoint.

// The specification version implemented is v1.0-draft2, available at:
// http://iris.karalabe.com/specs/relay-protocol-v1.0-draft2.pdf
package com.karalabe.iris.protocol;

import com.karalabe.iris.ServiceHandler;
import com.karalabe.iris.schemes.BroadcastScheme;
import com.karalabe.iris.schemes.PublishScheme;
import com.karalabe.iris.schemes.RequestScheme;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ProtocolException;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.util.concurrent.atomic.AtomicInteger;

// Wire protocol for communicating with the Iris relay endpoint.
public class RelayProtocol {
    // Relay protocol packet opcodes
    private static final byte OP_INIT  = 0x00;           // Out: connection initiation            | In: connection acceptance
    private static final byte OP_DENY  = 0x01;           // Out: <never sent>                     | In: connection refusal
    private static final byte OP_CLOSE = 0x02;           // Out: connection tear-down initiation  | In: connection tear-down notification

    private static final byte OP_BROADCAST = 0x03;       // Out: application broadcast initiation | In: application broadcast delivery
    private static final byte OP_REQUEST   = 0x04;       // Out: application request initiation   | In: application request delivery
    private static final byte OP_REPLY     = 0x05;       // Out: application reply initiation     | In: application reply delivery

    private static final byte OP_SUBSCRIBE   = 0x06;     // Out: topic subscription               | In: <never received>
    private static final byte OP_UNSUBSCRIBE = 0x07;     // Out: topic subscription removal       | In: <never received>
    private static final byte OP_PUBLISH     = 0x08;     // Out: topic event publish              | In: topic event delivery

    private static final byte OP_TUNNEL_INIT     = 0x09; // Out: tunnel construction request      | In: tunnel initiation
    private static final byte OP_TUNNEL_CONFIRM  = 0x0a; // Out: tunnel confirmation              | In: tunnel construction result
    private static final byte OP_TUNNEL_ALLOW    = 0x0b; // Out: tunnel transfer allowance        | In: <same as out>
    private static final byte OP_TUNNEL_TRANSFER = 0x0c; // Out: tunnel data exchange             | In: <same as out>
    private static final byte OP_TUNNEL_CLOSE    = 0x0d; // Out: tunnel termination request       | In: tunnel termination notification

    // Protocol constants
    private static final String  PROTOCOL_VERSION = "v1.0-draft2";
    private static final Charset PROTOCOL_CHARSET = StandardCharsets.UTF_8;
    private static final String  CLIENT_MAGIC     = "iris-client-magic";
    private static final String  RELAY_MAGIC      = "iris-relay-magic";

    // Network layer fields
    private final Socket           socket;     // Network connection to the iris node
    private final DataInputStream  socketIn;   // Input buffer of the network socket
    private final DataOutputStream socketOut;  // Output buffer of the network socket
    private final AtomicInteger    socketWait; // Counter for the pending writes (batch before flush)

    // Connects to a local relay endpoint on port and registers as cluster.
    public RelayProtocol(final int port, final String cluster) throws IOException {
        // Connect to the iris relay node
        socket = new Socket(InetAddress.getLoopbackAddress(), port);
        socketIn = new DataInputStream(socket.getInputStream());
        socketOut = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        socketWait = new AtomicInteger();

        // Initialize the connection and wait for a confirmation
        sendInit(cluster);
        processInit();
    }

    // Serializes a single byte into the relay connection.
    private void sendByte(byte data) throws IOException {
        socketOut.writeByte(data);
    }

    // Serializes a boolean into the relay connection.
    private void sendBoolean(boolean data) throws IOException {
        sendByte((byte) (data ? 0x01 : 0x00));
    }

    // Serializes a variable int using base 128 encoding into the relay connection.
    @SuppressWarnings("MagicNumber")
    private void sendVarint(long data) throws IOException {
        while (data > 127) {
            // Internal byte, set the continuation flag and send
            sendByte((byte) (128 | (data & 127)));
            data >>>= 7;
        }
        // Final byte, send and return
        sendByte((byte) data);
    }

    // Serializes a length-tagged binary array into the relay connection.
    private void sendBinary(final byte[] data) throws IOException {
        sendVarint(data.length);
        socketOut.write(data);
    }

    // Serializes a length-tagged string into the relay connection.
    private void sendString(final String data) throws IOException {
        sendBinary(data.getBytes(PROTOCOL_CHARSET));
    }

    // Closure for defining a runnable that permits IOExceptions.
    @FunctionalInterface public interface Closure {
        void run() throws IOException;
    }

    // Serializes a packet through a closure into the relay connection.
    private void sendPacket(byte opCode, Closure closure) throws IOException {
        // Increment the pending write count
        socketWait.addAndGet(1);

        // Acquire the socket lock and send the packet itself
        synchronized (socketOut) {
            sendByte(opCode);
            closure.run();
        }
        // Flush the stream if no more messages are pending
        if (socketWait.addAndGet(-1) == 0) {
            socketOut.flush();
        }
    }

    // Sends a connection initiation.
    private void sendInit(final String cluster) throws IOException {
        sendPacket(OP_INIT, () -> {
            sendString(CLIENT_MAGIC);
            sendString(PROTOCOL_VERSION);
            sendString(cluster);
        });
    }

    // Sends a connection tear-down initiation.
    public void sendClose() throws IOException {
        sendPacket(OP_CLOSE, () -> {});
    }

    // Sends an application broadcast initiation.
    public void sendBroadcast(final String cluster, final byte[] message) throws IOException {
        sendPacket(OP_BROADCAST, () -> {
            sendString(cluster);
            sendBinary(message);
        });
    }

    // Sends an application request initiation.
    public void sendRequest(final long id, final String cluster, byte[] request, long timeout) throws IOException {
        sendPacket(OP_REQUEST, () -> {
            sendVarint(id);
            sendString(cluster);
            sendBinary(request);
            sendVarint(timeout);
        });
    }

    // Sends an application reply initiation.
    public void sendReply(final long id, final byte[] response, final String fault) throws IOException {
        sendPacket(OP_REPLY, () -> {
            sendVarint(id);
            sendBoolean(fault == null);
            if (fault == null) {
                sendBinary(response);
            } else {
                sendString(fault);
            }
        });
    }

    // Sends a topic subscription.
    public void sendSubscribe(final String topic) throws IOException {
        sendPacket(OP_SUBSCRIBE, () -> sendString(topic));
    }

    // Sends a topic subscription removal.
    public void sendUnsubscribe(final String topic) throws IOException {
        sendPacket(OP_UNSUBSCRIBE, () -> sendString(topic));
    }

    // Sends a topic event publish.
    public void sendPublish(final String topic, final byte[] event) throws IOException {
        sendPacket(OP_PUBLISH, () -> {
            sendString(topic);
            sendBinary(event);
        });
    }

    // Retrieves a single byte from the relay connection.
    private byte receiveByte() throws IOException {
        return socketIn.readByte();
    }

    // Retrieves a boolean from the relay connection.
    private boolean receiveBoolean() throws IOException {
        final byte data = receiveByte();
        switch (data) {
            case 0:
                return false;
            case 1:
                return true;
            default:
                throw new ProtocolException("Invalid boolean value: " + data);
        }
    }

    // Retrieves a variable int in base 128 encoding from the relay connection.
    @SuppressWarnings("MagicNumber")
    private long receiveVarint() throws IOException {
        long result = 0;
        for (int shift = 0; ; shift += 7) {
            final byte chunk = receiveByte();
            result += ((long) (chunk & 127)) << shift;
            if ((chunk & 128) == 0) {
                break;
            }
        }
        return result;
    }

    // Retrieves a length-tagged binary array from the relay connection.
    private byte[] receiveBinary() throws IOException {
        final byte[] result = new byte[(int) receiveVarint()];
        socketIn.readFully(result);
        return result;
    }

    // Retrieves a length-tagged string from the relay connection.
    private String receiveString() throws IOException {
        return new String(receiveBinary(), PROTOCOL_CHARSET);
    }

    // Retrieves a connection initiation response (either accept or deny).
    private String processInit() throws IOException {
        final byte opCode = receiveByte();

        // Verify the opcode validity and relay magic string
        if (opCode != OP_INIT && opCode != OP_DENY) {
            throw new ProtocolException("Invalid init response opcode: " + opCode);
        }
        final String relayMagic = receiveString();
        if (!RELAY_MAGIC.equals(relayMagic)) {
            throw new ProtocolException("Invalid relay magic: " + relayMagic);
        }
        // Depending on success or failure, proceed and return
        switch (opCode) {
            case OP_INIT:
                // Read the highest supported protocol version
                return receiveString();

            case OP_DENY:
                // Read the reason for connection denial
                throw new ProtocolException("Connection denied: " + receiveString());

            default:
                throw new RuntimeException("Unreachable code!");
        }
    }

    // Retrieves a connection tear-down notification.
    private String processClose() throws IOException {
        return receiveString();
    }

    // Retrieves an application broadcast delivery.
    private void processBroadcast(final BroadcastScheme scheme) throws IOException {
        final byte[] message = receiveBinary();
        scheme.handleBroadcast(message);
    }

    // Retrieves an application request delivery.
    private void processRequest(final RequestScheme scheme) throws IOException {
        final long id = receiveVarint();
        final byte[] request = receiveBinary();
        final long timeout = receiveVarint();

        scheme.handleRequest(id, request, timeout);
    }

    // Retrieves an application reply delivery.
    private void processReply(final RequestScheme scheme) throws IOException {
        final long id = receiveVarint();

        final boolean timeout = receiveBoolean();
        if (timeout) {
            scheme.handleReply(id, null, null);
            return;
        }

        final boolean success = receiveBoolean();
        if (success) {
            final byte[] reply = receiveBinary();
            scheme.handleReply(id, reply, null);
        } else {
            final String fault = receiveString();
            scheme.handleReply(id, null, fault);
        }
    }

    public void processPublish(final PublishScheme scheme) throws IOException {
        final String topic = receiveString();
        final byte[] event = receiveBinary();

        scheme.handlePublish(topic, event);
    }

    // Retrieves messages from the client connection and keeps processing them until
    // either the relay closes (graceful close) or the connection drops.
    public void process(final ServiceHandler handler, final BroadcastScheme broadcaster, final RequestScheme requester, final PublishScheme publisher) {
        try {
            boolean closed = false;
            while (!closed) {
                final byte opCode = receiveByte();
                switch (opCode) {
                    case OP_BROADCAST:
                        processBroadcast(broadcaster);
                        break;

                    case OP_REQUEST:
                        processRequest(requester);
                        break;
                    case OP_REPLY:
                        processReply(requester);
                        break;

                    case OP_PUBLISH:
                        processPublish(publisher);
                        break;

                    case OP_TUNNEL_INIT:
                        //tunneler.handleTunnelInit();
                        break;
                    case OP_TUNNEL_CONFIRM:
                        //tunneler.handleTunnelConfirm();
                        break;
                    case OP_TUNNEL_ALLOW:
                        //tunneler.handleTunnelAllow();
                        break;
                    case OP_TUNNEL_TRANSFER:
                        //tunneler.handleTunnelTransfer();
                        break;
                    case OP_TUNNEL_CLOSE:
                        //tunneler.handleTunnelClose();
                        break;

                    case OP_CLOSE:
                        // Retrieve any reason for remote closure
                        final String reason = processClose();
                        if (reason.length() > 0) {
                            throw new RemoteException("Connection dropped: " + reason);
                        }
                        closed = true;
                        break;

                    default:
                        throw new ProtocolException("Unknown opcode: " + opCode);
                }
            }
        } catch (Exception e) {
            if (handler != null) {
                handler.handleDrop(e);
            }
        } finally {
            // Close the socket and signal termination to all blocked threads
            try {
                socketOut.close();
            } catch (Exception e) {}
            try {
                socketIn.close();
            } catch (Exception e) {}
            try {
                socket.close();
            } catch (Exception e) {}
        }
    }
}