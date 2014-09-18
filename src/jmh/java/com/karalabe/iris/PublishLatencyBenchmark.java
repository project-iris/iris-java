// Copyright (c) 2014 Project Iris. All rights reserved.
//
// The current language binding is an official support library of the Iris
// cloud messaging framework, and as such, the same licensing terms apply.
// For details please see http://iris.karalabe.com/downloads#License
package com.karalabe.iris;

import com.karalabe.iris.exceptions.ClosedException;
import com.karalabe.iris.exceptions.InitializationException;
import org.openjdk.jmh.annotations.*;

import java.io.IOException;
import java.util.concurrent.Semaphore;

// Benchmarks publishing a single message.
@State(Scope.Thread)
public class PublishLatencyBenchmark {
    static class BenchmarkHandler implements TopicHandler {
        Semaphore pending;

        @Override public void handleEvent(final byte[] event) {
            pending.release();
        }
    }

    private Connection       connection = null;
    private BenchmarkHandler handler    = null;

    // Connects to the relay and subscribes to a topic.
    @Setup(Level.Iteration) public void init() throws InterruptedException, IOException, InitializationException, ClosedException {
        handler = new BenchmarkHandler();
        handler.pending = new Semaphore(0);

        connection = new Connection(BenchmarkConfigs.RELAY_PORT);
        connection.subscribe(BenchmarkConfigs.TOPIC_NAME, handler);
        Thread.sleep(100);
    }

    // Unsubscribes from the topic and closes the connection.
    @TearDown(Level.Iteration) public void close() throws IOException, InterruptedException, ClosedException {
        connection.unsubscribe(BenchmarkConfigs.TOPIC_NAME);
        connection.close();
        Thread.sleep(100);
    }

    // Benchmarks publishing a single message.
    @Benchmark public void timeLatency() throws InterruptedException, IOException, ClosedException {
        connection.publish(BenchmarkConfigs.TOPIC_NAME, new byte[]{0x00});
        handler.pending.acquire();
    }
}
