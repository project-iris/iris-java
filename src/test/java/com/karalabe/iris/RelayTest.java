package com.karalabe.iris;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.ServerSocket;

public class RelayTest {

    @Test public void testSimpleCases() throws Exception {
        try {
            final ServerSocket socket = new ServerSocket(55555);
        }
        catch (IOException e) {
            Assert.fail();
        }
    }
}