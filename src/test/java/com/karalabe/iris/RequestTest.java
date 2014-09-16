// Copyright (c) 2014 Project Iris. All rights reserved.
//
// The current language binding is an official support library of the Iris
// cloud messaging framework, and as such, the same licensing terms apply.
// For details please see http://iris.karalabe.com/downloads#License
package com.karalabe.iris;

import com.carrotsearch.junitbenchmarks.AbstractBenchmark;
import com.carrotsearch.junitbenchmarks.BenchmarkOptions;
import com.karalabe.iris.exceptions.RemoteException;
import com.karalabe.iris.exceptions.TimeoutException;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

@SuppressWarnings("JUnitTestNG")
public class RequestTest extends AbstractBenchmark {
    // Service handler for the request/reply tests.
    static class RequestTestHandler implements ServiceHandler {
        Connection connection;

        @Override public void init(final Connection connection) {
            this.connection = connection;
        }

        @Override public byte[] handleRequest(final byte[] request) {
            return request;
        }
    }

    // Service handler for the request/reply failure tests.
    static class RequestTestFailHandler implements ServiceHandler {
        Connection connection;

        @Override public void init(final Connection connection) {
            this.connection = connection;
        }

        @Override public byte[] handleRequest(final byte[] request) throws RemoteException {
            throw new RemoteException(new String(request));
        }
    }

    // Tests multiple concurrent client and service requests.
    @BenchmarkOptions(benchmarkRounds = 5, warmupRounds = 10)
    @Test public void concurrentRequests() throws Exception {
        final int CLIENT_COUNT = 25, SERVER_COUNT = 25, REQUEST_COUNT = 25;

        final Collection<Thread> workers = new ArrayList<>(CLIENT_COUNT + SERVER_COUNT);
        final CyclicBarrier barrier = new CyclicBarrier(CLIENT_COUNT + SERVER_COUNT + 1);
        final List<Exception> errors = Collections.synchronizedList(new ArrayList<>());

        // Start up the concurrent requesting clients
        for (int i = 0; i < CLIENT_COUNT; i++) {
            final int client = i;
            final Thread worker = new Thread(() -> {
                try (final Connection conn = new Connection(Config.RELAY_PORT)) {
                    // Wait till all clients and servers connect
                    barrier.await(Config.PHASE_TIMEOUT, TimeUnit.SECONDS);

                    // Request from the service cluster
                    for (int j = 0; j < REQUEST_COUNT; j++) {
                        final String request = String.format("client #%d, request %d", client, j);
                        final byte[] requestBlob = request.getBytes(StandardCharsets.UTF_8);

                        final byte[] replyBlob = conn.request(Config.CLUSTER_NAME, requestBlob, 1000);
                        final String reply = new String(replyBlob, StandardCharsets.UTF_8);

                        Assert.assertEquals(reply, request);
                    }
                    barrier.await(Config.PHASE_TIMEOUT, TimeUnit.SECONDS);
                } catch (Exception e) {
                    errors.add(e);
                }
            });
            worker.start();
            workers.add(worker);
        }

        // Start up the concurrent requesting services
        for (int i = 0; i < SERVER_COUNT; i++) {
            final int server = i;
            final Thread worker = new Thread(() -> {
                final RequestTestHandler handler = new RequestTestHandler();

                try (final Service ignored = new Service(Config.RELAY_PORT, Config.CLUSTER_NAME, handler)) {
                    // Wait till all clients and servers connect
                    barrier.await(Config.PHASE_TIMEOUT, TimeUnit.SECONDS);

                    // Request from the service cluster
                    for (int j = 0; j < REQUEST_COUNT; j++) {
                        final String request = String.format("server #%d, request %d", server, j);
                        final byte[] requestBlob = request.getBytes(StandardCharsets.UTF_8);

                        final byte[] replyBlob = handler.connection.request(Config.CLUSTER_NAME, requestBlob, 1000);
                        final String reply = new String(replyBlob, StandardCharsets.UTF_8);

                        Assert.assertEquals(reply, request);
                    }
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

    // Tests request failure forwarding.
    @BenchmarkOptions(benchmarkRounds = 5, warmupRounds = 10)
    @Test public void fail() throws Exception {
        // Test specific configurations
        final int REQUEST_COUNT = 125;

        // Create the service handler
        final RequestTestFailHandler handler = new RequestTestFailHandler();
        try (final Service ignored = new Service(Config.RELAY_PORT, Config.CLUSTER_NAME, handler)) {
            // Request from the failing service cluster
            for (int i = 0; i < REQUEST_COUNT; i++) {
                final String request = String.format("failure %d", i);
                final byte[] requestBlob = request.getBytes(StandardCharsets.UTF_8);

                try {
                    handler.connection.request(Config.CLUSTER_NAME, requestBlob, 1000);
                    Assert.fail("Request succeeded, should have failed!");
                } catch (RemoteException e) {
                    Assert.assertEquals(request, e.getMessage());
                }
            }
        }
    }

    // Service handler for the thread limited request tests.
    static class RequestTestTimedHandler implements ServiceHandler {
        Connection connection;
        int        sleep;

        @Override public void init(final Connection connection) {
            this.connection = connection;
        }

        @Override public byte[] handleRequest(final byte[] request) {
            try {
                Thread.sleep(sleep);
                return request;
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    // Tests the request thread limitation.
    @BenchmarkOptions(benchmarkRounds = 5, warmupRounds = 10)
    @Test public void timeout() throws Exception {
        final int SLEEP = 100;

        // Create the service handler
        final RequestTestTimedHandler handler = new RequestTestTimedHandler();
        handler.sleep = SLEEP;

        try (final Service ignored = new Service(Config.RELAY_PORT, Config.CLUSTER_NAME, handler)) {
            // Check that the timeouts are complied with.
            handler.connection.request(Config.CLUSTER_NAME, new byte[]{0x00}, SLEEP * 2);

            handler.connection.request(Config.CLUSTER_NAME, new byte[]{0x00}, SLEEP / 2);
            Assert.fail();
        } catch (TimeoutException ignored) {} catch (Exception e) {
            Assert.fail();
        }
    }

    // Tests the request thread limitation.
    @BenchmarkOptions(benchmarkRounds = 5, warmupRounds = 10)
    @Test public void threadLimiting() throws Exception {
        final int REQUEST_COUNT = 4, SLEEP = 250;

        // Create the service handler and limiter
        final RequestTestTimedHandler handler = new RequestTestTimedHandler();
        handler.sleep = SLEEP;

        final ServiceLimits limits = new ServiceLimits();
        limits.requestThreads = 1;

        try (final Service ignored = new Service(Config.RELAY_PORT, Config.CLUSTER_NAME, handler, limits)) {
            // Start a batch of requesters
            final LongAdder done = new LongAdder();
            for (int j = 0; j < REQUEST_COUNT; j++) {
                new Thread(() -> {
                    try {
                        handler.connection.request(Config.CLUSTER_NAME, new byte[]{0x00}, (REQUEST_COUNT + 1) * SLEEP);
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        done.increment();
                    }
                }).start();
            }
            // Wait for half time and verify that only half was processed
            Thread.sleep(((REQUEST_COUNT / 2) * SLEEP) + (SLEEP / 2));
            Assert.assertEquals(REQUEST_COUNT / 2, done.intValue());

            // Wait for the rest to complete
            Thread.sleep(((REQUEST_COUNT / 2) * SLEEP) + (SLEEP / 2));
            Assert.assertEquals(REQUEST_COUNT, done.intValue());
        }
    }

    // Tests the request memory limitation.
    @BenchmarkOptions(benchmarkRounds = 5, warmupRounds = 10)
    @Test public void memoryLimiting() throws Exception {
        // Create the service handler and limiter
        final RequestTestHandler handler = new RequestTestHandler();
        final ServiceLimits limits = new ServiceLimits();
        limits.requestMemory = 1;

        try (final Service ignored = new Service(Config.RELAY_PORT, Config.CLUSTER_NAME, handler, limits)) {
            // Check that a 1 byte request succeeds
            handler.connection.request(Config.CLUSTER_NAME, new byte[]{0x00}, 1000);

            // Check that a 2 byte request fails
            try {
                handler.connection.request(Config.CLUSTER_NAME, new byte[]{0x00, 0x00}, 1000);
                Assert.fail();
            } catch (TimeoutException ignore) { }

            // Check that space freed gets replenished
            handler.connection.request(Config.CLUSTER_NAME, new byte[]{0x00}, 1000);
        }
    }

    // Service handler for the request/reply expiry tests.
    static class RequestTestExpiryHandler implements ServiceHandler {
        Connection connection;
        int        sleep;
        AtomicInteger done = new AtomicInteger(0);

        @Override public void init(final Connection connection) {
            this.connection = connection;
        }

        @Override public byte[] handleRequest(final byte[] request) {
            try {
                Thread.sleep(sleep);
                done.addAndGet(1);
                return request;
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    // Tests that enqueued but expired requests don't get executed.
    @BenchmarkOptions(benchmarkRounds = 5, warmupRounds = 10)
    @Test public void expiration() throws Exception {
        // Test specific configurations
        final int REQUEST_COUNT = 4, SLEEP = 25;

        // Create the service handler and limiter
        final RequestTestExpiryHandler handler = new RequestTestExpiryHandler();
        handler.sleep = SLEEP;

        final ServiceLimits limits = new ServiceLimits();
        limits.requestThreads = 1;

        try (final Service ignored = new Service(Config.RELAY_PORT, Config.CLUSTER_NAME, handler, limits)) {
            // Start a batch of concurrent requesters (all but one should be scheduled remotely)
            final Collection<Thread> workers = new ArrayList<>(REQUEST_COUNT);
            for (int i = 0; i < REQUEST_COUNT; i++) {
                final Thread worker = new Thread(() -> {
                    try {
                        handler.connection.request(Config.CLUSTER_NAME, new byte[]{0x00}, 10);
                    } catch (TimeoutException ignore) {
                        // All ok
                    } catch (IOException | RemoteException | InterruptedException e) {
                        e.printStackTrace();
                    }
                });
                worker.start();
                workers.add(worker);
            }

            // Wait for all of them to complete and verify that all but 1 expired
            for (Thread worker : workers) {
                worker.join();
            }
            Thread.sleep((REQUEST_COUNT + 1) * SLEEP);
            Assert.assertEquals(1, handler.done.get());
        }
    }
}
