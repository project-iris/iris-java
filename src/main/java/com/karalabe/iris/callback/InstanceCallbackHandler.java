package com.karalabe.iris.callback;

import java.util.concurrent.atomic.AtomicLong;

public interface InstanceCallbackHandler extends CallbackHandler {
    AtomicLong LAST_ID = new AtomicLong();

    @Override default Long getId() {
        return LAST_ID.getAndIncrement();
    }
}