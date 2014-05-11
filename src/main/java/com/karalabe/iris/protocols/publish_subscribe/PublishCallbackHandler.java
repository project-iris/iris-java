package com.karalabe.iris.protocols.publish_subscribe;

import com.karalabe.iris.callback.CallbackHandler;
import com.karalabe.iris.callback.StaticCallbackHandler;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

@FunctionalInterface
public interface PublishCallbackHandler extends StaticCallbackHandler, CallbackHandler {
    Object ID = PublishCallbackHandler.class;

    @Override default Object getId() { return ID; }

    void handleEvent(@NotNull String topic, @NotNull byte[] message) throws IOException;
}