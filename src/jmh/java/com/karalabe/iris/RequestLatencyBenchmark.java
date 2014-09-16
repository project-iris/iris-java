// Copyright (c) 2014 Project Iris. All rights reserved.
//
// The current language binding is an official support library of the Iris
// cloud messaging framework, and as such, the same licensing terms apply.
// For details please see http://iris.karalabe.com/downloads#License
package com.karalabe.iris;

import com.karalabe.iris.exceptions.InitializationException;
import com.karalabe.iris.exceptions.RemoteException;
import com.karalabe.iris.exceptions.TimeoutException;
import org.openjdk.jmh.annotations.*;

import java.io.IOException;

// Benchmarks the latency of a single request/reply operation.
@State(Scope.Thread)
public class RequestLatencyBenchmark {
    private class BenchmarkHandler implements ServiceHandler {
        Connection connection;

        @Override public void init(final Connection connection) {
            this.connection = connection;
        }

        @Override public byte[] handleRequest(final byte[] request) {
            return request;
        }
    }

    private BenchmarkHandler handler = null;
    private Service          service = null;

    // Registers a new service to the relay.
    @Setup(Level.Iteration) public void init() throws InterruptedException, IOException, InitializationException {
        handler = new BenchmarkHandler();
        service = Iris.register(BenchmarkConfigs.RELAY_PORT, BenchmarkConfigs.CLUSTER_NAME, handler);
    }

    // Unregisters the service.
    @TearDown(Level.Iteration) public void close() throws IOException, InterruptedException {
        service.close();
    }

    // Benchmarks the latency of a single request/reply operation.
    @Benchmark public void timeLatency() throws InterruptedException, IOException, TimeoutException, RemoteException {
        handler.connection.request(BenchmarkConfigs.CLUSTER_NAME, new byte[]{0x00}, 1000);
    }
}
