package com.karalabe.iris.protocol;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

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

    private static void testProtocol(TestConsumer<ProtocolBase> testConsumer) throws Exception {
        // Create a simple echo server (i.e. loop output stream back to input stream)
        final ServerSocket server = new ServerSocket(0);
        new Thread(() -> {
            try {
                Socket socket = server.accept();
                byte[] buffer = new byte[256];
                int len = 0;

                while ((len = socket.getInputStream().read(buffer)) >= 0) {
                    socket.getOutputStream().write(buffer, 0, len);
                    socket.getOutputStream().flush();
                }
                socket.close();
                server.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        // Execute the requested test
        try (final ProtocolBase protocol = new ProtocolBase(server.getLocalPort())) {
            testConsumer.accept(protocol);
        }
        catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    @FunctionalInterface
    public interface TestConsumer<T> {
        void accept(T t) throws Exception;
    }
}