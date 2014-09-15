// Copyright (c) 2014 Project Iris. All rights reserved.
//
// The current language binding is an official support library of the Iris
// cloud messaging framework, and as such, the same licensing terms apply.
// For details please see http://iris.karalabe.com/downloads#License
package com.karalabe.iris.protocol;

import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

// Validators ensuring names and data blobs conform to the protocol.
public final class Validators {
    private static final Pattern CLUSTER_NAME_PATTERN    = Pattern.compile("[^:]*");
    private static final Pattern CLUSTER_ADDRESS_PATTERN = Pattern.compile("([^:]+:)?[^:]+");
    private static final Pattern TOPIC_NAME_PATTERN      = Pattern.compile("[^:]*");

    private Validators() {}

    public static void validateClusterName(@NotNull final String cluster) {
        if (!CLUSTER_NAME_PATTERN.matcher(cluster).matches()) {
            throw new IllegalArgumentException("Cluster names must not contain the scoping operator ':'");
        }
    }

    public static void validateClusterAddress(@NotNull final String cluster) {
        if (!CLUSTER_ADDRESS_PATTERN.matcher(cluster).matches()) {
            throw new IllegalArgumentException("Cluster addresses may contain a maximum of one scoping operator ':'");
        }
    }

    public static void validateTopicName(@NotNull final String topic) {
        if (!TOPIC_NAME_PATTERN.matcher(topic).matches()) {
            throw new IllegalArgumentException("Topic names must not contain the scoping operator ':'");
        }
    }

    public static void validateBroadcastPayload(@NotNull final byte[] message) {
        if (message.length == 0) {
            throw new IllegalArgumentException("Broadcast payload must not be empty");
        }
    }

    public static void validateRequestPayload(@NotNull final byte[] request) {
        if (request.length == 0) {
            throw new IllegalArgumentException("Request payload must not be empty");
        }
    }

    public static void validatePublishPayload(@NotNull final byte[] event) {
        if (event.length == 0) {
            throw new IllegalArgumentException("Published event payload must not be empty");
        }
    }

    public static void validateTunnelPayload(@NotNull final byte[] data) {
        if (data.length == 0) {
            throw new IllegalArgumentException("Tunnel payload must not be empty");
        }
    }
}