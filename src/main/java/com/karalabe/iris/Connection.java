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
public class Connection extends ProtocolBase {
    private static final String VERSION = "v1.0";

    private final Socket            socket;    // Network connection to the iris node
    private final ConnectionHandler handler;

    public Connection(@NotNull final Socket socket, @Nullable ConnectionHandler handler) throws IOException, ProtocolException {
        super(socket.getInputStream(), socket.getOutputStream());

        this.socket = socket;
        this.handler = handler;
    }

    public Connection(int port, @NotNull String clusterName, @Nullable ConnectionHandler handler) throws IOException, ProtocolException {
        this(new Socket(InetAddress.getLoopbackAddress(), port), handler);

        sendInit(clusterName);
        procInit();
    }

    private void sendInit(@NotNull final String app) throws IOException {
        sendByte(OpCode.INIT.getOrdinal());
        sendString(VERSION);
        sendString(app);
        sendFlush();
    }

    private void procInit() throws IOException {
        if (recvByte() != OpCode.INIT.getOrdinal()) {
            throw new ProtocolException("Protocol violation");
        }
    }

    private void sendBroadcast(@NotNull final String app, @NotNull final byte[] msg) throws IOException {
        synchronized (socketOut) {
            sendByte(OpCode.BROADCAST.getOrdinal());
            sendString(app);
            sendBinary(msg);
            sendFlush();
        }
    }

    public void broadcast(@NotNull final String clusterName, @NotNull final byte[] message) throws IOException {
        if (clusterName.isEmpty()) { throw new IllegalArgumentException("Empty cluster name!"); }

        sendBroadcast(clusterName, message);
    }

    @Override public void close() throws Exception {
        super.close();
        socket.close();
    }
}