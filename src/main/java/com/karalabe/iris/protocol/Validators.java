package com.karalabe.iris.protocol;

import org.jetbrains.annotations.NotNull;

public final class Validators {

    public static final String FEDERATION_SEPARATOR = ":";

    private Validators() {}

    public static void validateLocalClusterName(@NotNull final String clusterName) {
        if (clusterName.contains(FEDERATION_SEPARATOR)) {
            throw new IllegalArgumentException("Service name cannot contain colon!");
        }
    }

    public static void validateRemoteClusterName(@NotNull final String clusterName) {
        if (clusterName.isEmpty()) {
            throw new IllegalArgumentException("Empty cluster name!");
        }
    }

    public static void validateTopic(@NotNull final String topic) {
        if (topic.isEmpty()) { throw new IllegalArgumentException("Empty topic name!"); }
    }

    public static void validateMessage(@NotNull final byte[] message) {
        if (message.length == 0) { throw new IllegalArgumentException("Empty message!"); }
    }
}