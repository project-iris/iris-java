/*
 * Copyright © 2014 Project Iris. All rights reserved.
 *
 * The current language binding is an official support library of the Iris cloud messaging framework, and as such, the same licensing terms apply.
 * For details please see http://iris.karalabe.com/downloads#License
 */

package com.karalabe.iris.protocol;

import com.karalabe.iris.ProtocolException;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class ProtocolBase implements AutoCloseable {
    private static final short VAR_INT_CHUNK_BIT_SIZE  = 7;
    private static final short VAR_INT_MERGE_BIT       = 1 << VAR_INT_CHUNK_BIT_SIZE;
    private static final short VAR_INT_CHUNK_BYTE_MASK = VAR_INT_MERGE_BIT - 1;

    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    @NotNull private final   Socket           socket;
    @NotNull protected final DataInputStream  socketIn;
    @NotNull protected final DataOutputStream socketOut;

    public ProtocolBase(final int port) {
        try {
            socket = new Socket(InetAddress.getLoopbackAddress(), port);
            socketIn = new DataInputStream(socket.getInputStream()); // TODO non-buffered input?
            socketOut = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        } catch (IOException e) { throw new ProtocolException(e); }
    }

    public void send(OpCode opCode, Runnable runnable) {
        synchronized (socketOut) {
            sendByte(opCode.getOrdinal());
            runnable.run();
            flush();
        }
    }

    public void flush() {
        try {
            socketOut.flush();
        } catch (IOException e) { throw new ProtocolException(e); }
    }

    public void sendByte(final byte data) {
        try {
            socketOut.writeByte(data);
        } catch (IOException e) { throw new ProtocolException(e); }
    }

    public byte receiveByte() {
        try {
            return socketIn.readByte();
        } catch (IOException e) { throw new ProtocolException(e); }
    }

    public void sendBoolean(final boolean data) {
        sendByte((byte) (data ? 1 : 0));
    }

    @SuppressWarnings("BooleanMethodNameMustStartWithQuestion")
    public boolean receiveBoolean() {
        final byte data = receiveByte();
        switch (data) {
            case 0:
                return false;
            case 1:
                return true;
            default:
                throw new ProtocolException("Boolean expected, received: " + data);
        }
    }

    private static boolean hasNextChunk(final long chunk) {
        return (chunk & ~VAR_INT_CHUNK_BYTE_MASK) != 0;
    }

    public void sendVarint(final long data) {
        long toSend = data;
        while (true) {
            final long chunk = (toSend & VAR_INT_CHUNK_BYTE_MASK);
            if (ProtocolBase.hasNextChunk(toSend)) {
                sendByte((byte) (VAR_INT_MERGE_BIT | chunk));
                toSend >>>= VAR_INT_CHUNK_BIT_SIZE;
            } else {
                sendByte((byte) chunk);
                break;
            }
        }
    }

    public long receiveVarint() {
        long result = 0;

        short nextByte;
        byte shiftAmount = 0;
        do {
            nextByte = receiveByte();
            final long chunk = (nextByte & VAR_INT_CHUNK_BYTE_MASK);
            result |= (chunk << shiftAmount);

            shiftAmount += VAR_INT_CHUNK_BIT_SIZE;
            if (shiftAmount > Long.SIZE) { throw new ProtocolException("Invalid data read!"); }
        } while ((nextByte & VAR_INT_MERGE_BIT) != 0);

        return result;
    }

    public void sendBinary(@NotNull final byte... data) {
        try {
            sendVarint(data.length);
            socketOut.write(data);
        } catch (IOException e) { throw new ProtocolException(e); }
    }

    public byte[] receiveBinary() {
        try {
            final byte[] result = new byte[(int) receiveVarint()];
            socketIn.readFully(result);
            return result;
        } catch (IOException e) { throw new ProtocolException(e); }
    }

    public void sendString(@NotNull final String data) {
        sendBinary(data.getBytes(DEFAULT_CHARSET));
    }

    public String receiveString() {
        return new String(receiveBinary(), DEFAULT_CHARSET);
    }

    @Override public void close() {
        try {
            socketOut.close();
            socketIn.close();
            socket.close();
        } catch (IOException e) { throw new ProtocolException(e); }
    }
}