/*
 * Copyright © 2014 Project Iris. All rights reserved.
 *
 * The current language binding is an official support library of the Iris cloud messaging framework, and as such, the same licensing terms apply.
 * For details please see http://iris.karalabe.com/downloads#License
 */

package com.karalabe.iris.common;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/** A thread pool with a fixed number of workers and bounded memory usage, after which new tasks are dropped until previously scheduled ones complete. */
public class BoundedThreadPool {
    private final ExecutorService executor;
    private final Semaphore       capacity;

    public BoundedThreadPool(int threadCount, int bounds) {
        executor = Executors.newFixedThreadPool(threadCount);
        capacity = new Semaphore(bounds);
    }

    /** Schedules a new task into the thread pool if the required memory capacity is available. */
    public boolean schedule(int size, Runnable task) {
        if (!capacity.tryAcquire(size)) { return false; }

        executor.submit(() -> {
            capacity.release(size);
            task.run();
        });
        return true;
    }

    /**
     * Schedules a new task into the thread pool if the required memory capacity is available.
     * The additional timeout is used to ensure that expired tasks get dropped instead of executed.
     */
    public boolean schedule(int size, int timeout, Runnable task) {
        final long start = System.nanoTime();
        return schedule(size, () -> {
            if (((System.nanoTime() - start) / 1_000_000) < timeout) {
                task.run();
            }
        });
    }

    public enum Terminate {
        NOW,
        AWAIT
    }

    /** Terminates the thread pool, either cleaning all pending tasks or waiting for them to complete. */
    public void terminate(Terminate terminate) throws InterruptedException {
        if (terminate == Terminate.NOW) {
            executor.shutdownNow();
        } else {
            executor.shutdown();
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        }
    }
}
