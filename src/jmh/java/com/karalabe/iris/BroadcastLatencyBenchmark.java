// Copyright (c) 2014 Project Iris. All rights reserved.
//
// The current language binding is an official support library of the Iris
// cloud messaging framework, and as such, the same licensing terms apply.
// For details please see http://iris.karalabe.com/downloads#License
package com.karalabe.iris;

import com.karalabe.iris.exceptions.InitializationException;
import org.jetbrains.annotations.NotNull;
import org.openjdk.jmh.annotations.*;

import java.io.IOException;
import java.lang.InterruptedException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Semaphore;

// Benchmarks broadcasting a single message.
@State(Scope.Thread)
public class BroadcastLatencyBenchmark {
    private class BenchmarkHandler implements ServiceHandler {
        final Set<String> arrived = Collections.synchronizedSet(new HashSet<>());
        Connection connection;
        Semaphore  pending;

        @Override public void init(@NotNull final Connection connection) {
            this.connection = connection;
        }

        @Override public void handleBroadcast(@NotNull final byte[] message) {
            arrived.add(new String(message, StandardCharsets.UTF_8));
            pending.release(1);
        }
    }

    private BenchmarkHandler handler = null;
    private Service          service = null;

    // Registers a new service to the relay.
    @Setup public void init() throws InterruptedException, IOException, InitializationException {
        handler = new BenchmarkHandler();
        handler.pending = new Semaphore(0);

        service = Iris.register(BenchmarkConfigs.RELAY_PORT, BenchmarkConfigs.CLUSTER_NAME, handler);
    }

    // Unregisters the service.
    @TearDown public void close() throws IOException, InterruptedException {
        service.close();

    }

    // Benchmarks broadcasting a single message.
    @Benchmark public void timeLatency() throws InterruptedException, IOException {
        handler.connection.broadcast(BenchmarkConfigs.CLUSTER_NAME, new byte[]{0x00});
        handler.pending.acquire(1);

    }
}
