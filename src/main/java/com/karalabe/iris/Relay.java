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
    private Socket       socket;    // Network connection to the iris node
    private InputStream  socketIn;  //
    private OutputStream socketOut; //

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
    	while (toSend > 127) {
    		this.sendByte((byte)(128 + toSend % 128));
    		toSend /= 128;
    	}
    	this.sendByte((byte)toSend);
    }
}