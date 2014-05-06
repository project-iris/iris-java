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

    private final Socket            socket;    // Network connection to the iris node
    private final ConnectionHandler handler;

    public Connection(int port, @NotNull String clusterName, @Nullable ConnectionHandler handler) throws IOException {
        this(new Socket(InetAddress.getLoopbackAddress(), port), handler);

        sendInit(clusterName);
        procInit();
    }

    public Connection(@NotNull final Socket socket, @Nullable ConnectionHandler handler) throws IOException {
        super(socket.getInputStream(), socket.getOutputStream());

        this.socket = socket;
        this.handler = handler;
    }

    private void sendInit(@NotNull final String clusterName) throws IOException {
        sendByte(OpCode.INIT.getOrdinal());
        sendString(VERSION);
        sendString(clusterName);
        sendFlush();
    }

    private void procInit() throws IOException {
        if (recvByte() != OpCode.INIT.getOrdinal()) {
            throw new ProtocolException("Protocol violation");
        }
    }

    @Override public void broadcast(@NotNull final String clusterName, @NotNull final byte[] message) throws IOException {
        if (clusterName.isEmpty()) { throw new IllegalArgumentException("Empty cluster name!"); }
        if (message.length == 0) { throw new IllegalArgumentException("Empty message!"); }

        doBroadcast(clusterName, message);
    }

    private void doBroadcast(@NotNull final String clusterName, @NotNull final byte[] message) throws IOException {
        synchronized (socketOut) {
            sendByte(OpCode.BROADCAST.getOrdinal());
            sendString(clusterName);
            sendBinary(message);
            sendFlush();
        }
    }

    @NotNull @Override public byte[] request(@NotNull final String clusterName, @NotNull final byte[] request, final long timeout) throws IOException {
        if (clusterName.isEmpty()) { throw new IllegalArgumentException("Empty cluster name!"); }
        if (request.length == 0) { throw new IllegalArgumentException("Empty request!"); }

        return doRequest(clusterName, request, timeout);
    }

    private byte[] doRequest(final String clusterName, final byte[] request, final long timeout) throws IOException {
        // FIXME unfinished stub
        final byte[] result = null;
        synchronized (socketOut) {
            sendByte(OpCode.REQUEST.getOrdinal());
            sendString(clusterName);
            sendBinary(request);
            sendFlush();
        }
        return result;
    }

    @Override public void subscribe(@NotNull final String topic, @NotNull final SubscriptionHandler handler) throws IOException {
        if (topic.isEmpty()) { throw new IllegalArgumentException("Empty topic name!"); }

        doSubscribe(topic, handler);
    }

    private void doSubscribe(@NotNull final String topic, final SubscriptionHandler handler) throws IOException {
        // FIXME unfinished stub
        synchronized (socketOut) {
            sendByte(OpCode.SUBSCRIBE.getOrdinal());
            sendString(topic);
            sendFlush();
        }
    }

    @Override public void unsubscribe(@NotNull final String topic) throws IOException {
        if (topic.isEmpty()) { throw new IllegalArgumentException("Empty topic name!"); }

        doUnsubscribe(topic);
    }

    private void doUnsubscribe(@NotNull final String topic) throws IOException {
        // FIXME unfinished stub
        synchronized (socketOut) {
            sendByte(OpCode.UNSUBSCRIBE.getOrdinal());
            sendString(topic);
            sendFlush();
        }
    }

    @Override public void publish(@NotNull final String topic, @NotNull final byte[] message) throws IOException {
        if (topic.isEmpty()) { throw new IllegalArgumentException("Empty topic name!"); }
        if (message.length == 0) { throw new IllegalArgumentException("Empty message!"); }

        doPublish(topic, message);
    }

    private void doPublish(@NotNull final String topic, @NotNull final byte[] message) throws IOException {
        // FIXME unfinished stub
        synchronized (socketOut) {
            sendByte(OpCode.PUBLISH.getOrdinal());
            sendString(topic);
            sendBinary(message);
            sendFlush();
        }
    }

    @Override public Tunnel tunnel(@NotNull final String clusterName, final long timeout) throws IOException {
        if (clusterName.isEmpty()) { throw new IllegalArgumentException("Empty cluster name!"); }

        return doTunnel(clusterName, timeout);
    }

    private Tunnel doTunnel(@NotNull final String clusterName, final long timeout) throws IOException {
        // FIXME unfinished stub
        final Tunnel result = null;
        synchronized (socketOut) {
            sendByte(OpCode.TUNNEL_REQUEST.getOrdinal());
            sendString(clusterName);
            sendFlush();
        }
        return result;
    }

    @Override public void close() throws Exception {
        super.close();
        socket.close();
    }
}