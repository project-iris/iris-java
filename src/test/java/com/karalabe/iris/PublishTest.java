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
import java.util.*;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("JUnitTestNG")
public class PublishTest extends AbstractBenchmark {
    // Service handler for the publish/subscribe tests.
    static class PublishTestServiceHandler implements ServiceHandler {
        Connection connection;

        @Override public void init(@NotNull final Connection connection) {
            this.connection = connection;
        }
    }

    // Topic handler for the publish/subscribe tests
    static class PublishTestTopicHandler implements TopicHandler {
        final Set<String> arrived = Collections.synchronizedSet(new HashSet<>());
        Semaphore pending;

        @Override public void handleEvent(@NotNull final byte[] event) {
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
            topics[i] = String.format("%s-%d", Config.TOPIC_NAME, i);
        }

        final Collection<Thread> workers = new ArrayList<>(CLIENT_COUNT + SERVER_COUNT);
        final CyclicBarrier barrier = new CyclicBarrier(CLIENT_COUNT + SERVER_COUNT + 1);
        final List<Exception> errors = Collections.synchronizedList(new ArrayList<>());

        // Start up the concurrent publishing clients
        for (int i = 0; i < CLIENT_COUNT; i++) {
            final int client = i;
            final Thread worker = new Thread(() -> {
                try (final Connection conn = Iris.connect(Config.RELAY_PORT)) {
                    // Wait till all clients and servers connect
                    barrier.await(Config.PHASE_TIMEOUT, TimeUnit.SECONDS);

                    // Subscribe to the batch of topics
                    PublishTestTopicHandler[] handlers = new PublishTestTopicHandler[TOPIC_COUNT];
                    for (int j = 0; j < TOPIC_COUNT; j++) {
                        handlers[j] = new PublishTestTopicHandler();
                        handlers[j].pending = new Semaphore((CLIENT_COUNT + SERVER_COUNT) * EVENT_COUNT);
                        handlers[j].pending.acquire((CLIENT_COUNT + SERVER_COUNT) * EVENT_COUNT);

                        conn.subscribe(topics[j], handlers[j]);
                    }
                    Thread.sleep(100);
                    barrier.await(Config.PHASE_TIMEOUT, TimeUnit.SECONDS);

                    // Publish to all subscribers
                    for (int j = 0; j < EVENT_COUNT; j++) {
                        final String event = String.format("client #%d, event %d", client, j);
                        final byte[] eventBlob = event.getBytes(StandardCharsets.UTF_8);

                        for (int k = 0; k < TOPIC_COUNT; k++) {
                            conn.publish(topics[k], eventBlob);
                        }
                    }
                    barrier.await(Config.PHASE_TIMEOUT, TimeUnit.SECONDS);

                    // Wait for all events to arrive
                    for (PublishTestTopicHandler handler : handlers) {
                        verifyEvents(CLIENT_COUNT, SERVER_COUNT, EVENT_COUNT, handler);
                    }
                    barrier.await(Config.PHASE_TIMEOUT, TimeUnit.SECONDS);
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
                PublishTestServiceHandler handler = new PublishTestServiceHandler();

                try (final Service ignored = Iris.register(Config.RELAY_PORT, Config.CLUSTER_NAME, handler)) {
                    // Wait till all clients and servers connect
                    barrier.await(Config.PHASE_TIMEOUT, TimeUnit.SECONDS);

                    // Subscribe to the batch of topics
                    PublishTestTopicHandler[] handlers = new PublishTestTopicHandler[TOPIC_COUNT];
                    for (int j = 0; j < TOPIC_COUNT; j++) {
                        handlers[j] = new PublishTestTopicHandler();
                        handlers[j].pending = new Semaphore((CLIENT_COUNT + SERVER_COUNT) * EVENT_COUNT);
                        handlers[j].pending.acquire((CLIENT_COUNT + SERVER_COUNT) * EVENT_COUNT);

                        handler.connection.subscribe(topics[j], handlers[j]);
                    }
                    Thread.sleep(100);
                    barrier.await(Config.PHASE_TIMEOUT, TimeUnit.SECONDS);

                    // Publish to all subscribers
                    for (int j = 0; j < EVENT_COUNT; j++) {
                        final String event = String.format("server #%d, event %d", server, j);
                        final byte[] eventBlob = event.getBytes(StandardCharsets.UTF_8);

                        for (int k = 0; k < TOPIC_COUNT; k++) {
                            handler.connection.publish(topics[k], eventBlob);
                        }
                    }
                    barrier.await(Config.PHASE_TIMEOUT, TimeUnit.SECONDS);

                    // Wait for all events to arrive
                    for (PublishTestTopicHandler hand : handlers) {
                        verifyEvents(CLIENT_COUNT, SERVER_COUNT, EVENT_COUNT, hand);
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

            barrier.await(Config.PHASE_TIMEOUT, TimeUnit.SECONDS);
            Assert.assertTrue(errors.isEmpty());
        } finally {
            for (Thread worker : workers) {
                worker.join();
            }
        }
    }

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
}
