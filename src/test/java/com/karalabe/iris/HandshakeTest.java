package com.karalabe.iris;

import com.carrotsearch.junitbenchmarks.AbstractBenchmark;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

@SuppressWarnings({"resource", "JUnitTestNG", "ProhibitedExceptionDeclared", "UnqualifiedStaticUsage"})
public class HandshakeTest extends AbstractBenchmark {
    @Test public void connection() throws Exception {
        try (final Connection ignored = new Connection(Config.RELAY_PORT)) {
        }
        catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test public void service() throws Exception {
        try (final Service ignored = new Service(Config.RELAY_PORT, Config.CLUSTER_NAME, new ServiceHandler() {})) {
        }
        catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }
}