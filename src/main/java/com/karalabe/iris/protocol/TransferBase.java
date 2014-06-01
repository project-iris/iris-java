package com.karalabe.iris.protocol;

import com.karalabe.iris.callback.CallbackHandlerRegistry;
import com.karalabe.iris.callback.InstanceCallbackHandler;
import org.jetbrains.annotations.NotNull;

public class TransferBase {
    protected final ProtocolBase            protocol;
    protected final CallbackHandlerRegistry callbacks;

    public TransferBase(@NotNull final ProtocolBase protocol, @NotNull final CallbackHandlerRegistry callbacks) {

        this.protocol = protocol;
        this.callbacks = callbacks;
    }

    protected Long addCallbackHandler(@NotNull final InstanceCallbackHandler callbackHandler) {
        return callbacks.addCallbackHandler(callbackHandler);
    }
}