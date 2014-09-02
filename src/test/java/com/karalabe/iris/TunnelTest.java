// Copyright (c) 2014 Project Iris. All rights reserved.
//
// The current language binding is an official support library of the Iris
// cloud messaging framework, and as such, the same licensing terms apply.
// For details please see http://iris.karalabe.com/downloads#License
package com.karalabe.iris;

import com.carrotsearch.junitbenchmarks.AbstractBenchmark;
import com.carrotsearch.junitbenchmarks.BenchmarkOptions;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("JUnitTestNG")
public class TunnelTest extends AbstractBenchmark {
    // Service handler for the tunnel tests.
    static class TunnelTestHandler implements ServiceHandler {
        Connection connection;

        @Override public void init(@NotNull final Connection connection) {
            this.connection = connection;
        }

        @Override public void handleTunnel(@NotNull final Tunnel tunnel) {
            try {
                tunnel.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Tests multiple concurrent client and service tunnels.
    @BenchmarkOptions(benchmarkRounds = 1, warmupRounds = 0)
    @Test public void concurrentTunnels() throws Exception {
        final int CLIENT_COUNT = 7, SERVER_COUNT = 7, TUNNEL_COUNT = 7, EXCHANGE_COUNT = 7;

        final Collection<Thread> workers = new ArrayList<>(CLIENT_COUNT + SERVER_COUNT);
        final CyclicBarrier barrier = new CyclicBarrier(CLIENT_COUNT + SERVER_COUNT + 1);
        final List<Exception> errors = Collections.synchronizedList(new ArrayList<>());

        // Start up the concurrent tunneling clients
        for (int i = 0; i < CLIENT_COUNT; i++) {
            final int client = i;
            final Thread worker = new Thread(() -> {
                try (final Connection conn = Iris.connect(Config.RELAY_PORT)) {
                    // Wait till all clients and servers connect
                    barrier.await(Config.PHASE_TIMEOUT, TimeUnit.SECONDS);

                    // Execute the tunnel construction, message exchange and verification
                    final String id = String.format("client #%d", client);
                    buildExchangeVerify(id, conn, TUNNEL_COUNT, EXCHANGE_COUNT);
                    barrier.await(Config.PHASE_TIMEOUT, TimeUnit.SECONDS);
                } catch (Exception e) {
                    errors.add(e);
                }
            });
            worker.start();
            workers.add(worker);
        }

        // Start up the concurrent tunneling services
        for (int i = 0; i < SERVER_COUNT; i++) {
            final int server = i;
            final Thread worker = new Thread(() -> {
                final TunnelTestHandler handler = new TunnelTestHandler();

                try (final Service ignored = Iris.register(Config.RELAY_PORT, Config.CLUSTER_NAME, handler)) {
                    // Wait till all clients and servers connect
                    barrier.await(Config.PHASE_TIMEOUT, TimeUnit.SECONDS);

                    // Execute the tunnel construction, message exchange and verification
                    final String id = String.format("client #%d", server);
                    buildExchangeVerify(id, handler.connection, TUNNEL_COUNT, EXCHANGE_COUNT);
                    barrier.await(Config.PHASE_TIMEOUT, TimeUnit.SECONDS);
                } catch (Exception e) {
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
        } finally {
            for (Thread worker : workers) {
                worker.join();
            }
        }
    }

    // Opens a batch of concurrent tunnels, and executes a data exchange.
    private static void buildExchangeVerify(String id, Connection conn, int tunnels, int exchanges) throws Exception {
        final Collection<Thread> workers = new ArrayList<>(tunnels);
        final CyclicBarrier barrier = new CyclicBarrier(tunnels + 1);
        final List<Exception> errors = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < tunnels; i++) {
            int tunnelId = i;
            final Thread worker = new Thread(() -> {
                // Open a tunnel to the service cluster
                try (final Tunnel tunnel = conn.tunnel(Config.CLUSTER_NAME, 1000)) {
                    // Serialize a batch of messages
                    for (int j = 0; j < exchanges; j++) {
                        final String message = String.format("%s, tunnel #%d, message #%d", id, tunnelId, j);
                        final byte[] messageBlob = message.getBytes(StandardCharsets.UTF_8);

                        tunnel.send(messageBlob, 1000);
                    }
                    // Read back the echo stream and verify
                    for (int j = 0; j < exchanges; j++) {
                        final byte[] messageBlob = tunnel.receive(1000);
                        final String message = new String(messageBlob, StandardCharsets.UTF_8);

                        final String original = String.format("%s, tunnel #%d, message #%d", id, tunnelId, j);
                        Assert.assertEquals(original, message);
                    }
                    // Wait till all tunnels complete the transfers
                    barrier.await(Config.PHASE_TIMEOUT, TimeUnit.SECONDS);
                } catch (Exception e) {
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
        } finally {
            for (Thread worker : workers) {
                worker.join();
            }
        }
    }
}