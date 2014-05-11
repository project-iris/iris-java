package com.karalabe.iris;

import com.karalabe.iris.callback.CallbackHandlerRegistry;
import com.karalabe.iris.callback.CallbackRegistry;
import com.karalabe.iris.callback.StaticCallbackHandler;
import com.karalabe.iris.callback.handlers.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ProtocolException;
import java.net.Socket;

/*
 * Message relay between the local app and the local iris node.
 **/
public class Connection extends ProtocolBase implements ConnectionApi, CallbackRegistry {
    private static final String VERSION = "v1.0";

    private final CallbackHandlerRegistry callbacks = new CallbackHandlerRegistry();
    private final Socket socket;  // Network connection to the iris node

    public Connection(@NotNull final Socket socket) throws IOException {
        super(socket.getInputStream(), socket.getOutputStream());

        this.socket = socket;
    }

    public Connection(int port, @NotNull String clusterName) throws IOException {
        this(new Socket(InetAddress.getLoopbackAddress(), port));

        init(clusterName);
        processInit();
    }

    public void addCallbackHandler(@NotNull final StaticCallbackHandler callbackHandler) {
        callbacks.addCallbackHandler(callbackHandler);
    }

    // TODO precessors; should be segregated

    private void processInit() throws IOException {
        if (recvByte() != OpCode.INIT.getOrdinal()) { throw new ProtocolException("Protocol violation"); }
    }

    private void processBroadcast() throws IOException {
        try {
            final byte[] message = recvBinary();

            final BroadcastCallbackHandler handler = callbacks.useCallbackHandler(BroadcastCallbackHandler.getBroadcastId());
            handler.handleBroadcast(message);
        }
        catch (IllegalArgumentException e) {
            System.err.printf("No %s found!%n", BroadcastCallbackHandler.class.getSimpleName());
        }
    }

    private void processRequest() throws IOException {
        try {
            final long requestId = recvVarint();
            final byte[] message = recvBinary();

            final RequestCallbackHandler handler = callbacks.useCallbackHandler(requestId);
            handler.handleRequest(requestId, message);
        }
        catch (IllegalArgumentException e) {
            System.err.printf("No %s found!%n", RequestCallbackHandler.class.getSimpleName());
        }
    }

    private void processReply() throws IOException {
        try {
            final long requestId = recvVarint();
            final boolean hasTimedOut = recvBoolean();

            final ReplyCallbackHandler handler = callbacks.useCallbackHandler(requestId);
            if (hasTimedOut) {
                handler.handleReply(requestId, null);
            } else {

                final byte[] reply = recvBinary();
                handler.handleReply(requestId, reply);
            }
        }
        catch (IllegalArgumentException e) {
            System.err.printf("No %s found!%n", ReplyCallbackHandler.class.getSimpleName());
        }
    }

    private void processPublish() throws IOException {
        try {
            final String topic = recvString();
            final byte[] message = recvBinary();

            final PublishCallbackHandler handler = callbacks.useCallbackHandler(PublishCallbackHandler.getPublishId());
            handler.handlePublish(topic, message);
        }
        catch (IllegalArgumentException e) {
            System.err.printf("No %s found!%n", PublishCallbackHandler.class.getSimpleName());
        }
    }

    private void processTunnelRequest() throws IOException {
        try {
            final long tunId = recvVarint(); // TODO tmpId?
            final long bufferSize = recvVarint();

            final TunnelCallbackHandlers handler = callbacks.useCallbackHandler(tunId);
            handler.handleTunnelRequest(tunId, bufferSize);
        }
        catch (IllegalArgumentException e) {
            System.err.printf("No %s found!%n", TunnelCallbackHandlers.class.getSimpleName());
        }
    }

    private void processTunnelReply() throws IOException {
        try {
            final long tunId = recvVarint();
            final boolean hasTimedOut = recvBoolean();

            final TunnelCallbackHandlers handler = callbacks.useCallbackHandler(tunId);
            if (hasTimedOut) {
                handler.handleTunnelReply(tunId, 0, true);
            } else {

                final long bufferSize = recvVarint();
                handler.handleTunnelReply(tunId, bufferSize, false);
            }
        }
        catch (IllegalArgumentException e) {
            System.err.printf("No %s found!%n", TunnelCallbackHandlers.class.getSimpleName());
        }
    }

    private void processTunnelData() throws IOException {
        try {
            final long tunId = recvVarint();
            final byte[] message = recvBinary();

            final TunnelCallbackHandlers handler = callbacks.useCallbackHandler(tunId);
            handler.handleTunnelData(tunId, message);
        }
        catch (IllegalArgumentException e) {
            System.err.printf("No %s found!%n", TunnelCallbackHandlers.class.getSimpleName());
        }
    }

    private void processTunnelAck() throws IOException {
        try {
            final long tunId = recvVarint();

            final TunnelCallbackHandlers handler = callbacks.useCallbackHandler(tunId);
            handler.handleTunnelAck(tunId);
        }
        catch (IllegalArgumentException e) {
            System.err.printf("No %s found!%n", TunnelCallbackHandlers.class.getSimpleName());
        }
    }

    private void processTunnelClose() throws IOException {
        try {
            final long tunId = recvVarint();

            final TunnelCallbackHandlers handler = callbacks.useCallbackHandler(tunId);
            handler.handleTunnelClose(tunId);
        }
        catch (IllegalArgumentException e) {
            System.err.printf("No %s found!%n", TunnelCallbackHandlers.class.getSimpleName());
        }
    }

    public void process() throws Exception {
        // TODO process all messages
        // Producer/consumer?
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
                    processPublish();
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

    // Send data
    // TODO separate different abstraction layers

    private void init(@NotNull final String clusterName) throws IOException {
        Validators.validateClusterName(clusterName);

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

        synchronized (socketOut) {
            sendByte(OpCode.BROADCAST.getOrdinal());
            sendString(clusterName);
            sendBinary(message);
            sendFlush();
        }
    }

    @Override public void request(@NotNull final String clusterName, @NotNull final byte[] request, final long timeOutMillis, RequestCallbackHandler callbackHandler) throws IOException {
        Validators.validateClusterName(clusterName);
        Validators.validateMessage(request);

        final Long requestId = callbacks.addCallbackHandler(callbackHandler);
        synchronized (socketOut) {
            sendByte(OpCode.REQUEST.getOrdinal());
            sendVarint(requestId);
            sendString(clusterName);
            sendBinary(request);
            sendVarint(timeOutMillis);
            sendFlush();
        }
    }

    private void sendReply(final long requestId, final byte[] request) throws IOException {
        synchronized (socketOut) {
            sendByte(OpCode.REPLY.getOrdinal());
            sendVarint(requestId);
            sendBinary(request);
            sendFlush();
        }
    }

    @Override public void subscribe(@NotNull final String topic, @NotNull final SubscriptionHandler handler) throws IOException {
        Validators.validateTopic(topic);

        synchronized (socketOut) {
            sendByte(OpCode.SUBSCRIBE.getOrdinal());
            sendString(topic);
            sendFlush();
        }
    }

    @Override public void unsubscribe(@NotNull final String topic) throws IOException {
        Validators.validateTopic(topic);

        synchronized (socketOut) {
            sendByte(OpCode.UNSUBSCRIBE.getOrdinal());
            sendString(topic);
            sendFlush();
        }
    }

    @Override public void publish(@NotNull final String topic, @NotNull final byte[] message) throws IOException {
        Validators.validateTopic(topic);
        Validators.validateMessage(message);

        synchronized (socketOut) {
            sendByte(OpCode.PUBLISH.getOrdinal());
            sendString(topic);
            sendBinary(message);
            sendFlush();
        }
    }

    @Override public void tunnel(@NotNull final String clusterName, final long timeOutMillis, TunnelCallbackHandlers callbackHandlers) throws IOException {
        Validators.validateClusterName(clusterName);

        final int bufferSize = 0; // TODO
        final Long requestId = callbacks.addCallbackHandler(callbackHandlers);
        synchronized (socketOut) {
            sendByte(OpCode.TUNNEL_REQUEST.getOrdinal());
            sendVarint(requestId);
            sendString(clusterName);
            sendVarint(bufferSize); // TODO buf?
            sendVarint(timeOutMillis);
            sendFlush();
        }
    }

    private void sendTunnelReply(final long tempId, final long tunnelId, final long bufferSize) throws IOException {
        synchronized (socketOut) {
            sendByte(OpCode.TUNNEL_REPLY.getOrdinal());
            sendVarint(tempId);     // TODO huh?
            sendVarint(tunnelId);
            sendVarint(bufferSize); // TODO buf?
            sendFlush();
        }
    }

    private void sendTunnelData(final long tunnelId, @NotNull final byte[] message) throws IOException {
        synchronized (socketOut) {
            sendByte(OpCode.TUNNEL_DATA.getOrdinal());
            sendVarint(tunnelId);
            sendBinary(message);
            sendFlush();
        }
    }

    private void sendTunnelAck(final long tunnelId) throws IOException {
        synchronized (socketOut) {
            sendByte(OpCode.TUNNEL_ACK.getOrdinal());
            sendVarint(tunnelId);
            sendFlush();
        }
    }

    private void sendTunnelClose(final long tunnelId) throws IOException {
        synchronized (socketOut) {
            sendByte(OpCode.TUNNEL_CLOSE.getOrdinal());
            sendVarint(tunnelId);
            sendFlush();
        }
    }

    private void sendClose() throws IOException {
        synchronized (socketOut) {
            sendByte(OpCode.CLOSE.getOrdinal());
            sendFlush();
        }
    }

    @Override public void close() throws Exception {
        super.close();
        socket.close();
    }
}