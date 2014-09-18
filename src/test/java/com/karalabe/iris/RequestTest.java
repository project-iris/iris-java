// Copyright (c) 2014 Project Iris. All rights reserved.
//
// The current language binding is an official support library of the Iris
// cloud messaging framework, and as such, the same licensing terms apply.
// For details please see http://iris.karalabe.com/downloads#License
package com.karalabe.iris;

import com.carrotsearch.junitbenchmarks.AbstractBenchmark;
import com.carrotsearch.junitbenchmarks.BenchmarkOptions;
import com.karalabe.iris.exceptions.ClosedException;
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
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

@SuppressWarnings({"JUnitTestNG", "ProhibitedExceptionDeclared"})
public class RequestTest extends AbstractBenchmark {
    // Service handler for the request/reply tests.
    static class RequestTestSuccessHandler extends BaseServiceHandler {
        final AtomicInteger done = new AtomicInteger(0);
        final int sleep;

        RequestTestSuccessHandler(final int sleep) { this.sleep = sleep; }

        @Override public byte[] handleRequest(final byte[] request) {
            // Simulate some processing time
            try {
                Thread.sleep(sleep);
            } catch (InterruptedException ignore) { }

            // Return the processing result
            done.incrementAndGet();
            return request;
        }
    }

    // Service handler for the request/reply failure tests.
    static class RequestTestFailureHandler extends BaseServiceHandler {
        @Override public byte[] handleRequest(final byte[] request) throws RemoteException {
            throw new RemoteException(new String(request, StandardCharsets.UTF_8));
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
                try (final Connection conn = new Connection(TestConfigs.RELAY_PORT)) {
                    // Wait till all clients and servers connect
                    barrier.await(TestConfigs.PHASE_TIMEOUT, TimeUnit.SECONDS);

                    // Request from the service cluster
                    for (int j = 0; j < REQUEST_COUNT; j++) {
                        final String request = String.format("client #%d, request %d", client, j);
                        final byte[] requestBlob = request.getBytes(StandardCharsets.UTF_8);

                        final byte[] replyBlob = conn.request(TestConfigs.CLUSTER_NAME, requestBlob, 1000);
                        final String reply = new String(replyBlob, StandardCharsets.UTF_8);

                        Assert.assertEquals(reply, request);
                    }
                    barrier.await(TestConfigs.PHASE_TIMEOUT, TimeUnit.SECONDS);
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
                final RequestTestSuccessHandler handler = new RequestTestSuccessHandler(0);

                try (final Service ignored = new Service(TestConfigs.RELAY_PORT, TestConfigs.CLUSTER_NAME, handler)) {
                    // Wait till all clients and servers connect
                    barrier.await(TestConfigs.PHASE_TIMEOUT, TimeUnit.SECONDS);

                    // Request from the service cluster
                    for (int j = 0; j < REQUEST_COUNT; j++) {
                        final String request = String.format("server #%d, request %d", server, j);
                        final byte[] requestBlob = request.getBytes(StandardCharsets.UTF_8);

                        final byte[] replyBlob = handler.connection.request(TestConfigs.CLUSTER_NAME, requestBlob, 1000);
                        final String reply = new String(replyBlob, StandardCharsets.UTF_8);

                        Assert.assertEquals(reply, request);
                    }
                    barrier.await(TestConfigs.PHASE_TIMEOUT, TimeUnit.SECONDS);
                } catch (Exception e) {
                    errors.add(e);
                }
            });
            worker.start();
            workers.add(worker);
        }
        // Schedule the parallel operations
        try {
            barrier.await(TestConfigs.PHASE_TIMEOUT, TimeUnit.SECONDS);
            Assert.assertTrue(errors.isEmpty());

            barrier.await(TestConfigs.PHASE_TIMEOUT, TimeUnit.SECONDS);
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
        final RequestTestFailureHandler handler = new RequestTestFailureHandler();
        try (final Service ignored = new Service(TestConfigs.RELAY_PORT, TestConfigs.CLUSTER_NAME, handler)) {
            // Request from the failing service cluster
            for (int i = 0; i < REQUEST_COUNT; i++) {
                final String request = String.format("failure %d", i);
                final byte[] requestBlob = request.getBytes(StandardCharsets.UTF_8);

                try {
                    handler.connection.request(TestConfigs.CLUSTER_NAME, requestBlob, 1000);
                    Assert.fail("Request succeeded, should have failed!");
                } catch (RemoteException e) {
                    Assert.assertEquals(request, e.getMessage());
                }
            }
        }
    }

    // Tests the request thread limitation.
    @BenchmarkOptions(benchmarkRounds = 5, warmupRounds = 10)
    @Test public void timeout() throws Exception {
        final int SLEEP = 100;

        final RequestTestSuccessHandler handler = new RequestTestSuccessHandler(SLEEP);
        try (final Service ignored = new Service(TestConfigs.RELAY_PORT, TestConfigs.CLUSTER_NAME, handler)) {
            // Check that the timeouts are complied with.
            handler.connection.request(TestConfigs.CLUSTER_NAME, new byte[]{0x00}, SLEEP * 2);

            handler.connection.request(TestConfigs.CLUSTER_NAME, new byte[]{0x00}, SLEEP / 2);
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
        final RequestTestSuccessHandler handler = new RequestTestSuccessHandler(SLEEP);
        final ServiceLimits limits = new ServiceLimits();
        limits.requestThreads = 1;

        try (final Service ignored = new Service(TestConfigs.RELAY_PORT, TestConfigs.CLUSTER_NAME, handler, limits)) {
            // Start a batch of requesters
            final LongAdder done = new LongAdder();
            for (int j = 0; j < REQUEST_COUNT; j++) {
                new Thread(() -> {
                    try {
                        handler.connection.request(TestConfigs.CLUSTER_NAME, new byte[]{0x00}, (REQUEST_COUNT + 1) * SLEEP);
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
        final RequestTestSuccessHandler handler = new RequestTestSuccessHandler(0);
        final ServiceLimits limits = new ServiceLimits();
        limits.requestMemory = 1;

        try (final Service ignored = new Service(TestConfigs.RELAY_PORT, TestConfigs.CLUSTER_NAME, handler, limits)) {
            // Check that a 1 byte request succeeds
            handler.connection.request(TestConfigs.CLUSTER_NAME, new byte[]{0x00}, 1000);

            // Check that a 2 byte request fails
            try {
                handler.connection.request(TestConfigs.CLUSTER_NAME, new byte[]{0x00, 0x00}, 1000);
                Assert.fail();
            } catch (TimeoutException ignore) { }

            // Check that space freed gets replenished
            handler.connection.request(TestConfigs.CLUSTER_NAME, new byte[]{0x00}, 1000);
        }
    }

    // Tests that enqueued but expired requests don't get executed.
    @BenchmarkOptions(benchmarkRounds = 5, warmupRounds = 10)
    @Test public void expiration() throws Exception {
        // Test specific configurations
        final int REQUEST_COUNT = 4, SLEEP = 25;

        // Create the service handler and limiter
        final RequestTestSuccessHandler handler = new RequestTestSuccessHandler(SLEEP);
        final ServiceLimits limits = new ServiceLimits();
        limits.requestThreads = 1;

        try (final Service ignored = new Service(TestConfigs.RELAY_PORT, TestConfigs.CLUSTER_NAME, handler, limits)) {
            // Start a batch of concurrent requesters (all but one should be scheduled remotely)
            final Collection<Thread> workers = new ArrayList<>(REQUEST_COUNT);
            for (int i = 0; i < REQUEST_COUNT; i++) {
                final Thread worker = new Thread(() -> {
                    try {
                        handler.connection.request(TestConfigs.CLUSTER_NAME, new byte[]{0x00}, 10);
                    } catch (TimeoutException ignore) {
                        // All ok
                    } catch (IOException | RemoteException | ClosedException e) {
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

    // Tests that a failing connection interrupts pending requests.
    @BenchmarkOptions(benchmarkRounds = 5, warmupRounds = 10)
    @Test public void terminate() throws Exception {
        // Test specific configurations
        final int SLEEP = 250;

        // Create the service handler and register it
        final RequestTestSuccessHandler handler = new RequestTestSuccessHandler(SLEEP);
        try (final Service ignored = new Service(TestConfigs.RELAY_PORT, TestConfigs.CLUSTER_NAME, handler)) {
            // Connect with a client connection
            final Connection conn = new Connection(TestConfigs.RELAY_PORT);

            // Issue a request, but close before reply arrives
            final Semaphore done = new Semaphore(0);
            new Thread(() -> {
                try {
                    conn.request(TestConfigs.CLUSTER_NAME, new byte[]{0x00}, 1000);
                } catch (IOException | RemoteException | TimeoutException ignore) {
                    // Not what we expected, time out
                } catch (ClosedException ignore) {
                    done.release();
                }
            }).start();

            // Wait a while to make sure request propagates and close the connection
            Thread.sleep(SLEEP / 2);
            conn.close();

            // Verify the request interruption and failure to schedule new
            Assert.assertTrue(done.tryAcquire(SLEEP, TimeUnit.MILLISECONDS));
            try {
                conn.request(TestConfigs.CLUSTER_NAME, new byte[]{0x00}, 1000);
                Assert.fail("Request succeeded on closed connection");
            } catch (ClosedException ignore) {
                // Ok, connection was indeed closed
            } catch (Exception e) {
                Assert.fail("Request didn't report closure: " + e.getMessage());
            }
        }
    }
}
