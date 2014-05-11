package com.karalabe.iris.callback.handlers;

import com.karalabe.iris.callback.CallbackHandler;
import com.karalabe.iris.callback.StaticCallbackHandler;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

@FunctionalInterface
public interface PublishCallbackHandler extends StaticCallbackHandler, CallbackHandler {
    @NotNull byte[] handleEvent(@NotNull String topic, @NotNull byte[] message) throws IOException;

    @Override default Object getId() { return BroadcastCallbackHandler.getBroadcastId(); }

    static Object getPublishId() { return PublishCallbackHandler.class; }
}