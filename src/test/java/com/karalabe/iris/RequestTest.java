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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

public class RequestTest extends AbstractBenchmark {
    // Service handler for the request/reply tests.
    private class RequestTestHandler implements ServiceHandler {
        public Connection conn;

        @Override public void init(final Connection conn) {
            this.conn = conn;
        }

        @Override public byte[] handleRequest(final byte[] request) throws RuntimeException {
            return request;
        }
    }

    // Tests multiple concurrent client and service requests.
    @BenchmarkOptions(benchmarkRounds = 5, warmupRounds = 10)
    @Test public void concurrentRequests() throws Exception {
        final int CLIENTS = 25, SERVERS = 25, REQUESTS = 25;

        final CyclicBarrier barrier = new CyclicBarrier(CLIENTS + SERVERS + 1);
        final List<Exception> errors = Collections.synchronizedList(new ArrayList<>());

        // Start up the concurrent requesting clients
        for (int i = 0; i < CLIENTS; i++) {
            final int client = i;

            new Thread(() -> {
                try (final Connection conn = new Connection(Config.RELAY_PORT)) {
                    // Wait till all clients and servers connect
                    barrier.await(Config.PHASE_TIMEOUT, TimeUnit.SECONDS);

                    // Request from the service cluster
                    for (int j = 0; j < REQUESTS; j++) {
                        final String request = String.format("client #%d, broadcast %d", client, j);
                        final byte[] requestBlob = request.getBytes(StandardCharsets.UTF_8);

                        final byte[] replyBlob = conn.request(Config.CLUSTER_NAME, requestBlob, 1000);
                        final String reply = new String(replyBlob, StandardCharsets.UTF_8);

                        Assert.assertEquals(reply, request);
                    }
                    barrier.await(Config.PHASE_TIMEOUT, TimeUnit.SECONDS);
                }
                catch (Exception e) {
                    errors.add(e);
                }
            }).start();
        }
        // Start up the concurrent requesting services
        for (int i = 0; i < SERVERS; i++) {
            final int server = i;

            new Thread(() -> {
                RequestTestHandler handler = new RequestTestHandler();

                try (final Service serv = new Service(Config.RELAY_PORT, Config.CLUSTER_NAME, handler)) {
                    // Wait till all clients and servers connect
                    barrier.await(Config.PHASE_TIMEOUT, TimeUnit.SECONDS);

                    // Request from the service cluster
                    for (int j = 0; j < REQUESTS; j++) {
                        final String request = String.format("server #%d, broadcast %d", server, j);
                        final byte[] requestBlob = request.getBytes(StandardCharsets.UTF_8);

                        final byte[] replyBlob = handler.conn.request(Config.CLUSTER_NAME, requestBlob, 1000);
                        final String reply = new String(replyBlob, StandardCharsets.UTF_8);
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
    }
}
