// Copyright (c) 2014 Project Iris. All rights reserved.
//
// The current language binding is an official support library of the Iris
// cloud messaging framework, and as such, the same licensing terms apply.
// For details please see http://iris.karalabe.com/downloads#License
package com.karalabe.iris.common;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

// A thread pool with a fixed number of workers and bounded memory usage, after which new tasks are
// dropped until previously scheduled ones complete.
public class BoundedThreadPool {
    private final ExecutorService workers;
    private final Semaphore       capacity;

    public BoundedThreadPool(int threads, int bounds) {
        workers = Executors.newFixedThreadPool(threads);
        capacity = new Semaphore(bounds);
    }

    // Schedules a new task into the thread pool if the required memory capacity is available or
    // returns the approximately available space (note, race since not synced).
    public boolean schedule(Runnable task, int size) {
        if (!capacity.tryAcquire(size)) {
            return false;
        }
        workers.submit(() -> {
            capacity.release(size);
            task.run();
        });
        return true;
    }

    // Terminates the thread pool, either cleaning all pending tasks or waiting for them to complete.
    public void terminate(boolean clean) throws InterruptedException {
        if (clean) {
            workers.shutdownNow();
        } else {
            workers.shutdown();
            workers.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        }
    }
}
