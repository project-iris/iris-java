// Copyright (c) 2014 Project Iris. All rights reserved.
//
// The current language binding is an official support library of the Iris
// cloud messaging framework, and as such, the same licensing terms apply.
// For details please see http://iris.karalabe.com/downloads#License
package com.karalabe.iris;

import com.karalabe.iris.exceptions.ClosedException;
import com.karalabe.iris.exceptions.InitializationException;
import com.karalabe.iris.exceptions.TimeoutException;
import org.openjdk.jmh.annotations.*;

import java.io.IOException;
import java.util.concurrent.Semaphore;

// Benchmarks the latency of the tunnel data transfer.
@State(Scope.Thread)
public class TunnelLatencyBenchmark {
    private class BenchmarkHandler implements ServiceHandler {
        Connection connection;
        Semaphore  pending;

        @Override public void init(final Connection connection) {
            this.connection = connection;
        }

        @Override public void handleTunnel(final Tunnel tunnel) {
            try {
                while (true) {
                    tunnel.receive();
                    pending.release();
                }
            } catch (IOException | ClosedException ignored) {
                // Tunnel was torn down, clean up
            } finally {
                try {
                    tunnel.close();
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private BenchmarkHandler handler = null;
    private Service          service = null;
    private Tunnel           tunnel  = null;

    // Registers a new service to the relay and opens a tunnel into it
    @Setup(Level.Iteration) public void init() throws InterruptedException, IOException, InitializationException, TimeoutException {
        handler = new BenchmarkHandler();
        handler.pending = new Semaphore(0);

        service = new Service(BenchmarkConfigs.RELAY_PORT, BenchmarkConfigs.CLUSTER_NAME, handler);
        tunnel = handler.connection.tunnel(BenchmarkConfigs.CLUSTER_NAME, 1000);
    }

    // Closes the tunnel and unregisters the service.
    @TearDown(Level.Iteration) public void close() throws IOException, InterruptedException {
        tunnel.close();
        service.close();
    }

    // Benchmarks the latency of the tunnel data transfer.
    @Benchmark public void timeLatency() throws ClosedException, IOException, InterruptedException {
        tunnel.send(new byte[]{0x00});
        handler.pending.acquire();
    }
}
