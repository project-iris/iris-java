// Copyright (c) 2014 Project Iris. All rights reserved.
//
// The current language binding is an official support library of the Iris
// cloud messaging framework, and as such, the same licensing terms apply.
// For details please see http://iris.karalabe.com/downloads#License
package com.karalabe.iris;

import com.karalabe.iris.common.ContextualLogger;
import com.karalabe.iris.exceptions.RemoteException;
import com.karalabe.iris.exceptions.TimeoutException;
import com.karalabe.iris.protocol.RelayProtocol;
import com.karalabe.iris.schemes.BroadcastScheme;
import com.karalabe.iris.schemes.PublishScheme;
import com.karalabe.iris.schemes.RequestScheme;
import com.karalabe.iris.schemes.TunnelScheme;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.regex.Pattern;

/*
 * Message relay between the local app and the local iris node.
 **/
public class Connection implements AutoCloseable {
    static final class Validators {
        private static final Pattern CLUSTER_NAME_PATTERN    = Pattern.compile("[^:]*");
        private static final Pattern CLUSTER_ADDRESS_PATTERN = Pattern.compile("([^:]+:)?[^:]+");
        private static final Pattern TOPIC_NAME_PATTERN      = Pattern.compile("[^:]*");

        private Validators() {}

        public static void validateClusterName(@NotNull final String cluster) {
            if (!CLUSTER_NAME_PATTERN.matcher(cluster).matches()) {
                throw new IllegalArgumentException("Cluster names may not contain the scoping operator ':'");
            }
        }

        public static void validateClusterAddress(@NotNull final String cluster) {
            if (!CLUSTER_ADDRESS_PATTERN.matcher(cluster).matches()) {
                throw new IllegalArgumentException("Cluster addresses may contain a maximum of one scoping operator ':'");
            }
        }

        public static void validateTopicName(@NotNull final String topic) {
            if (!TOPIC_NAME_PATTERN.matcher(topic).matches()) {
                throw new IllegalArgumentException("Topic names may not contain the scoping operator ':'");
            }
        }
    }

    private final RelayProtocol    protocol; // Iris relay protocol wire format implementation
    private final Thread           runner;   // Thread reading and handling the inbound messages
    private final ContextualLogger logger;   // Logger with connection id injected

    // Communication pattern implementers
    private final BroadcastScheme broadcaster;
    private final RequestScheme   requester;
    private final PublishScheme   subscriber;
    private final TunnelScheme    tunneler;

    // Connects to the Iris network as a simple client.
    Connection(final int port, final String cluster, final ServiceHandler handler, final ServiceLimits limits, final ContextualLogger logger) throws IOException {
        Validators.validateClusterName(cluster);

        this.logger = logger;

        protocol = new RelayProtocol(port, cluster);

        // Create the individual message pattern implementations
        broadcaster = new BroadcastScheme(protocol, handler, limits, logger);
        requester = new RequestScheme(protocol, handler, limits, logger);
        subscriber = new PublishScheme(protocol, logger);
        tunneler = new TunnelScheme(protocol, handler, logger);

        // Start processing inbound network packets
        runner = new Thread(() -> protocol.process(handler, broadcaster, requester, subscriber, tunneler));
        runner.start();
    }

    // Broadcasts a message to all members of a cluster. No guarantees are made that
    // all recipients receive the message (best effort).
    //
    // The call blocks until the message is forwarded to the local Iris node.
    public void broadcast(@NotNull final String cluster, @NotNull final byte[] message) throws IOException {
        Validators.validateClusterAddress(cluster);
        broadcaster.broadcast(cluster, message);
    }

    // Executes a synchronous request to be serviced by a member of the specified
    // cluster, load-balanced between all participant, returning the received reply.
    //
    // The timeout unit is in milliseconds. Anything lower will fail with an error.
    public byte[] request(@NotNull final String cluster, @NotNull final byte[] request, final long timeout) throws IOException, InterruptedException, RemoteException, TimeoutException {
        Validators.validateClusterAddress(cluster);
        return requester.request(cluster, request, timeout);
    }

    // Subscribes to a topic using handler as the callback for arriving events.
    //
    // The method blocks until the subscription is forwarded to the relay. There
    // might be a small delay between subscription completion and start of event
    // delivery. This is caused by subscription propagation through the network.
    public void subscribe(@NotNull final String topic, @NotNull final TopicHandler handler) throws IOException {
        subscribe(topic, handler, null);
    }

    // Subscribes to a topic using handler as the callback for arriving events,
    // and additionally sets some limits on the inbound event processing.
    //
    // The method blocks until the subscription is forwarded to the relay. There
    // might be a small delay between subscription completion and start of event
    // delivery. This is caused by subscription propagation through the network.
    public void subscribe(@NotNull final String topic, @NotNull final TopicHandler handler, @Nullable TopicLimits limits) throws IOException {
        Validators.validateTopicName(topic);
        if (limits == null) { limits = new TopicLimits(); }
        subscriber.subscribe(topic, handler, limits);
    }

    // Publishes an event asynchronously to topic. No guarantees are made that all
    // subscribers receive the message (best effort).
    //
    // The method blocks until the message is forwarded to the local Iris node.
    public void publish(@NotNull final String topic, @NotNull final byte[] event) throws IOException {
        Validators.validateTopicName(topic);
        subscriber.publish(topic, event);
    }

    // Unsubscribes from topic, receiving no more event notifications for it.
    //
    // The method blocks until the unsubscription is forwarded to the local Iris node.
    public void unsubscribe(@NotNull final String topic) throws IOException {
        Validators.validateTopicName(topic);
        subscriber.unsubscribe(topic);
    }

    // Opens a direct tunnel to a member of a remote cluster, allowing pairwise-
    // exclusive, order-guaranteed and throttled message passing between them.
    //
    // The method blocks until the newly created tunnel is set up, or the time
    // limit is reached.
    //
    // The timeout unit is in milliseconds. Anything lower will fail with an error.
    public Tunnel tunnel(@NotNull final String cluster, final long timeout) throws IOException, TimeoutException, InterruptedException {
        Validators.validateClusterAddress(cluster);
        return new Tunnel(tunneler.tunnel(cluster, timeout));
    }

    // Gracefully terminates the connection removing all subscriptions and closing
    // all active tunnels.
    //
    // The call blocks until the connection tear-down is confirmed by the Iris node.
    @Override public void close() throws IOException, InterruptedException {
        logger.loadContext();
        try {
            logger.info("Detaching from relay");

            // Terminate the relay connection
            protocol.sendClose();
            runner.join();

            // Tear down the individual scheme implementations
            tunneler.close();
            subscriber.close();
            requester.close();
            broadcaster.close();
        } finally {
            // Make sure the logger context doesn't leak out
            logger.unloadContext();
        }
    }
}
