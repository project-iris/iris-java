package com.karalabe.iris;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.List;

/*
* Message relay between the local app and the local iris node.
**/
public class Relay {
    // Network layer fields
    private Socket socket; // Network connection to the iris node

    public Relay(int port, String cluster) throws IOException {
        socket = new Socket(InetAddress.getLoopbackAddress(), port);
    }
}