package com.karalabe.iris;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

public class RelayTest {
    @Test public void isConnectable() throws Exception {
        try (final Socket ignored = new Socket(InetAddress.getLoopbackAddress(), 55555)) {
        }
        catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }
}