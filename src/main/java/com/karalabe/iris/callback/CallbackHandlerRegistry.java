package com.karalabe.iris.callback;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class CallbackHandlerRegistry implements CallbackRegistry {
    private final Map<Object, StaticCallbackHandler>   staticCallbacks   = new HashMap<>();
    private final Map<Object, InstanceCallbackHandler> instanceCallbacks = new HashMap<>();

    public void addCallbackHandler(@NotNull StaticCallbackHandler callbackHandler) {
        staticCallbacks.put(callbackHandler.getId(), callbackHandler);
    }

    public Long addCallbackHandler(@NotNull InstanceCallbackHandler callbackHandler) {
        final Long id = callbackHandler.getId();
        instanceCallbacks.put(id, callbackHandler);
        return id;
    }

    @NotNull public <T extends CallbackHandler> T useCallbackHandler(@NotNull Object id) throws IllegalArgumentException {
        CallbackHandler callbackHandler = instanceCallbacks.get(id);
        if (callbackHandler != null) {
            instanceCallbacks.remove(id);
        } else {
            callbackHandler = staticCallbacks.get(id);
        }

        if (callbackHandler == null) { throw new IllegalArgumentException(String.format("No handler found for '%s'!", id)); }
        return (T) callbackHandler;
    }
}