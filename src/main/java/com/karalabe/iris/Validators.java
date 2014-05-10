package com.karalabe.iris;

import org.jetbrains.annotations.NotNull;

final class Validators {
    private Validators() {}

    public static void validateClusterName(@NotNull final String clusterName) {
        if (clusterName.isEmpty()) { throw new IllegalArgumentException("Empty cluster name!"); }
    }

    public static void validateTopic(@NotNull final String topic) {
        if (topic.isEmpty()) { throw new IllegalArgumentException("Empty topic name!"); }
    }

    public static void validateMessage(@NotNull final byte[] message) {
        if (message.length == 0) { throw new IllegalArgumentException("Empty message!"); }
    }
}