package com.karalabe.iris;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@SuppressWarnings({"resource", "JUnitTestNG", "ProhibitedExceptionDeclared", "UnqualifiedStaticUsage"})
public class HandshakeTest {
    private static final byte[] MESSAGE_BYTES  = "testMessage".getBytes(StandardCharsets.UTF_8);
    private static final int    TIMEOUT_MILLIS = 10;

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