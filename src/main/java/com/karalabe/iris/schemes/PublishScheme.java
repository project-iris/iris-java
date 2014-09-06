// Copyright (c) 2014 Project Iris. All rights reserved.
//
// The current language binding is an official support library of the Iris
// cloud messaging framework, and as such, the same licensing terms apply.
// For details please see http://iris.karalabe.com/downloads#License
package com.karalabe.iris.schemes;

import com.karalabe.iris.TopicHandler;
import com.karalabe.iris.TopicLimits;
import com.karalabe.iris.common.BoundedThreadPool;
import com.karalabe.iris.common.ContextualLogger;
import com.karalabe.iris.protocol.RelayProtocol;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

// Implements the publish/subscribe communication pattern.
public class PublishScheme {
    // Simple container for an individual subscription's state
    static class Subscription {
        public TopicHandler      handler; // Callback handler for processing inbound events
        public TopicLimits       limits;  // Subscription handler resource consumption allowance
        public BoundedThreadPool workers; // Thread pool for limiting the concurrent processing
        public ContextualLogger  logger;  // Logger with connection and topic id injected
    }

    private final RelayProtocol    protocol; // Network connection implementing the relay protocol
    private final ContextualLogger logger;   // Logger with connection id injected

    private final AtomicInteger             nextId = new AtomicInteger();       // Unique identifier for the next subscription
    private final Map<String, Subscription> active = new ConcurrentHashMap<>(); // Active topic subscription pool

    // Constructs a publish/subscribe scheme implementation.
    public PublishScheme(final RelayProtocol protocol, final ContextualLogger logger) {
        this.protocol = protocol;
        this.logger = logger;
    }

    // Relays a subscription request to the local Iris node.
    public void subscribe(final String topic, final TopicHandler handler, final TopicLimits limits) throws IOException {
        // Make sure double subscriptions result in a failure
        final Subscription sub = new Subscription();
        synchronized (active) {
            if (active.containsKey(topic)) {
                throw new IllegalStateException("Already subscribed!");
            }
            active.put(topic, sub);
        }
        // Leave the critical section and finish initialization
        sub.logger  = new ContextualLogger(logger, "topic", String.valueOf(nextId.incrementAndGet()));
        sub.logger.loadContext();
        sub.logger.info("Subscribing to new topic", "name", topic,
                        "limits", String.format("%dT|%dB", limits.eventThreads, limits.eventMemory));

        sub.handler = handler;
        sub.limits = limits;
        sub.workers = new BoundedThreadPool(limits.eventThreads, limits.eventMemory);

        try {
            protocol.sendSubscribe(topic);
        } finally {
            sub.logger.unloadContext();
        }
    }

    // Relays a subscription removal request to the local Iris node.
    public void unsubscribe(final String topic) throws IOException {
        // Make sure there's an active subscription
        final Subscription sub;
        synchronized (active) {
            if (!active.containsKey(topic)) {
                throw new IllegalStateException("Not subscribed!");
            }
            sub = active.remove(topic);
        }
        // Leave the critical section and finish cleanup
        try {
            sub.workers.terminate(true);
        } catch (InterruptedException ignored) {
            // Someone just killed our killer
        }
        try {
            sub.logger.loadContext();
            sub.logger.info("Unsubscribing from topic");

            protocol.sendUnsubscribe(topic);
        } finally {
            sub.logger.unloadContext();
        }
    }

    // Relays an event publish to the local Iris node.
    public void publish(final String topic, final byte[] event) throws IOException {
        protocol.sendPublish(topic, event);
    }

    // Forwards a topic publish event to the topic subscription.
    public void handlePublish(final String topic, final byte[] event) throws IOException {
        final Subscription sub = active.get(topic);
        if (sub != null) {
            if (!sub.workers.schedule(() -> sub.handler.handleEvent(event), event.length)) {
                sub.logger.loadContext();
                sub.logger.error("Event exceeded memory allowance",
                                "limit", String.valueOf(sub.limits.eventMemory),
                                "size", String.valueOf(event.length));
                sub.logger.unloadContext();
            }
        } else {
            logger.loadContext();
            logger.warn("Stale publish arrived", "topic", topic);
            logger.unloadContext();
        }
    }

    // Terminates the publish/subscribe primitive.
    public void close() throws InterruptedException {
        for (final Subscription sub : active.values()) {
            sub.logger.loadContext();
            sub.logger.warn("Forcefully terminating subscription");
            sub.logger.unloadContext();

            sub.workers.terminate(false);
        }
    }
}
