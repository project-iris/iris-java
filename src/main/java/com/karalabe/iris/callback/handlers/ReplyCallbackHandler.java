package com.karalabe.iris.callback.handlers;

import com.karalabe.iris.callback.CallbackHandler;
import com.karalabe.iris.callback.InstanceCallbackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

@FunctionalInterface
public interface ReplyCallbackHandler extends InstanceCallbackHandler, CallbackHandler {
    /* Handles a request (message), returning the reply that should be forwarded back to the caller.
     * If the method crashes, nothing is returned and the caller will eventually time out.*/
    @NotNull byte[] handleEvent(long requestId, @Nullable byte[] reply) throws IOException;
}