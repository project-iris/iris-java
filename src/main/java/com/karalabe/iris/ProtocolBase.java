package com.karalabe.iris;

import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.ProtocolException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class ProtocolBase implements AutoCloseable {
    private static final short VAR_INT_CHUNK_BIT_SIZE  = 7;
    private static final short VAR_INT_MERGE_BIT       = 1 << VAR_INT_CHUNK_BIT_SIZE;
    private static final short VAR_INT_CHUNK_BYTE_MASK = VAR_INT_MERGE_BIT - 1;

    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    @NotNull protected final DataInputStream  socketIn;
    @NotNull protected final DataOutputStream socketOut;

    public ProtocolBase(@NotNull final InputStream socketIn, @NotNull final OutputStream socketOut) {
        this.socketIn = new DataInputStream(socketIn);
        this.socketOut = new DataOutputStream(socketOut);
    }

    protected void sendByte(final byte data) throws IOException {
        socketOut.writeByte(data);
    }

    protected byte recvByte() throws IOException {
        return socketIn.readByte();
    }

    protected void sendBoolean(final boolean data) throws IOException {
        sendByte((byte) (data ? 1 : 0));
    }

    protected void sendVarint(final long data) throws IOException {
        long toSend = data;
        while (true) {
            final long chunk = (toSend & VAR_INT_CHUNK_BYTE_MASK);
            if (hasNextChunk(toSend)) {
                sendByte((byte) (VAR_INT_MERGE_BIT | chunk));
                toSend >>>= VAR_INT_CHUNK_BIT_SIZE;
            } else {
                sendByte((byte) chunk);
                break;
            }
        }
    }

    private boolean hasNextChunk(final long chunk) {
        return (chunk & ~VAR_INT_CHUNK_BYTE_MASK) != 0;
    }

    protected long recvVarint() throws IOException {
        long result = 0;

        short nextByte;
        byte shiftAmount = 0;
        do {
            nextByte = recvByte();
            final long chunk = (nextByte & VAR_INT_CHUNK_BYTE_MASK);
            result |= (chunk << shiftAmount);

            shiftAmount += VAR_INT_CHUNK_BIT_SIZE;
            if (shiftAmount > Long.SIZE) { throw new IllegalStateException("Invalid data read!"); }
        } while ((nextByte & VAR_INT_MERGE_BIT) != 0);

        return result;
    }

    protected boolean recvBoolean() throws IOException {
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

    protected void sendBinary(@NotNull final byte[] data) throws IOException {
        sendVarint(data.length);
        socketOut.write(data);
    }

    protected byte[] recvBinary() throws IOException {
        final byte[] result = new byte[(int) recvVarint()];
        socketIn.readFully(result);
        return result;
    }

    protected void sendString(@NotNull final String data) throws IOException {
        sendBinary(data.getBytes(DEFAULT_CHARSET));
    }

    protected String recvString() throws IOException {
        return new String(recvBinary(), DEFAULT_CHARSET);
    }

    protected void sendFlush() throws IOException {
        socketOut.flush();
    }

    @Override public void close() throws Exception {
        socketOut.close();
        socketIn.close();
    }
}