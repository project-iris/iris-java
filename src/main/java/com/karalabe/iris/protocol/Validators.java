/*
 * Copyright © 2014 Project Iris. All rights reserved.
 *
 * The current language binding is an official support library of the Iris cloud messaging framework, and as such, the same licensing terms apply.
 * For details please see http://iris.karalabe.com/downloads#License
 */

package com.karalabe.iris.protocol;

import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

public final class Validators {
    private static final Pattern LOCAL_CLUSTER_PATTERN  = Pattern.compile("[^:]*");
    private static final Pattern REMOTE_CLUSTER_PATTERN = Pattern.compile("([^:]+:)?[^:]+");

    private Validators() {}

    public static void validateLocalClusterName(@NotNull final CharSequence cluster) {
        if (!LOCAL_CLUSTER_PATTERN.matcher(cluster).matches()) {
            throw new IllegalArgumentException("Invalid local cluster name!");
        }
    }

    public static void validateRemoteClusterName(@NotNull final CharSequence cluster) {
        if (!REMOTE_CLUSTER_PATTERN.matcher(cluster).matches()) {
            throw new IllegalArgumentException("Invalid remote cluster name!");
        }
    }

    public static void validateTopicName(@NotNull final String topic) {
        if (topic.isEmpty()) { throw new IllegalArgumentException("Empty topic name!"); }
    }
}