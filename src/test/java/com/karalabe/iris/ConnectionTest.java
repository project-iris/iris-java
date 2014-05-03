package com.karalabe.iris;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

@SuppressWarnings({"resource", "JUnitTestNG", "ProhibitedExceptionDeclared", "UnqualifiedStaticUsage"})
public class ConnectionTest {
    private static final int    IRIS_PORT    = 55555;
    private static final String CLUSTER_NAME = "testClusterName";

    @Test public void connectIsWorking() throws Exception {
        try (final Socket ignored = new Socket(InetAddress.getLoopbackAddress(), IRIS_PORT)) {
        }
        catch (IOException ignored) {
            Assert.fail();
        }
    }

    @Test public void handshakeIsWorking() throws Exception {
        try (final Connection ignored = new Connection(IRIS_PORT, CLUSTER_NAME, null)) {
        }
        catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test public void broadcastIsWorking() throws Exception {
        try (final Connection connection = new Connection(IRIS_PORT, CLUSTER_NAME, null)) {
        }
        catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }
}