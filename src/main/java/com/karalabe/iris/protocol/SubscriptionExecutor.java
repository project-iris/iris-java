/*
 * Copyright © 2014 Project Iris. All rights reserved.
 *
 * The current language binding is an official support library of the Iris cloud messaging framework, and as such, the same licensing terms apply.
 * For details please see http://iris.karalabe.com/downloads#License
 */

package com.karalabe.iris.protocol;

import com.karalabe.iris.ProtocolException;
import com.karalabe.iris.TopicHandler;
import com.karalabe.iris.TopicLimits;
import com.karalabe.iris.common.BoundedThreadPool;
import com.karalabe.iris.common.BoundedThreadPool.Terminate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SubscriptionExecutor extends ExecutorBase {
    static class SubscriptionState {
        public TopicHandler      handler; // Callback handler for processing inbound events
        public BoundedThreadPool workers; // Thread pool for limiting the concurrent processing
    }

    private final Map<String, SubscriptionState> activeSubscriptions = new ConcurrentHashMap<>();

    public SubscriptionExecutor(final ProtocolBase protocol) {
        super(protocol);
    }

    public void subscribe(final String topic, final TopicHandler handler, final TopicLimits limits) {
        // Make sure double subscriptions result in a failure
        final SubscriptionState subscriptionState = new SubscriptionState();
        synchronized (activeSubscriptions) {
            if (activeSubscriptions.containsKey(topic)) { throw new ProtocolException("Already subscribed!"); }
            activeSubscriptions.put(topic, subscriptionState);
        }
        // Leave the critical section and finish initialization
        subscriptionState.handler = handler;
        subscriptionState.workers = new BoundedThreadPool(limits.eventThreads, limits.eventMemory);

        protocol.send(OpCode.SUBSCRIBE, () -> protocol.sendString(topic));
    }

    public void unsubscribe(final String topic) throws InterruptedException {
        // Make sure there's an activeSubscriptions subscription
        final SubscriptionState subscriptionState;
        synchronized (activeSubscriptions) {
            if (!activeSubscriptions.containsKey(topic)) { throw new ProtocolException("Not subscribed!"); }
            subscriptionState = activeSubscriptions.remove(topic);
        }
        subscriptionState.workers.terminate(Terminate.NOW); // Leave the critical section and finish cleanup

        protocol.send(OpCode.UNSUBSCRIBE, () -> protocol.sendString(topic));
    }

    public void publish(final String topic, final byte... event) {
        protocol.send(OpCode.PUBLISH, () -> {
            protocol.sendString(topic);
            protocol.sendBinary(event);
        });
    }

    public void handlePublish() {
        final String topic = protocol.receiveString();
        final byte[] event = protocol.receiveBinary();

        final SubscriptionState subscriptionState = activeSubscriptions.get(topic);
        if (subscriptionState != null) {
            subscriptionState.workers.schedule(event.length, () -> subscriptionState.handler.handleEvent(event));
        }
    }

    @Override public void close() throws InterruptedException {
        for (SubscriptionState subscriptionState : activeSubscriptions.values()) {
            subscriptionState.workers.terminate(Terminate.NOW);
        }
    }
}
