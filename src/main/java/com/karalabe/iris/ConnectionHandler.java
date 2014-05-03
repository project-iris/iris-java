package com.karalabe.iris;

public interface ConnectionHandler {
    /* Handles a message broadcasted to all applications of the local type.*/
    void HandleBroadcast(byte[] message);

    /* Handles a request (msg), returning the reply that should be forwarded back
       to the caller. If the method crashes, nothing is returned and the caller
       will eventually time out.*/
    byte[] HandleRequest(byte[] request);

    /* Handles the request to open a direct tunnel.*/
    void HandleTunnel(Tunnel tunnel);

    /* Handles the unexpected termination of the relay connection.*/
    void HandleDrop(Exception reason);
}