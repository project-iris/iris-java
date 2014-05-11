package com.karalabe.iris;

import com.karalabe.iris.callback.CallbackHandlerRegistry;
import com.karalabe.iris.callback.CallbackRegistry;
import com.karalabe.iris.callback.InstanceCallbackHandler;
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
        handleInit();
    }

    private void init(@NotNull final String clusterName) throws IOException {
        Validators.validateClusterName(clusterName);

        synchronized (socketOut) {
            sendByte(OpCode.INIT.getOrdinal());
            sendString(VERSION);
            sendString(clusterName);
            sendFlush();
        }
    }

    private void handleInit() throws IOException {
        if (recvByte() != OpCode.INIT.getOrdinal()) {
            throw new ProtocolException("Protocol violation");
        }
    }

    // TODO extract to BroadcastProtocol

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

    private void handleBroadcast() throws IOException {
        try {
            final byte[] message = recvBinary();

            final BroadcastCallbackHandler handler = callbacks.useCallbackHandler(BroadcastCallbackHandler.getBroadcastId());
            handler.handleEvent(message);
        }
        catch (IllegalArgumentException e) {
            System.err.printf("No %s found!%n", BroadcastCallbackHandler.class.getSimpleName());
        }
    }

    // TODO extract to RequestProtocol

    @Override public void request(@NotNull final String clusterName, @NotNull final byte[] request, final long timeOutMillis, RequestCallbackHandler callbackHandler) throws IOException {
        Validators.validateClusterName(clusterName);
        Validators.validateMessage(request);

        final Long requestId = addCallbackHandler(callbackHandler);
        synchronized (socketOut) {
            sendByte(OpCode.REQUEST.getOrdinal());
            sendVarint(requestId);
            sendString(clusterName);
            sendBinary(request);
            sendVarint(timeOutMillis);
            sendFlush();
        }
    }

    private void handleRequest() throws IOException {
        try {
            final long requestId = recvVarint();
            final byte[] message = recvBinary();

            final RequestCallbackHandler handler = callbacks.useCallbackHandler(requestId);
            handler.handleEvent(requestId, message);
        }
        catch (IllegalArgumentException e) {
            System.err.printf("No %s found!%n", RequestCallbackHandler.class.getSimpleName());
        }
    }

    // TODO extract to ReplyProtocol
    // TODO not sure how/where this fits in ...

    private void sendReply(final long requestId, final byte[] request) throws IOException {
        synchronized (socketOut) {
            sendByte(OpCode.REPLY.getOrdinal());
            sendVarint(requestId);
            sendBinary(request);
            sendFlush();
        }
    }

    private void handleReply() throws IOException {
        try {
            final long requestId = recvVarint();
            final boolean hasTimedOut = recvBoolean();

            final ReplyCallbackHandler handler = callbacks.useCallbackHandler(requestId);
            if (hasTimedOut) {
                handler.handleEvent(requestId, null);
            } else {

                final byte[] reply = recvBinary();
                handler.handleEvent(requestId, reply);
            }
        }
        catch (IllegalArgumentException e) {
            System.err.printf("No %s found!%n", ReplyCallbackHandler.class.getSimpleName());
        }
    }

    // TODO extract to PublishSubscribeProtocol

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

    private void handlePublish() throws IOException {
        try {
            final String topic = recvString();
            final byte[] message = recvBinary();

            final PublishCallbackHandler handler = callbacks.useCallbackHandler(PublishCallbackHandler.getPublishId());
            handler.handleEvent(topic, message);
        }
        catch (IllegalArgumentException e) {
            System.err.printf("No %s found!%n", PublishCallbackHandler.class.getSimpleName());
        }
    }

    @Override public void subscribe(@NotNull final String topic, @NotNull final SubscriptionHandler handler) throws IOException {
        Validators.validateTopic(topic);

        final Long subscriptionId = addCallbackHandler(handler);// TODO is the topic the id?
        synchronized (socketOut) {
            sendByte(OpCode.SUBSCRIBE.getOrdinal());
            sendVarint(subscriptionId); // TODO
            sendString(topic);
            sendFlush();
        }
    }

    @Override public void unsubscribe(@NotNull final String topic, @NotNull final SubscriptionHandler handler) throws IOException {
        Validators.validateTopic(topic);

        final Long subscriptionId = addCallbackHandler(handler);// TODO is the topic the id?
        synchronized (socketOut) {
            sendByte(OpCode.UNSUBSCRIBE.getOrdinal());
            sendVarint(subscriptionId); // TODO
            sendString(topic);
            sendFlush();
        }
    }

    // TODO is there such a thing?
    private void handleSubscribe() throws IOException {
        try {
            final long id = recvVarint(); // TODO tmpId?
            final byte[] message = recvBinary();

            final SubscriptionHandler handler = callbacks.useCallbackHandler(id);
            handler.handleEvent(message);
        }
        catch (IllegalArgumentException e) {
            System.err.printf("No %s found!%n", PublishCallbackHandler.class.getSimpleName());
        }
    }

    // TODO extract to TunnelProtocol
    @Override public void tunnel(@NotNull final String clusterName, final long timeOutMillis, TunnelCallbackHandlers callbackHandlers) throws IOException {
        Validators.validateClusterName(clusterName);

        final int bufferSize = 0; // TODO
        final long id = addCallbackHandler(callbackHandlers);
        synchronized (socketOut) {
            sendByte(OpCode.TUNNEL_REQUEST.getOrdinal());
            sendVarint(id);
            sendString(clusterName);
            sendVarint(bufferSize); // TODO buf?
            sendVarint(timeOutMillis);
            sendFlush();
        }
    }

    private void handleTunnelRequest() throws IOException {
        try {
            final long id = recvVarint(); // TODO tmpId?
            final long bufferSize = recvVarint();

            final TunnelCallbackHandlers handler = callbacks.useCallbackHandler(id);
            handler.handleTunnelRequest(id, bufferSize);
        }
        catch (IllegalArgumentException e) {
            System.err.printf("No %s found!%n", TunnelCallbackHandlers.class.getSimpleName());
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

    private void handleTunnelReply() throws IOException {
        try {
            final long id = recvVarint();
            final boolean hasTimedOut = recvBoolean();

            final TunnelCallbackHandlers handler = callbacks.useCallbackHandler(id);
            if (hasTimedOut) {
                handler.handleTunnelReply(id, 0, true);
            } else {

                final long bufferSize = recvVarint();
                handler.handleTunnelReply(id, bufferSize, false);
            }
        }
        catch (IllegalArgumentException e) {
            System.err.printf("No %s found!%n", TunnelCallbackHandlers.class.getSimpleName());
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

    private void handleTunnelData() throws IOException {
        try {
            final long id = recvVarint();
            final byte[] message = recvBinary();

            final TunnelCallbackHandlers handler = callbacks.useCallbackHandler(id);
            handler.handleTunnelData(id, message);
        }
        catch (IllegalArgumentException e) {
            System.err.printf("No %s found!%n", TunnelCallbackHandlers.class.getSimpleName());
        }
    }

    private void sendTunnelAck(final long tunnelId) throws IOException {
        synchronized (socketOut) {
            sendByte(OpCode.TUNNEL_ACK.getOrdinal());
            sendVarint(tunnelId);
            sendFlush();
        }
    }

    private void handleTunnelAck() throws IOException {
        try {
            final long id = recvVarint();

            final TunnelCallbackHandlers handler = callbacks.useCallbackHandler(id);
            handler.handleTunnelAck(id);
        }
        catch (IllegalArgumentException e) {
            System.err.printf("No %s found!%n", TunnelCallbackHandlers.class.getSimpleName());
        }
    }

    private void sendTunnelClose(final long tunnelId) throws IOException {
        synchronized (socketOut) {
            sendByte(OpCode.TUNNEL_CLOSE.getOrdinal());
            sendVarint(tunnelId);
            sendFlush();
        }
    }

    private void handleTunnelClose() throws IOException {
        try {
            final long id = recvVarint();

            final TunnelCallbackHandlers handler = callbacks.useCallbackHandler(id);
            handler.handleTunnelClose(id);
        }
        catch (IllegalArgumentException e) {
            System.err.printf("No %s found!%n", TunnelCallbackHandlers.class.getSimpleName());
        }
    }

    private void sendClose() throws IOException {
        synchronized (socketOut) {
            sendByte(OpCode.CLOSE.getOrdinal());
            sendFlush();
        }
    }

    public void addCallbackHandler(@NotNull final StaticCallbackHandler callbackHandler) {
        callbacks.addCallbackHandler(callbackHandler);
    }

    protected Long addCallbackHandler(@NotNull final InstanceCallbackHandler callbackHandler) {
        return callbacks.addCallbackHandler(callbackHandler);
    }

    // TODO precessors; should be segregated

    public void handle() throws Exception {
        // TODO handle all messages
        // Producer/consumer?
        try {
            final OpCode opCode = OpCode.valueOf(recvByte());
            switch (opCode) {
                case BROADCAST:
                    handleBroadcast();
                    break;

                case REQUEST:
                    handleRequest();
                    break;
                case REPLY:
                    handleReply();
                    break;

                case PUBLISH:
                    handlePublish();
                    break;

                case TUNNEL_REQUEST:
                    handleTunnelRequest();
                    break;
                case TUNNEL_REPLY:
                    handleTunnelReply();
                    break;
                case TUNNEL_DATA:
                    handleTunnelData();
                    break;
                case TUNNEL_ACK:
                    handleTunnelAck();
                    break;
                case TUNNEL_CLOSE:
                    handleTunnelClose();
                    break;

                case CLOSE:
                    return;

                default:
                case INIT:
                    throw new IllegalStateException(String.format("Illegal %s received: '%s'!", OpCode.class.getSimpleName(), opCode));
            }
        }
        finally {
            close();
        }
    }

    @Override public void close() throws Exception {
        super.close();
        socket.close();
    }
}