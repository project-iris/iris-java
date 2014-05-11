package com.karalabe.iris;

import com.karalabe.iris.callback.handlers.BroadcastCallbackHandler;
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
        try (final Connection ignored = new Connection(IRIS_PORT, CLUSTER_NAME)) {
        }
        catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test public void broadcastIsWorking() throws Exception {
        testConnection((originalMessage, connection) -> {
            connection.addCallbackHandler((BroadcastCallbackHandler) receivedMessage -> {
                Assert.assertArrayEquals("Wrong message received!", originalMessage, receivedMessage);
            });
            connection.broadcast(CLUSTER_NAME, originalMessage);
        });
    }

    @Test public void requestResponseIsWorking() throws Exception {
        testConnection((originalMessage, connection) -> {
            connection.request(CLUSTER_NAME, originalMessage, 1, (requestId, receivedMessage) -> {
                Assert.assertEquals("Wrong requestId received!", 0L, requestId);
                Assert.assertArrayEquals("Wrong message received!", originalMessage, receivedMessage);
            });
        });
    }

    private static void testConnection(TestConsumer testConsumer) throws Exception {
        final byte[] originalMessage = "testMessage".getBytes(StandardCharsets.UTF_8);

        try (final Connection connection = new Connection(IRIS_PORT, CLUSTER_NAME)) {
            final Semaphore semaphore = new Semaphore(1);
            semaphore.acquire();

            testConsumer.accept(originalMessage, connection, semaphore);

            Assert.assertTrue("ConnectionHandler was never called!", semaphore.tryAcquire(10, TimeUnit.MILLISECONDS));
        }
        catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    @FunctionalInterface
    public interface TestConsumer {
        void accept(final byte[] originalMessage, final Connection connection) throws Exception;

        default void accept(final byte[] originalMessage, final Connection connection, final Semaphore semaphore) throws Exception {
            accept(originalMessage, connection);
            connection.process(); // TODO move this

            semaphore.release();
        }
    }
}