package com.karalabe.iris;

import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

public class BroadcastTest {
    private class BroadcastTestHandler implements ServiceHandler {
        public Connection conn;
        public Set<String> arrived = Collections.synchronizedSet(new HashSet<>());

        @Override public void init(final Connection conn) {
            this.conn = conn;
        }

        @Override public void handleBroadcast(final byte[] message) {
            arrived.add(new String(message, StandardCharsets.UTF_8));
        }
    }

    @Test public void concurrentBroadcasts() throws Exception {
        final int CLIENTS = 25, SERVERS = 25, MESSAGES = 25;

        final CyclicBarrier barrier = new CyclicBarrier(CLIENTS + SERVERS + 1);
        final List<Exception> errors = Collections.synchronizedList(new ArrayList<>());

        // Start up the concurrent broadcasting clients
        for (int i = 0; i < CLIENTS; i++) {
            final int client = i;

            new Thread(() -> {
                try (final Connection conn = new Connection(Config.RELAY_PORT)) {
                    // Wait till all clients and servers connect
                    barrier.await(Config.PHASE_TIMEOUT, TimeUnit.SECONDS);

                    // Send all the client broadcasts
                    for (int j = 0; j < MESSAGES; j++) {
                        final String message = String.format("client #%d, broadcast %d", client, j);
                        final byte[] messageBlob = message.getBytes(StandardCharsets.UTF_8);

                        conn.broadcast(Config.CLUSTER_NAME, messageBlob);
                    }
                    barrier.await(Config.PHASE_TIMEOUT, TimeUnit.SECONDS);
                    barrier.await(Config.PHASE_TIMEOUT, TimeUnit.SECONDS);
                }
                catch (Exception e) {
                    errors.add(e);
                }
            }).start();
        }
        // Start up the concurrent broadcast services
        for (int i = 0; i < SERVERS; i++) {
            final int server = i;

            new Thread(() -> {
                BroadcastTestHandler handler = new BroadcastTestHandler();

                try (final Service serv = new Service(Config.RELAY_PORT, Config.CLUSTER_NAME, handler, null)) {
                    // Wait till all clients and servers connect
                    barrier.await(Config.PHASE_TIMEOUT, TimeUnit.SECONDS);

                    // Send all the service broadcasts
                    for (int j = 0; j < MESSAGES; j++) {
                        final String message = String.format("server #%d, broadcast %d", server, j);
                        final byte[] messageBlob = message.getBytes(StandardCharsets.UTF_8);

                        handler.conn.broadcast(Config.CLUSTER_NAME, messageBlob);
                    }
                    barrier.await(Config.PHASE_TIMEOUT, TimeUnit.SECONDS);

                    // Wait a while for all broadcasts to arrive (replace with some channel eventually)
                    Thread.sleep(1000);

                    for (int j = 0; j < CLIENTS; j++) {
                        for (int k = 0; k < MESSAGES; k++) {
                            final String message = String.format("client #%d, broadcast %d", j, k);
                            Assert.assertTrue(handler.arrived.contains(message));
                        }
                    }
                    for (int j = 0; j < SERVERS; j++) {
                        for (int k = 0; k < MESSAGES; k++) {
                            final String message = String.format("server #%d, broadcast %d", j, k);
                            Assert.assertTrue(handler.arrived.contains(message));
                        }
                    }
                    barrier.await(Config.PHASE_TIMEOUT, TimeUnit.SECONDS);
                }
                catch (Exception e) {
                    errors.add(e);
                }
            }).start();
        }
        // Schedule the parallel operations
        barrier.await(Config.PHASE_TIMEOUT, TimeUnit.SECONDS);
        Assert.assertTrue(errors.isEmpty());
        barrier.await(Config.PHASE_TIMEOUT, TimeUnit.SECONDS);
        Assert.assertTrue(errors.isEmpty());
        barrier.await(Config.PHASE_TIMEOUT, TimeUnit.SECONDS);
        Assert.assertTrue(errors.isEmpty());
    }
}