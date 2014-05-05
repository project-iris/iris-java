package com.karalabe.iris;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ProtocolException;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/*
 * Message relay between the local app and the local iris node.
 **/
public class Connection implements AutoCloseable {
    private static final int VAR_INT_CONTINUATION_BIT = 0b10000000;
    private static final int VAR_INT_BYTE_MAX_VALUE   = VAR_INT_CONTINUATION_BIT - 1;
    private static final int VAR_INT_BYTE_MASK        = VAR_INT_CONTINUATION_BIT - 1;

    public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    private static final String VERSION = "v1.0";

    private final Socket           socket;    // Network connection to the iris node
    private final DataInputStream  socketIn;  //
    private final DataOutputStream socketOut; //

    public Connection(int port, @NotNull String clusterName, @Nullable ConnectionHandler handler) throws IOException, ProtocolException {
        socket = new Socket(InetAddress.getLoopbackAddress(), port);

        socketIn = new DataInputStream(socket.getInputStream());
        socketOut = new DataOutputStream(socket.getOutputStream());

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

    private void sendByte(final byte data) throws IOException {
        socketOut.writeByte(data);
    }

    private void sendString(@NotNull final String data) throws IOException {
        sendBinary(data.getBytes(DEFAULT_CHARSET));
    }

    private void sendBoolean(final boolean data) throws IOException {
        sendByte((byte) (data ? 1 : 0));
    }

    private void sendBinary(@NotNull final byte[] data) throws IOException {
        sendVarint(data.length);
        socketOut.write(data);
    }

    private void sendVarint(final long data) throws IOException {
        long toSend = data;
        while (toSend > VAR_INT_BYTE_MAX_VALUE) {
            sendByte((byte) (VAR_INT_CONTINUATION_BIT | (toSend & VAR_INT_BYTE_MASK)));
            toSend /= VAR_INT_CONTINUATION_BIT;
        }
        sendByte((byte) toSend);
    }

    private void sendBroadcast(@NotNull final String app, @NotNull final byte[] msg) throws IOException {
        synchronized (socketOut) {
            sendByte(OpCode.BROADCAST.getOrdinal());
            sendString(app);
            sendBinary(msg);
            sendFlush();
        }
    }

    private short recvByte() throws IOException {
        return socketIn.readByte();
    }

    private boolean recvBool() throws IOException {
        final short data = recvByte();
        switch (data) {
            case 0:
                return false;
            case 1:
                return true;
            default:
                throw new ProtocolException("Boolean expected, received: " + data);
        }
    }

    private long recvVarint() throws IOException {
        throw new IllegalStateException("Not implemented!");
    }

    private byte[] recvBinary() throws IOException {
        final byte[] result = new byte[(int) recvVarint()];
        socketIn.readFully(result);
        return result;
    }

    private String recvString() throws IOException {
        return new String(recvBinary(), DEFAULT_CHARSET);
    }

    public void broadcast(@NotNull final String clusterName, @NotNull final byte[] message) throws IOException {
        if (clusterName.isEmpty()) { throw new IllegalArgumentException("Empty cluster name!"); }

        sendBroadcast(clusterName, message);
    }

    private void sendFlush() throws IOException {
        socketOut.flush();
    }

    @Override public void close() throws Exception {
        socketOut.close();
        socketIn.close();
        socket.close();
    }
}