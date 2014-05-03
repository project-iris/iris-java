package com.karalabe.iris;

import org.jetbrains.annotations.NotNull;

public abstract class SubscriptionHandler {
    /* Handles an event published to the subscribed topic. */
    public void handleEvent(@NotNull final byte[] message) {
        throw new IllegalStateException("Not implemented!");
    }
}