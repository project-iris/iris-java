package com.karalabe.iris.callback;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public class CallbackHandlerRegistry implements CallbackRegistry {
    private final Cache<Object, StaticCallbackHandler>   staticCallbacks   = CacheBuilder.newBuilder().build();
    private final Cache<Object, InstanceCallbackHandler> instanceCallbacks = CacheBuilder.newBuilder()
                                                                                 .expireAfterWrite(1, TimeUnit.MINUTES) // TODO make configurable
                                                                                 .removalListener(notification -> System.out.printf("Removed %s:%s from cache!%n", notification.getKey(), notification.getValue()))
                                                                                 .build();

    public void addCallbackHandler(@NotNull StaticCallbackHandler callbackHandler) {
        staticCallbacks.put(callbackHandler.getId(), callbackHandler);
    }

    public Long addCallbackHandler(@NotNull InstanceCallbackHandler callbackHandler) {
        final Long id = callbackHandler.getId();
        instanceCallbacks.put(id, callbackHandler);
        return id;
    }

    @NotNull public <T extends CallbackHandler> T useCallbackHandler(@NotNull Object id) throws IllegalArgumentException {
        CallbackHandler callbackHandler = instanceCallbacks.getIfPresent(id);
        if (callbackHandler != null) {
            instanceCallbacks.invalidate(id);
        } else {
            callbackHandler = staticCallbacks.getIfPresent(id);
        }

        if (callbackHandler == null) { throw new IllegalArgumentException(String.format("No handler found for '%s'!", id)); }
        return (T) callbackHandler;
    }
}