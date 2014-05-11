package com.karalabe.iris.callback.handlers;

import com.karalabe.iris.callback.CallbackHandler;
import com.karalabe.iris.callback.InstanceCallbackHandler;
import org.jetbrains.annotations.NotNull;

public interface SubscriptionHandler extends InstanceCallbackHandler, CallbackHandler {
    /* Handles an event published to the subscribed topic. */
    void handleEvent(@NotNull final byte[] message);
}