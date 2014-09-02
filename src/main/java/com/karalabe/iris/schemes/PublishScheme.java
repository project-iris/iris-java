// Copyright (c) 2014 Project Iris. All rights reserved.
//
// The current language binding is an official support library of the Iris
// cloud messaging framework, and as such, the same licensing terms apply.
// For details please see http://iris.karalabe.com/downloads#License
package com.karalabe.iris.schemes;

import com.karalabe.iris.TopicHandler;
import com.karalabe.iris.TopicLimits;
import com.karalabe.iris.common.BoundedThreadPool;
import com.karalabe.iris.protocol.RelayProtocol;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Implements the publish/subscribe communication pattern.
public class PublishScheme {
    // Simple container for an individual subscription's state
    static class Subscription {
        public TopicHandler      handler; // Callback handler for processing inbound events
        public BoundedThreadPool workers; // Thread pool for limiting the concurrent processing
    }

    private final RelayProtocol protocol;                           // Network connection implementing the relay protocol
    private final Map<String, Subscription> active = new ConcurrentHashMap<>(); // Active topic subscription pool

    // Constructs a publish/subscribe scheme implementation.
    public PublishScheme(final RelayProtocol protocol) {
        this.protocol = protocol;
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
        sub.handler = handler;
        sub.workers = new BoundedThreadPool(limits.eventThreads, limits.eventMemory);

        protocol.sendSubscribe(topic);
    }

    // Relays a subscription removal request to the local Iris node.
    public void unsubscribe(final String topic) throws IOException, InterruptedException {
        // Make sure there's an active subscription
        final Subscription sub;
        synchronized (active) {
            if (!active.containsKey(topic)) {
                throw new IllegalStateException("Not subscribed!");
            }
            sub = active.remove(topic);
        }
        // Leave the critical section and finish cleanup
        sub.workers.terminate(true);

        protocol.sendUnsubscribe(topic);
    }

    // Relays an event publish to the local Iris node.
    public void publish(final String topic, final byte[] event) throws IOException {
        protocol.sendPublish(topic, event);
    }

    // Forwards a topic publish event to the topic subscription.
    public void handlePublish(final String topic, final byte[] event) throws IOException {
        final Subscription sub = active.get(topic);
        if (sub != null) {
            sub.workers.schedule(() -> sub.handler.handleEvent(event), event.length);
        }
    }

    // Terminates the publish/subscribe primitive.
    public void close() throws InterruptedException {
        for (Subscription sub : active.values()) {
            sub.workers.terminate(true);
        }
    }
}
