// Copyright (c) 2014 Project Iris. All rights reserved.
//
// The current language binding is an official support library of the Iris
// cloud messaging framework, and as such, the same licensing terms apply.
// For details please see http://iris.karalabe.com/downloads#License
package com.karalabe.iris;

import com.karalabe.iris.exceptions.InitializationException;
import org.openjdk.jmh.annotations.*;

import java.io.IOException;
import java.util.concurrent.Semaphore;

// Benchmarks broadcasting a single message.
@State(Scope.Thread)
public class BroadcastLatencyBenchmark {
    private class BenchmarkHandler implements ServiceHandler {
        Connection connection;
        Semaphore  pending;

        @Override public void init(final Connection connection) {
            this.connection = connection;
        }

        @Override public void handleBroadcast(final byte[] message) {
            pending.release();
        }
    }

    private BenchmarkHandler handler = null;
    private Service          service = null;

    // Registers a new service to the relay.
    @Setup(Level.Iteration) public void init() throws InterruptedException, IOException, InitializationException {
        handler = new BenchmarkHandler();
        handler.pending = new Semaphore(0);

        service = new Service(BenchmarkConfigs.RELAY_PORT, BenchmarkConfigs.CLUSTER_NAME, handler);
    }

    // Unregisters the service.
    @TearDown(Level.Iteration) public void close() throws IOException, InterruptedException {
        service.close();
    }

    // Benchmarks broadcasting a single message.
    @Benchmark public void timeLatency() throws InterruptedException, IOException {
        handler.connection.broadcast(BenchmarkConfigs.CLUSTER_NAME, new byte[]{0x00});
        handler.pending.acquire();
    }
}
