// Copyright (c) 2014 Project Iris. All rights reserved.
//
// The current language binding is an official support library of the Iris
// cloud messaging framework, and as such, the same licensing terms apply.
// For details please see http://iris.karalabe.com/downloads#License
package com.karalabe.iris.common;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.atomic.LongAdder;

@SuppressWarnings({"resource", "JUnitTestNG", "ProhibitedExceptionDeclared", "UnqualifiedStaticUsage"})
public class BoundedThreadPoolTest {
    // Tests that simple task scheduling and execution works.
    @Test public void schedule() throws Exception {
        final int THREAD_COUNT = 4, MEMORY_SIZE = 1024, TASK_COUNT = 100;
        final BoundedThreadPool pool = new BoundedThreadPool(THREAD_COUNT, MEMORY_SIZE);

        final LongAdder counter = new LongAdder();
        for (int i = 0; i < TASK_COUNT; i++) {
            Assert.assertTrue(pool.schedule(counter::increment, 0));
        }
        pool.terminate(false);
        Assert.assertEquals(TASK_COUNT, counter.intValue());
    }

    // Tests that memory bounds are complied with.
    @Test public void capacity() throws Exception {
        final int THREAD_COUNT = 1, MEMORY_SIZE = 1;
        final BoundedThreadPool pool = new BoundedThreadPool(THREAD_COUNT, MEMORY_SIZE);

        Assert.assertTrue(pool.schedule(() -> {}, 1));
        Assert.assertFalse(pool.schedule(() -> {}, 2));
    }

    // Tests that scheduled timeouts are complied with.
    @Test public void timeout() throws Exception {
        final int THREAD_COUNT = 1, MEMORY_SIZE = 1024, TASK_COUNT = 10, TIMEOUT = 100;
        final BoundedThreadPool pool = new BoundedThreadPool(THREAD_COUNT, MEMORY_SIZE);

        final LongAdder counter = new LongAdder();
        for (int i = 0; i < TASK_COUNT; i++) {
            Assert.assertTrue(pool.schedule(() -> {
                counter.increment();
                try {
                    Thread.sleep(2 * TIMEOUT);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }, 0, TIMEOUT));
        }
        pool.terminate(false);
        Assert.assertEquals(1, counter.intValue());
    }
}