package com.karalabe.iris;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ProtocolException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/*
 * Message relay between the local app and the local iris node.
 **/
public class Connection implements AutoCloseable {
    private static final int VAR_INT_CONTINUATION_BIT = 0b10000000;
    private static final int VAR_INT_BYTE_MAX_VALUE   = VAR_INT_CONTINUATION_BIT - 1;
    private static final int VAR_INT_BYTE_MASK        = VAR_INT_CONTINUATION_BIT - 1;

    private static final String VERSION = "v1.0";

    private final Socket          socket;    // Network connection to the iris node
    private final DataInputStream socketIn;  //
    private final OutputStream    socketOut; //

    public Connection(int port, String clusterName, ConnectionHandler handler) throws IOException, ProtocolException {
        socket = new Socket(InetAddress.getLoopbackAddress(), port);

        socketIn = new DataInputStream(socket.getInputStream());
        socketOut = socket.getOutputStream();

        sendInit(clusterName);
        procInit();
    }

    private void sendInit(final String app) throws IOException {
        this.sendByte(OpCode.INIT.getOrdinal());
        this.sendString(VERSION);
        this.sendString(app);
        this.sendFlush();
    }

    private void procInit() throws IOException {
        if (recvByte() != OpCode.INIT.getOrdinal()) {
            throw new ProtocolException("Protocol version mismatch");
        }
    }

    private void sendByte(final byte data) throws IOException {
        socketOut.write(new byte[]{data});
    }

    private void sendString(final String data) throws IOException {
        this.sendBinary(data.getBytes(StandardCharsets.UTF_8));
    }

    private void sendBoolean(final boolean data) throws IOException {
        this.sendByte((byte) (data ? 1 : 0));
    }

    private void sendBroadcast(final String app, final byte[] msg) throws IOException {
        synchronized (socketOut) {
            this.sendByte(OpCode.BROADCAST.getOrdinal());
            this.sendString(app);
            this.sendBinary(msg);
            this.sendFlush();
        }
    }

    private void sendBinary(final byte[] data) throws IOException {
        this.sendVarint(data.length);
        socketOut.write(data);
    }

    private void sendVarint(final long data) throws IOException {
        long toSend = data;
        while (toSend > VAR_INT_BYTE_MAX_VALUE) {
            this.sendByte((byte) (VAR_INT_CONTINUATION_BIT | (toSend & VAR_INT_BYTE_MASK)));
            toSend /= VAR_INT_CONTINUATION_BIT;
        }
        this.sendByte((byte) toSend);
    }

    private byte recvByte() throws IOException {
        return socketIn.readByte();
    }

    private boolean recvBool() throws IOException {
        final byte data = recvByte();
        switch (data) {
            case 0:
                return false;
            case 1:
                return true;
            default:
                throw new ProtocolException("Boolean expected, received: " + data);
        }
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