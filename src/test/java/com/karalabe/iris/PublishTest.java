/*
 * Copyright © 2014 Project Iris. All rights reserved.
 *
 * The current language binding is an official support library of the Iris cloud messaging framework, and as such, the same licensing terms apply.
 * For details please see http://iris.karalabe.com/downloads#License
 */

package com.karalabe.iris;

import com.carrotsearch.junitbenchmarks.AbstractBenchmark;
import com.carrotsearch.junitbenchmarks.BenchmarkOptions;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("JUnitTestNG")
public class PublishTest extends AbstractBenchmark {
    static class PublishSubscribeTestServiceHandler implements ServiceHandler {
        Connection connection;

        @Override public void init(@NotNull final Connection connection) {
            this.connection = connection;
        }
    }

    static class PublishSubscribeTestTopicHandler implements TopicHandler {
        final Set<String> arrived = Collections.synchronizedSet(new HashSet<>());
        Semaphore pending;

        @Override public void handleEvent(@NotNull final byte... event) {
            arrived.add(new String(event, StandardCharsets.UTF_8));
            pending.release(1);
        }
    }

    // Tests multiple concurrent clients and services publishing to a batch of topics.
    @BenchmarkOptions(benchmarkRounds = 5, warmupRounds = 10)
    @Test public void concurrentPublishes() throws Exception {
        final int CLIENT_COUNT = 5, SERVER_COUNT = 5, TOPIC_COUNT = 7, EVENT_COUNT = 15;
        final String[] topics = new String[TOPIC_COUNT];
        for (int i = 0; i < TOPIC_COUNT; i++) {
            topics[i] = String.format("%s-%d", TestConfig.TOPIC_NAME, i);
        }

        final Collection<Thread> workers = new ArrayList<>(CLIENT_COUNT + SERVER_COUNT);
        final CyclicBarrier barrier = new CyclicBarrier(CLIENT_COUNT + SERVER_COUNT + 1);
        final Collection<Exception> errors = Collections.synchronizedList(new ArrayList<>());

        // Start up the concurrent publishing clients
        for (int i = 0; i < CLIENT_COUNT; i++) {
            final int client = i;
            final Thread worker = new Thread(() -> {
                try (final Connection connection = Iris.connect(TestConfig.RELAY_PORT)) {
                    // Wait till all clients and servers connect
                    barrier.await(TestConfig.PHASE_TIMEOUT, TimeUnit.SECONDS);

                    // Subscribe to the batch of topics
                    PublishSubscribeTestTopicHandler[] handlers = new PublishSubscribeTestTopicHandler[TOPIC_COUNT];
                    for (int j = 0; j < TOPIC_COUNT; j++) {
                        handlers[j] = new PublishSubscribeTestTopicHandler();
                        handlers[j].pending = new Semaphore((CLIENT_COUNT + SERVER_COUNT) * EVENT_COUNT);
                        handlers[j].pending.acquire((CLIENT_COUNT + SERVER_COUNT) * EVENT_COUNT);

                        connection.subscribe(topics[j], handlers[j]);
                    }
                    Thread.sleep(100);
                    barrier.await(TestConfig.PHASE_TIMEOUT, TimeUnit.SECONDS);

                    // Publish to all subscribers
                    for (int j = 0; j < EVENT_COUNT; j++) {
                        final String event = String.format("client #%d, event %d", client, j);
                        final byte[] eventBlob = event.getBytes(StandardCharsets.UTF_8);

                        for (int k = 0; k < TOPIC_COUNT; k++) {
                            connection.publish(topics[k], eventBlob);
                        }
                    }
                    barrier.await(TestConfig.PHASE_TIMEOUT, TimeUnit.SECONDS);

                    // Wait for all events to arrive
                    for (PublishSubscribeTestTopicHandler handler : handlers) {
                        verifyEvents(CLIENT_COUNT, SERVER_COUNT, EVENT_COUNT, handler);
                    }
                    barrier.await(TestConfig.PHASE_TIMEOUT, TimeUnit.SECONDS);
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
                PublishSubscribeTestServiceHandler handler = new PublishSubscribeTestServiceHandler();

                try (final Service ignored = Iris.register(TestConfig.RELAY_PORT, TestConfig.CLUSTER_NAME, handler)) {
                    // Wait till all clients and servers connect
                    barrier.await(TestConfig.PHASE_TIMEOUT, TimeUnit.SECONDS);

                    // Subscribe to the batch of topics
                    PublishSubscribeTestTopicHandler[] handlers = new PublishSubscribeTestTopicHandler[TOPIC_COUNT];
                    for (int j = 0; j < TOPIC_COUNT; j++) {
                        handlers[j] = new PublishSubscribeTestTopicHandler();
                        handlers[j].pending = new Semaphore((CLIENT_COUNT + SERVER_COUNT) * EVENT_COUNT);
                        handlers[j].pending.acquire((CLIENT_COUNT + SERVER_COUNT) * EVENT_COUNT);

                        handler.connection.subscribe(topics[j], handlers[j]);
                    }
                    Thread.sleep(100);
                    barrier.await(TestConfig.PHASE_TIMEOUT, TimeUnit.SECONDS);

                    // Publish to all subscribers
                    for (int j = 0; j < EVENT_COUNT; j++) {
                        final String event = String.format("server #%d, event %d", server, j);
                        final byte[] eventBlob = event.getBytes(StandardCharsets.UTF_8);

                        for (int k = 0; k < TOPIC_COUNT; k++) {
                            handler.connection.publish(topics[k], eventBlob);
                        }
                    }
                    barrier.await(TestConfig.PHASE_TIMEOUT, TimeUnit.SECONDS);

                    // Wait for all events to arrive
                    for (PublishSubscribeTestTopicHandler hand : handlers) {
                        verifyEvents(CLIENT_COUNT, SERVER_COUNT, EVENT_COUNT, hand);
                    }
                    barrier.await(TestConfig.PHASE_TIMEOUT, TimeUnit.SECONDS);
                } catch (Exception e) {
                    errors.add(e);
                }
            });
            worker.start();
            workers.add(worker);
        }
        // Schedule the parallel operations
        try {
            barrier.await(TestConfig.PHASE_TIMEOUT, TimeUnit.SECONDS);
            Assert.assertTrue(errors.isEmpty());

            barrier.await(TestConfig.PHASE_TIMEOUT, TimeUnit.SECONDS);
            Assert.assertTrue(errors.isEmpty());

            barrier.await(TestConfig.PHASE_TIMEOUT, TimeUnit.SECONDS);
            Assert.assertTrue(errors.isEmpty());
        } finally {
            for (Thread worker : workers) {
                worker.join();
            }
        }
    }

    private static void verifyEvents(int clientCount, int serverCount, int eventCount, PublishSubscribeTestTopicHandler handler) throws InterruptedException {
        // Wait for all pending events to arrive
        handler.pending.acquire((clientCount + serverCount) * eventCount);

        // Verify that the correct events have arrived
        for (int i = 0; i < eventCount; i++) {
            for (int j = 0; j < clientCount; j++) {
                final String message = String.format("client #%d, event %d", j, i);
                Assert.assertTrue(handler.arrived.contains(message));
            }
            for (int j = 0; j < serverCount; j++) {
                final String message = String.format("server #%d, event %d", j, i);
                Assert.assertTrue(handler.arrived.contains(message));
            }
        }
    }
}
