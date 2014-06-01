package com.karalabe.iris;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@SuppressWarnings({"resource", "JUnitTestNG", "ProhibitedExceptionDeclared", "UnqualifiedStaticUsage"})
public class HandshakeTest {
    private static final byte[] MESSAGE_BYTES  = "testMessage".getBytes(StandardCharsets.UTF_8);
    private static final int    TIMEOUT_MILLIS = 10;

    @Test public void connection() throws Exception {
        try (final Connection ignored = new Connection(Config.RELAY_PORT)) {
        }
        catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test public void service() throws Exception {
        try (final Service ignored = new Service(Config.RELAY_PORT, Config.CLUSTER_NAME, new ServiceHandler() {})) {
        }
        catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }
/*
    @Test public void broadcastIsWorking() throws Exception {
        testConnection(connection -> {
            connection.addCallbackHandler((BroadcastCallbackHandler) receivedMessage -> Assert.assertArrayEquals("Wrong message received!", MESSAGE_BYTES, receivedMessage));
            connection.broadcast(CLUSTER_NAME, MESSAGE_BYTES);
        });
    }

    @Test public void requestResponseIsWorking() throws Exception {
        testConnection(connection -> connection.request(CLUSTER_NAME, MESSAGE_BYTES, TIMEOUT_MILLIS, (requestId, receivedMessage) -> {
            Assert.assertEquals("Wrong requestId received!", 0L, requestId);
            Assert.assertArrayEquals("Wrong message received!", MESSAGE_BYTES, receivedMessage);
        }));
    }

    private static void testConnection(TestConsumer testConsumer) throws Exception {

        try (final Connection connection = new Connection(IRIS_PORT)) {
            final Semaphore semaphore = new Semaphore(1);
            semaphore.acquire();

            testConsumer.accept(connection, semaphore);

            Assert.assertTrue("ConnectionHandler was never called!", semaphore.tryAcquire(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS));
        }
        catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    @FunctionalInterface
    public interface TestConsumer {
        void accept(final Connection connection) throws Exception;

        default void accept(final Connection connection, final Semaphore semaphore) throws Exception {
            accept(connection);
            connection.handle(); // TODO move this

            semaphore.release();
        }
    }*/
}