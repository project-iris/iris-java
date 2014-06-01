// Copyright (c) 2014 Project Iris. All rights reserved.
//
// The current language binding is an official support library of the Iris
// cloud messaging framework, and as such, the same licensing terms apply.
// For details please see http://iris.karalabe.com/downloads#License
package com.karalabe.iris.common;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings({"resource", "JUnitTestNG", "ProhibitedExceptionDeclared", "UnqualifiedStaticUsage"})
public class BoundedThreadPoolTest {
    // Tests that simple task scheduling and execution works.
    @Test public void schedule() throws Exception {
        final int THREADS = 4, MEMORY = 1024, TASKS = 100;
        BoundedThreadPool pool = new BoundedThreadPool(THREADS, MEMORY);

        final AtomicInteger counter = new AtomicInteger();
        for (int i = 0; i < TASKS; i++) {
            Assert.assertTrue(pool.schedule(() -> {
                counter.addAndGet(1);
            }, 0));
        }
        pool.terminate(false);
        Assert.assertEquals(TASKS, counter.get());
    }

    // Tests that memory bounds are complied with.
    @Test public void capacity() throws Exception {
        final int THREADS = 1, MEMORY = 1;
        BoundedThreadPool pool = new BoundedThreadPool(THREADS, MEMORY);

        Assert.assertTrue(pool.schedule(() -> {}, 1));
        Assert.assertFalse(pool.schedule(() -> {}, 2));
    }

    // Tests that scheduled timeouts are complied with.
    @Test public void timeout() throws Exception {
        final int THREADS = 1, MEMORY = 1024, TASKS = 10, TIMEOUT = 100;
        BoundedThreadPool pool = new BoundedThreadPool(THREADS, MEMORY);

        final AtomicInteger counter = new AtomicInteger();
        for (int i=0; i<TASKS; i++) {
            Assert.assertTrue(pool.schedule(() -> {
                counter.addAndGet(1);
                try {
                    Thread.sleep(2 * TIMEOUT);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }, 0, TIMEOUT));
        }
        pool.terminate(false);
        Assert.assertEquals(1, counter.get());
    }
}