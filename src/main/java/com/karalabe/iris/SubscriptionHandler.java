package com.karalabe.iris;

import org.jetbrains.annotations.NotNull;

public interface SubscriptionHandler {
    /* Handles an event published to the subscribed topic. */
    default void handleEvent(@NotNull final byte[] message) {
        throw new IllegalStateException("Not implemented!");
    }
}