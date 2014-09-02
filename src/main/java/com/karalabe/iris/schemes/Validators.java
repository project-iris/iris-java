package com.karalabe.iris.schemes;

import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

public final class Validators {
    private static final Pattern localClusterPattern  = Pattern.compile("[^:]*");
    private static final Pattern remoteClusterPattern = Pattern.compile("([^:]+:)?[^:]+");

    private Validators() {}

    public static void validateLocalClusterName(@NotNull final String cluster) {
        if (!localClusterPattern.matcher(cluster).matches()) {
            throw new IllegalArgumentException("Invalid local cluster name!");
        }
    }

    public static void validateRemoteClusterName(@NotNull final String cluster) {
        if (!remoteClusterPattern.matcher(cluster).matches()) {
            throw new IllegalArgumentException("Invalid remote cluster name!");
        }
    }

    public static void validateTopicName(@NotNull final String topic) {
        if (topic.isEmpty()) { throw new IllegalArgumentException("Empty topic name!"); }
    }
}