package com.karalabe.iris;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ProtocolException;
import java.net.Socket;

/*
* Message relay between the local app and the local iris node.
**/
public class Relay implements AutoCloseable {
    private final Socket       socket;    // Network connection to the iris node
    private final InputStream  socketIn;  //
    private final OutputStream socketOut; //

    public static final int XXX  = 127; // FIXME rename and change to binary
    public static final int XXX2 = 128; // FIXME rename

    public Relay(int port, String clusterName) throws IOException, ProtocolException {
        socket = new Socket(InetAddress.getLoopbackAddress(), port);

        socketIn = socket.getInputStream();
        socketOut = socket.getOutputStream();

        sendInit();
        procInit();
    }

    @Override public void close() throws Exception {
        socketOut.close();
        socketIn.close();
        socket.close();
    }

    private void sendInit() throws IOException {
        throw new IOException("Not implemented");
    }

    private void procInit() throws IOException {
        throw new IOException("Not implemented");
    }
    
    private void sendByte(byte data) throws IOException {
    	socketOut.write(new byte[] {data});
    }
    
    private void sendBool(boolean data) throws IOException {
    	this.sendByte(data ? (byte)1 : (byte)0);
    }
    
    private void sendVarint(long data) throws IOException {
    	long toSend = data;
        while (toSend > XXX) {
            this.sendByte((byte) (XXX2 + (toSend % XXX2)));
            toSend /= XXX2;
        }
    	this.sendByte((byte)toSend);
    }
}