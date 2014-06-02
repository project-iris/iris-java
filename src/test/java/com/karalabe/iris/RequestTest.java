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
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

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

        final List<Thread> workers = new ArrayList<>(0);
        final CyclicBarrier barrier = new CyclicBarrier(CLIENTS + SERVERS + 1);
        final List<Exception> errors = Collections.synchronizedList(new ArrayList<>());

        // Start up the concurrent requesting clients
        for (int i = 0; i < CLIENTS; i++) {
            final int client = i;
            final Thread worker = new Thread(() -> {
                try (final Connection conn = new Connection(Config.RELAY_PORT)) {
                    // Wait till all clients and servers connect
                    barrier.await(Config.PHASE_TIMEOUT, TimeUnit.SECONDS);

                    // Request from the service cluster
                    for (int j = 0; j < REQUESTS; j++) {
                        final String request = String.format("client #%d, request %d", client, j);
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
            });
            worker.start();
            workers.add(worker);
        }
        // Start up the concurrent requesting services
        for (int i = 0; i < SERVERS; i++) {
            final int server = i;
            final Thread worker = new Thread(() -> {
                RequestTestHandler handler = new RequestTestHandler();

                try (final Service ignored = new Service(Config.RELAY_PORT, Config.CLUSTER_NAME, handler)) {
                    // Wait till all clients and servers connect
                    barrier.await(Config.PHASE_TIMEOUT, TimeUnit.SECONDS);

                    // Request from the service cluster
                    for (int j = 0; j < REQUESTS; j++) {
                        final String request = String.format("server #%d, request %d", server, j);
                        final byte[] requestBlob = request.getBytes(StandardCharsets.UTF_8);

                        final byte[] replyBlob = handler.conn.request(Config.CLUSTER_NAME, requestBlob, 1000);
                        final String reply = new String(replyBlob, StandardCharsets.UTF_8);
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
        }
        finally {
            for (Thread worker : workers) {
                worker.join();
            }
        }
    }

    // Service handler for the thread limited request tests.
    private class RequestTestTimedHandler implements ServiceHandler {
        public Connection conn;
        public int        sleep;

        @Override public void init(final Connection conn) {
            this.conn = conn;
        }

        @Override public byte[] handleRequest(final byte[] request) {
            try {
                Thread.sleep(sleep);
                return request;
            }
            catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    // Tests the request thread limitation.
    @BenchmarkOptions(benchmarkRounds = 5, warmupRounds = 10)
    @Test public void timeout() throws Exception {
        final int SLEEP = 100;

        // Create the service handler
        RequestTestTimedHandler handler = new RequestTestTimedHandler();
        handler.sleep = SLEEP;

        try (final Service ignored = new Service(Config.RELAY_PORT, Config.CLUSTER_NAME, handler)) {
            // Check that the timeouts are complied with.
            try {
                handler.conn.request(Config.CLUSTER_NAME, new byte[]{0x00}, SLEEP * 2);
            } catch (Exception e) {
                Assert.fail();
            }
            try {
                handler.conn.request(Config.CLUSTER_NAME, new byte[]{0x00}, SLEEP / 2);
                Assert.fail();
            } catch (TimeoutException e) {
            } catch (Exception e) {
                Assert.fail();
            }
        }
    }

    // Tests the request thread limitation.
    @BenchmarkOptions(benchmarkRounds = 5, warmupRounds = 10)
    @Test public void threadLimiting() throws Exception {
        final int REQUESTS = 4, SLEEP = 100;

        // Create the service handler and limiter
        RequestTestTimedHandler handler = new RequestTestTimedHandler();
        handler.sleep = SLEEP;

        ServiceLimits limits = new ServiceLimits();
        limits.requestThreads = 1;

        try (final Service ignored = new Service(Config.RELAY_PORT, Config.CLUSTER_NAME, handler, limits)) {
            // Start a batch of requesters
            AtomicInteger done = new AtomicInteger(0);
            for (int j = 0; j < REQUESTS; j++) {
                new Thread(() -> {
                    try {
                        handler.conn.request(Config.CLUSTER_NAME, new byte[]{0x00}, 1000);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                    finally {
                        done.addAndGet(1);
                    }
                }).start();
            }
            // Wait for half time and verify that only half was processed
            Thread.sleep(REQUESTS / 2 * SLEEP + SLEEP / 2);
            Assert.assertEquals(REQUESTS / 2, done.get());

            // Wait for the rest to complete
            Thread.sleep(REQUESTS / 2 * SLEEP + SLEEP / 2);
            Assert.assertEquals(REQUESTS, done.get());
        }
    }

    // Tests the request memory limitation.
    @BenchmarkOptions(benchmarkRounds = 5, warmupRounds = 10)
    @Test public void memoryLimiting() throws Exception {
        // Create the service handler and limiter
        RequestTestHandler handler = new RequestTestHandler();
        ServiceLimits limits = new ServiceLimits();
        limits.requestMemory = 1;

        try (final Service ignored = new Service(Config.RELAY_PORT, Config.CLUSTER_NAME, handler, limits)) {
            // Check that a 1 byte request succeeds
            try {
                handler.conn.request(Config.CLUSTER_NAME, new byte[]{0x00}, 1000);
            }
            catch (Exception e) {
                Assert.fail();
            }
            // Check that a 2 byte request fails
            try {
                handler.conn.request(Config.CLUSTER_NAME, new byte[]{0x00, 0x00}, 1000);
                Assert.fail();
            }
            catch (Exception e) { }
        }
    }
}
