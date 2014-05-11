package com.karalabe.iris.protocols.publish_subscribe;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public interface PublishApi {
    /* Publishes an event asynchronously to topic. No guarantees are made that all subscribers receive the message (best effort).
     * The method does blocks until the message is forwarded to the relay, or an error occurs, in which case an iris.Error is returned. */
    void publish(@NotNull final String topic, @NotNull byte[] message) throws IOException;
}