package com.karalabe.iris.protocols.publish_subscribe;

import com.karalabe.iris.TopicHandler;
import com.karalabe.iris.TopicLimits;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public interface SubscribeApi {
    /* Subscribes to topic, using handler as the callback for arriving events.
     * The method blocks until the subscription is forwarded to the relay, or an error occurs, in which case an iris.Error is returned.
     * Double subscription is considered a programming error and will result in a panic! */
    void subscribe(@NotNull final String topic, @NotNull TopicHandler handler, final TopicLimits limits) throws IOException;

    /* Unsubscribes from topic, receiving no more event notifications for it.
     * The method does blocks until the unsubscription is forwarded to the relay, or an error occurs, in which case an iris.Error is returned.
     * Unsubscribing from a topic not subscribed to is considered a programming error and will result in a panic! */
    void unsubscribe(@NotNull final String topic, @NotNull final TopicHandler handler) throws IOException;
}