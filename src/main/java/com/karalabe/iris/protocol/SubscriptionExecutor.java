// Copyright (c) 2014 Project Iris. All rights reserved.
//
// The current language binding is an official support library of the Iris
// cloud messaging framework, and as such, the same licensing terms apply.
// For details please see http://iris.karalabe.com/downloads#License
package com.karalabe.iris.protocol;

import com.karalabe.iris.TopicHandler;
import com.karalabe.iris.TopicLimits;
import com.karalabe.iris.common.BoundedThreadPool;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SubscriptionExecutor extends ExecutorBase {
    // Simple container for an individual subscription's state
    private class Subscription {
        public TopicHandler      handler; // Callback handler for processing inbound events
        public BoundedThreadPool workers; // Thread pool for limiting the concurrent processing
    }

    private Map<String, Subscription> active = new ConcurrentHashMap<>(0);

    public SubscriptionExecutor(final ProtocolBase protocol) {
        super(protocol);
    }

    public void subscribe(final String topic, final TopicHandler handler, final TopicLimits limits) throws IOException {
        // Make sure double subscriptions result in a failure
        Subscription sub = new Subscription();
        synchronized (active) {
            if (active.containsKey(topic)) {
                throw new IllegalStateException("Already subscribed!");
            }
            active.put(topic, sub);
        }
        // Leave the critical section and finish initialization
        sub.handler = handler;
        sub.workers = new BoundedThreadPool(limits.eventThreads, limits.eventMemory);

        protocol.send(OpCode.SUBSCRIBE, () -> {
            protocol.sendString(topic);
        });
    }

    public void unsubscribe(final String topic) throws IOException, InterruptedException {
        // Make sure there's an active subscription
        Subscription sub = null;
        synchronized (active) {
            if (!active.containsKey(topic)) {
                throw new IllegalStateException("Not subscribed!");
            }
            sub = active.remove(topic);
        }
        // Leave the critical section and finish cleanup
        sub.workers.terminate(true);

        protocol.send(OpCode.UNSUBSCRIBE, () -> {
            protocol.sendString(topic);
        });
    }

    public void publish(final String topic, final byte[] event) throws IOException {
        protocol.send(OpCode.PUBLISH, () -> {
            protocol.sendString(topic);
            protocol.sendBinary(event);
        });
    }

    public void handlePublish() throws IOException {
        final String topic = protocol.receiveString();
        final byte[] event = protocol.receiveBinary();

        Subscription sub = active.get(topic);
        if (sub != null) {
            sub.workers.schedule(() -> {
                sub.handler.handleEvent(event);
            }, event.length);
        }
    }

    @Override public void close() throws Exception {
        for (Subscription sub : active.values()) {
            sub.workers.terminate(true);
        }
    }
}
