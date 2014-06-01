package com.karalabe.iris;

import org.jetbrains.annotations.NotNull;

public interface TopicHandler {
    default void handleEvent(@NotNull final byte[] event) { }
}
