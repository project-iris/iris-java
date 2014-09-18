// Copyright (c) 2014 Project Iris. All rights reserved.
//
// The current language binding is an official support library of the Iris
// cloud messaging framework, and as such, the same licensing terms apply.
// For details please see http://iris.karalabe.com/downloads#License
package com.kalralabe.iris;

import com.carrotsearch.junitbenchmarks.AbstractBenchmark;
import com.carrotsearch.junitbenchmarks.BenchmarkOptions;
import com.karalabe.iris.BaseServiceHandler;
import com.karalabe.iris.Service;
import com.karalabe.iris.TestConfigs;
import com.karalabe.iris.Tunnel;
import com.karalabe.iris.exceptions.ClosedException;
import com.karalabe.iris.exceptions.TimeoutException;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@SuppressWarnings({"JUnitTestNG", "ProhibitedExceptionDeclared"})
public class TunnelInterruptTest extends AbstractBenchmark {
    // Handler accepting a tunnel and closing it after a while.
    static class TunnelTestInterruptHandler extends BaseServiceHandler {
        final Semaphore active = new Semaphore(0);
        final Semaphore close  = new Semaphore(0);
        final Semaphore done   = new Semaphore(0);
        final int sleep;

        TunnelTestInterruptHandler(int sleep) { this.sleep = sleep; }

        @Override public void handleTunnel(final Tunnel tunnel) {
            // Signal the remote side of tunnel construction
            active.release();

            // Wait for test completion
            try {
                close.tryAcquire(sleep, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ignored) {
            } finally {
                try {
                    tunnel.close();
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    done.release();
                }
            }
        }
    }

    // Tests that locally closing a tunnel indeed interrupts pending receives.
    @BenchmarkOptions(benchmarkRounds = 5, warmupRounds = 10)
    @Test public void interruptLocalReceive() throws Exception {
        final int SLEEP = 500;

        // Create the service handler and register to the relay
        final TunnelTestInterruptHandler handler = new TunnelTestInterruptHandler(SLEEP);
        try (final Service ignored = new Service(TestConfigs.RELAY_PORT, TestConfigs.CLUSTER_NAME, handler)) {
            // Initiate tunnel construction and wait for remote activation
            final Tunnel tunnel = handler.connection.tunnel(TestConfigs.CLUSTER_NAME, 1000);
            handler.active.acquire();

            // Start a receive operation and close the tunnel in the mean while
            final Semaphore done = new Semaphore(0);
            new Thread(() -> {
                try {
                    tunnel.receive();
                } catch (IOException ignore) {
                    // Not what we expected, time out
                } catch (ClosedException ignore) {
                    done.release();
                }
            }).start();

            // Wait a while to make sure the receive progresses and close the tunnel
            Thread.sleep(SLEEP / 2);
            tunnel.close();

            // Verify the receive interruption and failure to invoke new
            Assert.assertTrue(done.tryAcquire(SLEEP, TimeUnit.MILLISECONDS));
            try {
                tunnel.receive();
                Assert.fail("Receive succeeded on closed tunnel");
            } catch (ClosedException ignore) {
                // Ok, tunnel was indeed closed
            } catch (Exception e) {
                Assert.fail("Receive didn't report closure: " + e.getMessage());
            }
            // Terminate the remote tunnel and wait for cleanup
            handler.close.release();
            handler.done.acquire();
        }
    }

    // Tests that remotely closing a tunnel indeed interrupts local pending receives.
    @BenchmarkOptions(benchmarkRounds = 5, warmupRounds = 10)
    @Test public void interruptRemoteReceive() throws Exception {
        final int SLEEP = 250;

        // Create the service handler and register to the relay
        final TunnelTestInterruptHandler handler = new TunnelTestInterruptHandler(SLEEP);
        try (final Service ignored = new Service(TestConfigs.RELAY_PORT, TestConfigs.CLUSTER_NAME, handler)) {
            // Initiate tunnel construction and wait for remote activation
            try (final Tunnel tunnel = handler.connection.tunnel(TestConfigs.CLUSTER_NAME, 1000)) {
                handler.active.acquire();

                // Start a receive operation and wait until remote side closes the tunnel
                final Semaphore done = new Semaphore(0);
                new Thread(() -> {
                    try {
                        tunnel.receive();
                    } catch (IOException ignore) {
                        // Not what we expected, time out
                    } catch (ClosedException ignore) {
                        done.release();
                    }
                }).start();
                handler.done.acquire();

                // Verify the receive interruption and failure to invoke new
                Assert.assertTrue(done.tryAcquire(SLEEP, TimeUnit.MILLISECONDS));
                try {
                    tunnel.receive();
                    Assert.fail("Receive succeeded on closed tunnel");
                } catch (ClosedException ignore) {
                    // Ok, tunnel was indeed closed
                } catch (Exception e) {
                    Assert.fail("Receive didn't report closure: " + e.getMessage());
                }
            }
        }
    }

    // Tests that locally closing a tunnel indeed interrupts pending sends.
    @BenchmarkOptions(benchmarkRounds = 5, warmupRounds = 10)
    @Test public void interruptLocalSend() throws Exception {
        final int SLEEP = 10000;

        // Create the service handler and register to the relay
        final TunnelTestInterruptHandler handler = new TunnelTestInterruptHandler(SLEEP);
        try (final Service ignored = new Service(TestConfigs.RELAY_PORT, TestConfigs.CLUSTER_NAME, handler)) {
            // Initiate tunnel construction and wait for remote activation
            final Tunnel tunnel = handler.connection.tunnel(TestConfigs.CLUSTER_NAME, 1000);
            handler.active.acquire();

            // Fill the outbound buffers until a send times out, then wait
            final Semaphore full = new Semaphore(0);
            final Semaphore done = new Semaphore(0);
            new Thread(() -> {
                // Fill the outbound buffers until no more is accepted
                final byte[] message = new byte[1024 * 1024];
                try {
                    while (true) {
                        tunnel.send(message, 100);
                    }
                } catch (IOException | ClosedException ignore) {
                    // Not what we expected, time out
                    return;
                } catch (TimeoutException ignore) {
                    // Ok, proceed to next phase
                }
                // Signal buffers full and do a final blocking send
                full.release();
                try {
                    tunnel.send(message);
                } catch (IOException ignore) {
                    // Not what we expected, time out
                } catch (ClosedException ignore) {
                    done.release();
                }
            }).start();

            // Wait until buffers fill up, then a while to reach the send
            full.acquire();
            Thread.sleep(100);
            tunnel.close();

            // Verify the send interruption and failure to invoke new
            Assert.assertTrue(done.tryAcquire(250, TimeUnit.MILLISECONDS));
            try {
                tunnel.send(new byte[]{0x00});
                Assert.fail("Send succeeded on closed tunnel");
            } catch (ClosedException ignore) {
                // Ok, tunnel was indeed closed
            } catch (Exception e) {
                Assert.fail("Send didn't report closure: " + e.getMessage());
            }
            // Terminate the remote tunnel and wait for cleanup
            handler.close.release();
            handler.done.acquire();
        }
    }

    // Tests that locally closing a tunnel indeed interrupts remote pending sends.
    @BenchmarkOptions(benchmarkRounds = 5, warmupRounds = 10)
    @Test public void interruptRemoteSend() throws Exception {
        final int SLEEP = 10000;

        // Create the service handler and register to the relay
        final TunnelTestInterruptHandler handler = new TunnelTestInterruptHandler(SLEEP);
        try (final Service ignored = new Service(TestConfigs.RELAY_PORT, TestConfigs.CLUSTER_NAME, handler)) {
            // Initiate tunnel construction and wait for remote activation
            try (final Tunnel tunnel = handler.connection.tunnel(TestConfigs.CLUSTER_NAME, 1000)) {
                handler.active.acquire();

                // Fill the outbound buffers until a send times out, then wait
                final Semaphore full = new Semaphore(0);
                final Semaphore done = new Semaphore(0);
                new Thread(() -> {
                    // Fill the outbound buffers until no more is accepted
                    final byte[] message = new byte[1024 * 1024];
                    try {
                        while (true) {
                            tunnel.send(message, 100);
                        }
                    } catch (IOException | ClosedException ignore) {
                        // Not what we expected, time out
                        return;
                    } catch (TimeoutException ignore) {
                        // Ok, proceed to next phase
                    }
                    // Signal buffers full and do a final blocking send
                    full.release();
                    try {
                        tunnel.send(message);
                    } catch (IOException ignore) {
                        // Not what we expected, time out
                    } catch (ClosedException ignore) {
                        done.release();
                    }
                }).start();

                // Wait until buffers fill up, then close remote side
                full.acquire();
                handler.close.release();
                handler.done.acquire();

                // Verify the send interruption and failure to invoke new
                Assert.assertTrue(done.tryAcquire(250, TimeUnit.MILLISECONDS));
                try {
                    tunnel.send(new byte[]{0x00});
                    Assert.fail("Send succeeded on closed tunnel");
                } catch (ClosedException ignore) {
                    // Ok, tunnel was indeed closed
                } catch (Exception e) {
                    Assert.fail("Send didn't report closure: " + e.getMessage());
                }
            }
        }
    }
}