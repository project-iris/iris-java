package com.karalabe.iris;

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

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
        final byte[] originalMessage = "testMessage".getBytes(StandardCharsets.UTF_8);

        final Semaphore semaphore = new Semaphore(1);

        final ConnectionHandler handler = new ConnectionHandler() {
            @Override public void handleBroadcast(@NotNull final byte[] message) {
                Assert.assertArrayEquals("Wrong message received!", originalMessage, message);
                semaphore.release();
            }
        };

        try (final Connection connection = new Connection(IRIS_PORT, CLUSTER_NAME, handler)) {
            semaphore.acquire();

            connection.broadcast(CLUSTER_NAME, originalMessage);
            connection.process(); // TODO move this

            Assert.assertTrue("ConnectionHandler was never called!", semaphore.tryAcquire(10, TimeUnit.MILLISECONDS));
        }
        catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }
}