// Copyright (c) 2014 Project Iris. All rights reserved.
//
// The current language binding is an official support library of the Iris
// cloud messaging framework, and as such, the same licensing terms apply.
// For details please see http://iris.karalabe.com/downloads#License
package com.karalabe.iris;

import com.carrotsearch.junitbenchmarks.AbstractBenchmark;
import com.carrotsearch.junitbenchmarks.BenchmarkOptions;
import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings({"resource", "JUnitTestNG", "ProhibitedExceptionDeclared", "UnqualifiedStaticUsage"})
public class HandshakeTest extends AbstractBenchmark {
    @BenchmarkOptions(benchmarkRounds = 5, warmupRounds = 10)
    @Test public void connection() throws Exception {
        try (final Connection ignored = new Connection(TestConfigs.RELAY_PORT)) {
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @BenchmarkOptions(benchmarkRounds = 5, warmupRounds = 10)
    @Test public void service() throws Exception {
        try (final Service ignored = new Service(TestConfigs.RELAY_PORT, TestConfigs.CLUSTER_NAME, new ServiceHandler() { })) {
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }
}