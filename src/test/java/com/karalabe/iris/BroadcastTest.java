// Copyright (c) 2014 Project Iris. All rights reserved.
//
// The current language binding is an official support library of the Iris
// cloud messaging framework, and as such, the same licensing terms apply.
// For details please see http://iris.karalabe.com/downloads#License
package com.karalabe.iris;

import com.carrotsearch.junitbenchmarks.AbstractBenchmark;
import com.carrotsearch.junitbenchmarks.BenchmarkOptions;
import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class BroadcastTest extends AbstractBenchmark {
    // Service handler for the broadcast tests.
    private class BroadcastTestHandler implements ServiceHandler {
        public Connection conn;
        public Set<String> arrived = Collections.synchronizedSet(new HashSet<>());
        public Semaphore pending;

        @Override public void init(final Connection conn) {
            this.conn = conn;
        }

        @Override public void handleBroadcast(final byte[] message) {
            arrived.add(new String(message, StandardCharsets.UTF_8));
            pending.release(1);
        }
    }

    // Tests multiple concurrent client and service broadcasts.
    @BenchmarkOptions(benchmarkRounds = 5, warmupRounds = 10)
    @Test public void concurrentBroadcasts() throws Exception {
        final int CLIENTS = 25, SERVERS = 25, MESSAGES = 25;

        final List<Thread> workers = new ArrayList<>(0);
        final CyclicBarrier barrier = new CyclicBarrier(CLIENTS + SERVERS + 1);
        final List<Exception> errors = Collections.synchronizedList(new ArrayList<>());

        // Start up the concurrent broadcasting clients
        for (int i = 0; i < CLIENTS; i++) {
            final int client = i;
            final Thread worker = new Thread(() -> {
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
            });
            worker.start();
            workers.add(worker);
        }
        // Start up the concurrent broadcast services
        for (int i = 0; i < SERVERS; i++) {
            final int server = i;
            final Thread worker = new Thread(() -> {
                BroadcastTestHandler handler = new BroadcastTestHandler();

                try (final Service serv = new Service(Config.RELAY_PORT, Config.CLUSTER_NAME, handler)) {
                    // Wait till all clients and servers connect
                    handler.pending = new Semaphore((CLIENTS + SERVERS) * MESSAGES);
                    handler.pending.acquire((CLIENTS + SERVERS) * MESSAGES);

                    barrier.await(Config.PHASE_TIMEOUT, TimeUnit.SECONDS);

                    // Send all the service broadcasts
                    for (int j = 0; j < MESSAGES; j++) {
                        final String message = String.format("server #%d, broadcast %d", server, j);
                        final byte[] messageBlob = message.getBytes(StandardCharsets.UTF_8);

                        handler.conn.broadcast(Config.CLUSTER_NAME, messageBlob);
                    }
                    barrier.await(Config.PHASE_TIMEOUT, TimeUnit.SECONDS);

                    // Wait for all broadcasts to arrive
                    handler.pending.acquire((CLIENTS + SERVERS) * MESSAGES);

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
            });
            worker.start();
            workers.add(worker);
        }
        // Schedule the parallel operations
        try {
            barrier.await(Config.PHASE_TIMEOUT, TimeUnit.SECONDS);
            Assert.assertTrue(errors.isEmpty());
            barrier.await(Config.PHASE_TIMEOUT, TimeUnit.SECONDS);
            Assert.assertTrue(errors.isEmpty());
            barrier.await(Config.PHASE_TIMEOUT, TimeUnit.SECONDS);
            Assert.assertTrue(errors.isEmpty());
        }
        finally {
            for (Thread worker : workers) {
                worker.join();
            }
        }
    }
}
