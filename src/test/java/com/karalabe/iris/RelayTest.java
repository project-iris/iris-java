package com.karalabe.iris;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

public class RelayTest {

    @Test public void testSimpleCases() throws Exception {
        try (final Socket socket = new Socket(InetAddress.getLocalHost(), 55555)) {
            socket.isClosed();
        }
        catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }
}