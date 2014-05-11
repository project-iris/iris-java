package com.karalabe.iris.protocols.broadcast;

import com.karalabe.iris.callback.CallbackHandler;
import com.karalabe.iris.callback.StaticCallbackHandler;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

@FunctionalInterface
public interface BroadcastCallbackHandler extends StaticCallbackHandler, CallbackHandler {
    Object ID = BroadcastCallbackHandler.class;

    @Override default Object getId() { return ID; }

    /* Handles a message broadcasted to all applications of the local type. */
    void handleEvent(@NotNull byte[] message) throws IOException;
}