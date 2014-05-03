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
        throw new ProtocolException("Not implemented");
    }

    @Override public void close() throws Exception {
        socket.close();
    }
}