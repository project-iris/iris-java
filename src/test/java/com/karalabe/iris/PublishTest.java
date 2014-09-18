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

@SuppressWarnings({"JUnitTestNG", "ProhibitedExceptionDeclared"})
public class PublishTest extends AbstractBenchmark {
    // Topic handler for the publish/subscribe tests
    static class PublishTestTopicHandler implements TopicHandler {
        final Set<String> arrived = Collections.synchronizedSet(new HashSet<>());
        final Semaphore   pending = new Semaphore(0);
        final int sleep;

        PublishTestTopicHandler(final int sleep) { this.sleep = sleep; }

        @Override public void handleEvent(final byte[] event) {
            // Simulate some processing time
            try {
                Thread.sleep(sleep);
            } catch (InterruptedException ignore) { }

            // Store the arrived event and signal its arrival
            arrived.add(new String(event, StandardCharsets.UTF_8));
            pending.release();
        }
    }

    // Tests multiple concurrent clients and services publishing to a batch of topics.
    @BenchmarkOptions(benchmarkRounds = 5, warmupRounds = 10)
    @Test public void concurrentPublishes() throws Exception {
        final int CLIENT_COUNT = 5, SERVER_COUNT = 5, TOPIC_COUNT = 7, EVENT_COUNT = 15;
        final String[] topics = new String[TOPIC_COUNT];
        for (int i = 0; i < TOPIC_COUNT; i++) {
            topics[i] = String.format("%s-%d", TestConfigs.TOPIC_NAME, i);
        }

        final Collection<Thread> workers = new ArrayList<>(CLIENT_COUNT + SERVER_COUNT);
        final CyclicBarrier barrier = new CyclicBarrier(CLIENT_COUNT + SERVER_COUNT + 1);
        final List<Exception> errors = Collections.synchronizedList(new ArrayList<>());

        // Start up the concurrent publishing clients
        for (int i = 0; i < CLIENT_COUNT; i++) {
            final int client = i;
            final Thread worker = new Thread(() -> {
                try (final Connection conn = new Connection(TestConfigs.RELAY_PORT)) {
                    // Wait till all clients and servers connect
                    barrier.await(TestConfigs.PHASE_TIMEOUT, TimeUnit.SECONDS);

                    // Subscribe to the batch of topics
                    PublishTestTopicHandler[] handlers = new PublishTestTopicHandler[TOPIC_COUNT];
                    for (int j = 0; j < TOPIC_COUNT; j++) {
                        handlers[j] = new PublishTestTopicHandler(0);
                        conn.subscribe(topics[j], handlers[j]);
                    }
                    Thread.sleep(100);
                    barrier.await(TestConfigs.PHASE_TIMEOUT, TimeUnit.SECONDS);

                    // Publish to all subscribers
                    for (int j = 0; j < EVENT_COUNT; j++) {
                        final String event = String.format("client #%d, event %d", client, j);
                        final byte[] eventBlob = event.getBytes(StandardCharsets.UTF_8);

                        for (int k = 0; k < TOPIC_COUNT; k++) {
                            conn.publish(topics[k], eventBlob);
                        }
                    }
                    barrier.await(TestConfigs.PHASE_TIMEOUT, TimeUnit.SECONDS);

                    // Wait for all events to arrive
                    for (PublishTestTopicHandler handler : handlers) {
                        verifyEvents(CLIENT_COUNT, SERVER_COUNT, EVENT_COUNT, handler);
                    }
                    barrier.await(TestConfigs.PHASE_TIMEOUT, TimeUnit.SECONDS);

                    // Clean up the topic subscriptions
                    for (String topic : topics) {
                        conn.unsubscribe(topic);
                    }
                } catch (Exception e) {
                    errors.add(e);
                }
            });
            worker.start();
            workers.add(worker);
        }
        // Start up the concurrent publishing services
        for (int i = 0; i < SERVER_COUNT; i++) {
            final int server = i;
            final Thread worker = new Thread(() -> {
                BaseServiceHandler handler = new BaseServiceHandler();

                try (final Service ignored = new Service(TestConfigs.RELAY_PORT, TestConfigs.CLUSTER_NAME, handler)) {
                    // Wait till all clients and servers connect
                    barrier.await(TestConfigs.PHASE_TIMEOUT, TimeUnit.SECONDS);

                    // Subscribe to the batch of topics
                    PublishTestTopicHandler[] handlers = new PublishTestTopicHandler[TOPIC_COUNT];
                    for (int j = 0; j < TOPIC_COUNT; j++) {
                        handlers[j] = new PublishTestTopicHandler(0);
                        handler.connection.subscribe(topics[j], handlers[j]);
                    }
                    Thread.sleep(100);
                    barrier.await(TestConfigs.PHASE_TIMEOUT, TimeUnit.SECONDS);

                    // Publish to all subscribers
                    for (int j = 0; j < EVENT_COUNT; j++) {
                        final String event = String.format("server #%d, event %d", server, j);
                        final byte[] eventBlob = event.getBytes(StandardCharsets.UTF_8);

                        for (int k = 0; k < TOPIC_COUNT; k++) {
                            handler.connection.publish(topics[k], eventBlob);
                        }
                    }
                    barrier.await(TestConfigs.PHASE_TIMEOUT, TimeUnit.SECONDS);

                    // Wait for all events to arrive
                    for (PublishTestTopicHandler hand : handlers) {
                        verifyEvents(CLIENT_COUNT, SERVER_COUNT, EVENT_COUNT, hand);
                    }
                    barrier.await(TestConfigs.PHASE_TIMEOUT, TimeUnit.SECONDS);

                    // Clean up the topic subscriptions
                    for (String topic : topics) {
                        handler.connection.unsubscribe(topic);
                    }
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

    // Verifies the delivered topic events.
    private static void verifyEvents(int clients, int servers, int events, PublishTestTopicHandler handler) throws InterruptedException {
        // Wait for all pending events to arrive
        handler.pending.acquire((clients + servers) * events);

        // Verify that the correct events have arrived
        for (int j = 0; j < clients; j++) {
            for (int k = 0; k < events; k++) {
                final String message = String.format("client #%d, event %d", j, k);
                Assert.assertTrue(handler.arrived.contains(message));
            }
        }
        for (int j = 0; j < servers; j++) {
            for (int k = 0; k < events; k++) {
                final String message = String.format("server #%d, event %d", j, k);
                Assert.assertTrue(handler.arrived.contains(message));
            }
        }
    }

    // Tests the topic subscription thread limitation.
    @BenchmarkOptions(benchmarkRounds = 5, warmupRounds = 10)
    @Test public void threadLimiting() throws Exception {
        // Test specific configurations
        final int EVENT_COUNT = 4, SLEEP = 100;

        // Connect to the local relay
        try (final Connection conn = new Connection(TestConfigs.RELAY_PORT)) {
            // Subscribe to a topic and wait for state propagation
            final PublishTestTopicHandler handler = new PublishTestTopicHandler(SLEEP);
            final TopicLimits limits = new TopicLimits();
            limits.eventThreads = 1;

            conn.subscribe(TestConfigs.TOPIC_NAME, handler, limits);
            Thread.sleep(100);

            // Send a few publishes
            for (int i = 0; i < EVENT_COUNT; i++) {
                conn.publish(TestConfigs.TOPIC_NAME, new byte[]{(byte) i});
            }
            // Wait for half time and verify that only half was processed
            Thread.sleep((EVENT_COUNT / 2) * SLEEP + SLEEP / 2);
            Assert.assertEquals(EVENT_COUNT / 2, handler.arrived.size());

            // Clean up the topic subscription
            conn.unsubscribe(TestConfigs.TOPIC_NAME);
        }
    }

    // Tests the subscription memory limitation.
    @BenchmarkOptions(benchmarkRounds = 5, warmupRounds = 10)
    @Test public void memoryLimiting() throws Exception {
        try (final Connection conn = new Connection(TestConfigs.RELAY_PORT)) {
            // Subscribe to a topic and wait for state propagation
            final PublishTestTopicHandler handler = new PublishTestTopicHandler(0);
            final TopicLimits limits = new TopicLimits();
            limits.eventMemory = 1;

            conn.subscribe(TestConfigs.TOPIC_NAME, handler, limits);
            Thread.sleep(100);

            // Check that a 1 byte publish passes
            conn.publish(TestConfigs.TOPIC_NAME, new byte[]{0x00});
            Assert.assertTrue(handler.pending.tryAcquire(100, TimeUnit.MILLISECONDS));

            // Check that a 2 byte publish is dropped
            conn.publish(TestConfigs.TOPIC_NAME, new byte[]{0x00, 0x00});
            Assert.assertFalse(handler.pending.tryAcquire(100, TimeUnit.MILLISECONDS));

            // Check that space freed gets replenished
            conn.publish(TestConfigs.TOPIC_NAME, new byte[]{0x00});
            Assert.assertTrue(handler.pending.tryAcquire(100, TimeUnit.MILLISECONDS));

            // Clean up the topic subscription
            conn.unsubscribe(TestConfigs.TOPIC_NAME);
        }
    }
}
