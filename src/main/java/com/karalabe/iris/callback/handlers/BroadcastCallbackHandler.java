package com.karalabe.iris.callback.handlers;

import com.karalabe.iris.callback.CallbackHandler;
import com.karalabe.iris.callback.StaticCallbackHandler;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

@FunctionalInterface
public interface BroadcastCallbackHandler extends StaticCallbackHandler, CallbackHandler {
    /* Handles a message broadcasted to all applications of the local type.*/
    void handleEvent(@NotNull byte[] message) throws IOException;

    @Override default Object getId() { return BroadcastCallbackHandler.getBroadcastId(); }

    static Object getBroadcastId() { return BroadcastCallbackHandler.class; }
}