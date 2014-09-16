// Copyright (c) 2014 Project Iris. All rights reserved.
//
// The current language binding is an official support library of the Iris
// cloud messaging framework, and as such, the same licensing terms apply.
// For details please see http://iris.karalabe.com/downloads#License
package com.karalabe.iris;

import com.karalabe.iris.exceptions.InitializationException;
import org.openjdk.jmh.annotations.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

// Benchmarks publishing a batch of messages.
@State(Scope.Thread)
public class PublishThroughputBenchmark {
    static class BenchmarkHandler implements TopicHandler {
        Semaphore pending;

        @Override public void handleEvent(final byte[] event) {
            pending.release();
        }
    }

    @Param({"1", "2", "4", "8", "16", "32", "64", "128"})
    public int threads;

    private final int ITERATIONS = 50000;

    private Connection       connection = null;
    private BenchmarkHandler handler    = null;

    private ExecutorService              workers = null;
    private ArrayList<Callable<Integer>> tasks   = null;

    // Initializes the benchmark.
    @Setup(Level.Iteration) public void init() throws InterruptedException, IOException, InitializationException {
        // Connect to the relay and subscribe to a topic
        handler = new BenchmarkHandler();
        handler.pending = new Semaphore(0);

        connection = Iris.connect(BenchmarkConfigs.RELAY_PORT);
        connection.subscribe(BenchmarkConfigs.TOPIC_NAME, handler);
        Thread.sleep(100);

        // Assemble the tasks-set to benchmark
        workers = Executors.newFixedThreadPool(threads);
        tasks = new ArrayList<>(ITERATIONS);
        for (int i = 0; i < ITERATIONS; i++) {
            tasks.add(i, () -> {
                connection.publish(BenchmarkConfigs.TOPIC_NAME, new byte[]{0x00});
                return 0;
            });
        }
    }

    // Terminates the workers, unsubscribes and closes the connection.
    @TearDown(Level.Iteration) public void close() throws IOException, InterruptedException {
        workers.shutdown();
        connection.unsubscribe(BenchmarkConfigs.TOPIC_NAME);
        connection.close();
        Thread.sleep(100);
    }

    // Benchmarks publishing a batch of messages.
    @Benchmark @OperationsPerInvocation(ITERATIONS) public void timeThroughput() throws InterruptedException, IOException {
        workers.invokeAll(tasks);
        handler.pending.acquire(ITERATIONS);
    }
}
