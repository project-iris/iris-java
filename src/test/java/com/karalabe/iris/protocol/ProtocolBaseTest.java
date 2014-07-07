/*
 * Copyright © 2014 Project Iris. All rights reserved.
 *
 * The current language binding is an official support library of the Iris cloud messaging framework, and as such, the same licensing terms apply.
 * For details please see http://iris.karalabe.com/downloads#License
 */

package com.karalabe.iris.protocol;

import com.karalabe.iris.ProtocolException;
import org.junit.Assert;
import org.junit.Test;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.function.Consumer;

@SuppressWarnings({"resource", "JUnitTestNG", "ProhibitedExceptionDeclared", "UnqualifiedStaticUsage", "InstanceMethodNamingConvention"})
public class ProtocolBaseTest {
    @Test public void bytesAreSentAndReceivedCorrectly() throws Exception {
        testProtocol(protocol -> {
            for (final byte sentValue : Arrays.asList(Byte.MIN_VALUE, (byte) 0, Byte.MAX_VALUE)) {
                protocol.sendByte(sentValue);
                protocol.flush();
                Assert.assertEquals(sentValue, protocol.receiveByte());
            }
        });
    }

    @Test public void booleansAreSentAndReceivedCorrectly() throws Exception {
        testProtocol(protocol -> {
            for (final boolean sentValue : Arrays.asList(true, false)) {
                protocol.sendBoolean(sentValue);
                protocol.flush();
                Assert.assertEquals(sentValue, protocol.receiveBoolean());
            }
        });
    }

    @Test public void varintsAreSentAndReceivedCorrectly() throws Exception {
        testProtocol(protocol -> {
            for (final long sentValue : Arrays.asList(0L, 127L, 128L, 2560L, 1894L, 3141592653L, (long) Byte.MAX_VALUE, (long) Short.MAX_VALUE, (long) Integer.MAX_VALUE/*, Long.MAX_VALUE*/)) {
                protocol.sendVarint(sentValue);
                protocol.flush();
                Assert.assertEquals(sentValue, protocol.receiveVarint());
            }
        });
    }

    @Test public void binaryDataAreSentAndReceivedCorrectly() throws Exception {
        testProtocol(protocol -> {
            for (final String sentValue : Arrays.asList("", "a", "abcdefg", Arrays.toString(new Exception().getStackTrace()).substring(0, 1000))) {
                final byte[] bytes = sentValue.getBytes(StandardCharsets.UTF_8);
                protocol.sendBinary(bytes);
                protocol.flush();
                Assert.assertArrayEquals(bytes, protocol.receiveBinary());
            }
        });
    }

    @Test public void stringsAreSentAndReceivedCorrectly() throws Exception {
        testProtocol(protocol -> {
            for (final String sentValue : Arrays.asList("", "a", "abcdefg", Arrays.toString(new Exception().getStackTrace()).substring(0, 1000))) {
                protocol.sendString(sentValue);
                protocol.flush();
                Assert.assertEquals(sentValue, protocol.receiveString());
            }
        });
    }

    private static void testProtocol(Consumer<ProtocolBase> consumer) throws Exception {
        // Create a simple echo server (i.e. loop output stream back to input stream)
        try (final ServerSocket server = new ServerSocket(0)) {
            new Thread(() -> {
                try (Socket socket = server.accept()) {
                    byte[] buffer = new byte[256];
                    int len = 0;

                    final InputStream in = socket.getInputStream();
                    final OutputStream out = socket.getOutputStream();

                    while ((len = in.read(buffer)) >= 0) {
                        out.write(buffer, 0, len);
                        out.flush();
                    }
                } catch (Exception e) { throw new ProtocolException(e); }
            }).start();

            // Execute the requested test
            try (final ProtocolBase protocol = new ProtocolBase(server.getLocalPort())) {
                consumer.accept(protocol);
            }
        }
    }
}