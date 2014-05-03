package com.karalabe.iris;

import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

@SuppressWarnings({"resource", "JUnitTestNG", "ProhibitedExceptionDeclared", "UnqualifiedStaticUsage"})
public class RelayTest {
    public static final int IRIS_PORT = 55555;

    @Nullable private static Socket getConnection() {
        try (final Socket connection = new Socket(InetAddress.getLoopbackAddress(), IRIS_PORT)) {
            return connection;
        }
        catch (IOException ignored) {
            return null;
        }
    }

    @Test public void connectIsWorking() throws Exception {
        Assert.assertNotNull(getConnection());
    }

    @Test public void handshakeIsWorking() throws Exception {
        Assert.assertNotNull(getConnection());
    }
}