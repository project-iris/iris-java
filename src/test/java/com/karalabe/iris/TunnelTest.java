// Copyright (c) 2014 Project Iris. All rights reserved.
//
// The current language binding is an official support library of the Iris
// cloud messaging framework, and as such, the same licensing terms apply.
// For details please see http://iris.karalabe.com/downloads#License
package com.karalabe.iris;

import com.carrotsearch.junitbenchmarks.AbstractBenchmark;
import com.carrotsearch.junitbenchmarks.BenchmarkOptions;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("JUnitTestNG")
public class TunnelTest extends AbstractBenchmark {
    // Service handler for the tunnel tests.
    static class TunnelTestHandler implements ServiceHandler {
        Connection connection;

        @Override public void init(@NotNull final Connection connection) {
            this.connection = connection;
        }

        @Override public void handleTunnel(@NotNull final Tunnel tunnel) {
            try {
                tunnel.close();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @BenchmarkOptions(benchmarkRounds = 5, warmupRounds = 10)
    @Test public void construction() throws Exception {
        final TunnelTestHandler handler = new TunnelTestHandler();

        try (final Service ignored = Iris.register(Config.RELAY_PORT, Config.CLUSTER_NAME, handler)) {
            try (final Tunnel tunnel = handler.connection.tunnel(Config.CLUSTER_NAME, 1000)) {
                tunnel.close();
            }
            catch (Exception e) {
                Assert.fail(e.getMessage());
            }
        }
    }
}