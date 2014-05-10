package com.karalabe.iris;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ProtocolException;
import java.net.Socket;

/*
 * Message relay between the local app and the local iris node.
 **/
public class Connection extends ProtocolBase implements ConnectionApi {
    private static final String VERSION = "v1.0";

    private final Socket            socket;  // Network connection to the iris node
    private final ConnectionHandler handler; // Callback handler

    public Connection(int port, @NotNull String clusterName, @Nullable ConnectionHandler handler) throws IOException {
        this(new Socket(InetAddress.getLoopbackAddress(), port), handler);

        init(clusterName);
        processInit();
    }

    public Connection(@NotNull final Socket socket, @Nullable ConnectionHandler handler) throws IOException {
        super(socket.getInputStream(), socket.getOutputStream());

        this.socket = socket;
        this.handler = handler;
    }

    // TODO precessors; should be segregated

    private void processInit() throws IOException {
        if (recvByte() != OpCode.INIT.getOrdinal()) { throw new ProtocolException("Protocol violation"); }
    }

    private Object processBroadcast() throws IOException {
        final byte[] message = recvBinary();

        handler.handleBroadcast(message);
        return null;
    }

    private Object processRequest() throws IOException {
        final long reqId = recvVarint();
        final byte[] message = recvBinary();

        handler.handleRequest(reqId, message);
        return null;
    }

    private Object processReply() throws IOException {
        final long reqId = recvVarint();
        final boolean hasTimedOut = recvBoolean();

        if (hasTimedOut) {
            handler.handleReply(reqId, null);
        } else {

            final byte[] reply = recvBinary();
            handler.handleReply(reqId, reply);
        }
        return null;
    }

    private Object processPublish() throws IOException {
        final String topic = recvString();
        final byte[] message = recvBinary();

        handler.handlePublish(topic, message);
        return null;
    }

    private Object processTunnelRequest() throws IOException {
        final long tmpId = recvVarint();
        final long bufferSize = recvVarint();

        handler.handleTunnelRequest(tmpId, bufferSize);
        return null;
    }

    private Object processTunnelReply() throws IOException {
        final long tunId = recvVarint();
        final boolean hasTimedOut = recvBoolean();

        if (hasTimedOut) {
            handler.handleTunnelReply(tunId, 0, true);
        } else {

            final long bufferSize = recvVarint();
            handler.handleTunnelReply(tunId, bufferSize, false);
        }
        return null;
    }

    private Object processTunnelData() throws IOException {
        final long tunId = recvVarint();
        final byte[] message = recvBinary();

        handler.handleTunnelData(tunId, message);
        return null;
    }

    private Object processTunnelAck() throws IOException {
        final long tunId = recvVarint();

        handler.handleTunnelAck(tunId);
        return null;
    }

    private Object processTunnelClose() throws IOException {
        final long tunId = recvVarint();

        handler.handleTunnelClose(tunId);
        return null;
    }

    public void process() throws Exception {
        // TODO process all messages
        try {
            final OpCode opCode = OpCode.valueOf(recvByte());
            switch (opCode) {
                case BROADCAST:
                    processBroadcast();
                    break;

                case REQUEST:
                    processRequest();
                    break;
                case REPLY:
                    processReply();
                    break;

                case PUBLISH:
                    processBroadcast();
                    break;

                case TUNNEL_REQUEST:
                    processTunnelRequest();
                    break;
                case TUNNEL_REPLY:
                    processTunnelReply();
                    break;
                case TUNNEL_DATA:
                    processTunnelData();
                    break;
                case TUNNEL_ACK:
                    processTunnelAck();
                    break;
                case TUNNEL_CLOSE:
                    processTunnelClose();
                    break;

                case CLOSE:
                    return;

                default:
                case INIT:
                    throw new IllegalStateException("Illegal opcode received: " + opCode);
            }
        }
        finally {
            close();
        }
    }

    private void init(@NotNull final String clusterName) throws IOException {
        Validators.validateClusterName(clusterName);

        sendInit(clusterName);
    }

    private void sendInit(final String clusterName) throws IOException {
        synchronized (socketOut) {
            sendByte(OpCode.INIT.getOrdinal());
            sendString(VERSION);
            sendString(clusterName);
            sendFlush();
        }
    }

    @Override public void broadcast(@NotNull final String clusterName, @NotNull final byte[] message) throws IOException {
        Validators.validateClusterName(clusterName);
        Validators.validateMessage(message);

        sendBroadcast(clusterName, message);
    }

    private void sendBroadcast(@NotNull final String clusterName, @NotNull final byte[] message) throws IOException {
        synchronized (socketOut) {
            sendByte(OpCode.BROADCAST.getOrdinal());
            sendString(clusterName);
            sendBinary(message);
            sendFlush();
        }
    }

    @NotNull @Override public byte[] request(@NotNull final String clusterName, @NotNull final byte[] request, final long timeOutMillis) throws IOException {
        Validators.validateClusterName(clusterName);
        Validators.validateMessage(request);

        return sendRequest(0, clusterName, request, timeOutMillis); // TODO
    }

    private byte[] sendRequest(final long requestId, final String clusterName, final byte[] request, final long timeOutMillis) throws IOException {
        synchronized (socketOut) {
            sendByte(OpCode.REQUEST.getOrdinal());
            sendVarint(requestId);
            sendString(clusterName);
            sendBinary(request);
            sendVarint(timeOutMillis);
            sendFlush();
        }
        return null;
    }

    private byte[] sendReply(final long requestId, final byte[] request) throws IOException {
        synchronized (socketOut) {
            sendByte(OpCode.REPLY.getOrdinal());
            sendVarint(requestId);
            sendBinary(request);
            sendFlush();
        }
        return null;
    }

    @Override public void subscribe(@NotNull final String topic, @NotNull final SubscriptionHandler handler) throws IOException {
        Validators.validateTopic(topic);

        sendSubscribe(topic);
    }

    private void sendSubscribe(@NotNull final String topic) throws IOException {
        synchronized (socketOut) {
            sendByte(OpCode.SUBSCRIBE.getOrdinal());
            sendString(topic);
            sendFlush();
        }
    }

    @Override public void unsubscribe(@NotNull final String topic) throws IOException {
        Validators.validateTopic(topic);

        sendUnsubscribe(topic);
    }

    private void sendUnsubscribe(@NotNull final String topic) throws IOException {
        synchronized (socketOut) {
            sendByte(OpCode.UNSUBSCRIBE.getOrdinal());
            sendString(topic);
            sendFlush();
        }
    }

    private void sendClose() throws IOException {
        synchronized (socketOut) {
            sendByte(OpCode.CLOSE.getOrdinal());
            sendFlush();
        }
    }

    @Override public void publish(@NotNull final String topic, @NotNull final byte[] message) throws IOException {
        Validators.validateTopic(topic);
        Validators.validateMessage(message);

        sendPublish(topic, message);
    }

    private void sendPublish(@NotNull final String topic, @NotNull final byte[] message) throws IOException {
        synchronized (socketOut) {
            sendByte(OpCode.PUBLISH.getOrdinal());
            sendString(topic);
            sendBinary(message);
            sendFlush();
        }
    }

    @Override public Tunnel tunnel(@NotNull final String clusterName, final long timeOutMillis) throws IOException {
        Validators.validateClusterName(clusterName);

        return sendTunnel(0, clusterName, 0, timeOutMillis); // TODO
    }

    @NotNull private Tunnel sendTunnel(final long tunnelId, @NotNull final String clusterName, final long bufferSize, final long timeOutMillis) throws IOException {
        synchronized (socketOut) {
            sendByte(OpCode.TUNNEL_REQUEST.getOrdinal());
            sendVarint(tunnelId);
            sendString(clusterName);
            sendVarint(bufferSize); // TODO buf?
            sendVarint(timeOutMillis);
            sendFlush();
        }
        return null;
    }

    private Tunnel sendTunnelReply(final long tempId, final long tunnelId, final long bufferSize) throws IOException {
        synchronized (socketOut) {
            sendByte(OpCode.TUNNEL_REPLY.getOrdinal());
            sendVarint(tempId);     // TODO huh?
            sendVarint(tunnelId);
            sendVarint(bufferSize); // TODO buf?
            sendFlush();
        }
        return null;
    }

    private Tunnel sendTunnelData(final long tunnelId, @NotNull final byte[] message) throws IOException {
        synchronized (socketOut) {
            sendByte(OpCode.TUNNEL_DATA.getOrdinal());
            sendVarint(tunnelId);
            sendBinary(message);
            sendFlush();
        }
        return null;
    }

    private Tunnel sendTunnelAck(final long tunnelId) throws IOException {
        synchronized (socketOut) {
            sendByte(OpCode.TUNNEL_ACK.getOrdinal());
            sendVarint(tunnelId);
            sendFlush();
        }
        return null;
    }

    private Tunnel sendTunnelClose(final long tunnelId) throws IOException {
        synchronized (socketOut) {
            sendByte(OpCode.TUNNEL_CLOSE.getOrdinal());
            sendVarint(tunnelId);
            sendFlush();
        }
        return null;
    }

    @Override public void close() throws Exception {
        super.close();
        socket.close();
    }
}