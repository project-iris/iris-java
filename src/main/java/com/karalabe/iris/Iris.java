// Copyright (c) 2014 Project Iris. All rights reserved.
//
// The current language binding is an official support library of the Iris
// cloud messaging framework, and as such, the same licensing terms apply.
// For details please see http://iris.karalabe.com/downloads#License
package com.karalabe.iris;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public final class Iris {
    private Iris() {}

    // Connects to the Iris network as a simple client.
    public static Connection connect(final int port) throws IOException {
        return new Connection(port, "", null, null);
    }

    // Connects to the Iris network and registers a new service instance as a member of the
    // specified service cluster.
    public static Service register(final int port, @NotNull final String cluster, @NotNull final ServiceHandler handler) throws IOException, InterruptedException {
        return new Service(port, cluster, handler, null);
    }

    // Connects to the Iris network and registers a new service instance as a member of the
    // specified service cluster, overriding the default quality of service limits.
    public static Service register(final int port, @NotNull final String cluster, @NotNull final ServiceHandler handler, @NotNull final ServiceLimits limits) throws IOException, InterruptedException {
        return new Service(port, cluster, handler, limits);
    }
}
