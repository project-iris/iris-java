package com.karalabe.iris;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@SuppressWarnings({"resource", "JUnitTestNG", "ProhibitedExceptionDeclared", "UnqualifiedStaticUsage", "InstanceMethodNamingConvention"})
public class ProtocolBaseTest {
    @Test public void bytesAreSentAndReceivedCorrectly() throws Exception {
        testProtocol(protocol -> {
            for (final byte sentValue : Arrays.asList(Byte.MIN_VALUE, (byte) 0, Byte.MAX_VALUE)) {
                protocol.sendByte(sentValue);
                Assert.assertEquals(sentValue, protocol.recvByte());
            }
        });
    }

    @Test public void booleansAreSentAndReceivedCorrectly() throws Exception {
        testProtocol(protocol -> {
            for (final boolean sentValue : Arrays.asList(true, false)) {
                protocol.sendBoolean(sentValue);
                Assert.assertEquals(sentValue, protocol.recvBoolean());
            }
        });
    }

    @Test public void varintsAreSentAndReceivedCorrectly() throws Exception {
        testProtocol(protocol -> {
            for (final long sentValue : Arrays.asList(/*Long.MIN_VALUE, (long) Integer.MIN_VALUE, (long) Short.MIN_VALUE, (long) Byte.MIN_VALUE, -1L,*/ 0L, 127L, 128L, 2560L, 1894L, 3141592653L, (long) Byte.MAX_VALUE, (long) Short.MAX_VALUE, (long) Integer.MAX_VALUE, Long.MAX_VALUE)) {
                protocol.sendVarint(sentValue);
                Assert.assertEquals(sentValue, protocol.recvVarint());
            }
        });
    }

    @Test public void binaryDataAreSentAndReceivedCorrectly() throws Exception {
        testProtocol(protocol -> {
            for (final String sentValue : Arrays.asList("", "a", "abcdefg", Arrays.toString(new Exception().getStackTrace()).substring(0, 1000))) {
                final byte[] bytes = sentValue.getBytes(StandardCharsets.UTF_8);
                protocol.sendBinary(bytes);
                Assert.assertArrayEquals(bytes, protocol.recvBinary());
            }
        });
    }

    @Test public void stringsAreSentAndReceivedCorrectly() throws Exception {
        testProtocol(protocol -> {
            for (final String sentValue : Arrays.asList("", "a", "abcdefg", Arrays.toString(new Exception().getStackTrace()).substring(0, 1000))) {
                protocol.sendString(sentValue);
                Assert.assertEquals(sentValue, protocol.recvString());
            }
        });
    }

    private static void testProtocol(TestConsumer<ProtocolBase> testConsumer) throws Exception {
        final PipedOutputStream outputStream = new PipedOutputStream();
        final PipedInputStream inputStream = new PipedInputStream(outputStream);

        try (final ProtocolBase protocol = new ProtocolBase(inputStream, outputStream)) {
            testConsumer.accept(protocol);
        }
        catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }
}