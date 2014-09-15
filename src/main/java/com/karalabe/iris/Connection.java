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
import com.karalabe.iris.protocol.Validators;
import com.karalabe.iris.schemes.BroadcastScheme;
import com.karalabe.iris.schemes.PublishScheme;
import com.karalabe.iris.schemes.RequestScheme;
import com.karalabe.iris.schemes.TunnelScheme;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * Message relay between the local app and the local iris node.
 */
public class Connection implements AutoCloseable {
    private final RelayProtocol    protocol; // Iris relay protocol wire format implementation
    private final Thread           runner;   // Thread reading and handling the inbound messages
    private final ServiceHandler   handler;  // Callback handler for inbound service events
    private final ContextualLogger logger;   // Logger with connection id injected

    // Communication pattern implementers
    private final BroadcastScheme broadcaster;
    private final RequestScheme   requester;
    private final PublishScheme   subscriber;
    private final TunnelScheme    tunneler;

    // Connects to the Iris network as a simple client.
    Connection(final int port, final String cluster, final ServiceHandler handler, final ServiceLimits limits, final ContextualLogger logger) throws IOException {
        Validators.validateClusterName(cluster);

        this.handler = handler;
        this.logger = logger;

        protocol = new RelayProtocol(port, cluster);

        // Create the individual message pattern implementations
        broadcaster = new BroadcastScheme(protocol, handler, limits, logger);
        requester = new RequestScheme(protocol, handler, limits, logger);
        subscriber = new PublishScheme(protocol, logger);
        tunneler = new TunnelScheme(protocol, handler, logger, Tunnel::new);

        // Start processing inbound network packets
        runner = new Thread(() -> protocol.process(broadcaster, requester, subscriber, tunneler, this::handleClose));
        runner.start();
    }

    /**
     * Broadcasts a message to all members of a cluster. No guarantees are made that
     * all recipients receive the message (best effort).
     *
     * The call blocks until the message is forwarded to the local Iris node.
     * @param cluster name of the micro-service cluster to broadcast a message to
     * @param message binary data contents of the message to broadcast
     */
    public void broadcast(@NotNull final String cluster, @NotNull final byte[] message) throws IOException {
        Validators.validateClusterAddress(cluster);
        Validators.validateBroadcastPayload(message);

        broadcaster.broadcast(cluster, message);
    }

    /**
     * Executes a synchronous request to be serviced by a member of the specified
     * cluster, load-balanced between all participant, returning the received reply.
     *
     * The timeout unit is in milliseconds. Anything lower will fail with an error.
     * @param cluster name of the micro-service cluster to handle the request
     * @param request binary data contents of the request to service
     * @param timeout milliseconds to wait for the remote response to arrive
     * @return binary data contents of the remote reply to the request
     */
    public byte[] request(@NotNull final String cluster, @NotNull final byte[] request, final long timeout) throws IOException, InterruptedException, RemoteException, TimeoutException {
        Validators.validateClusterAddress(cluster);
        Validators.validateRequestPayload(request);

        return requester.request(cluster, request, timeout);
    }

    /**
     * Subscribes to a topic using handler as the callback for arriving events.
     *
     * The method blocks until the subscription is forwarded to the relay. There
     * might be a small delay between subscription completion and start of event
     * delivery. This is caused by subscription propagation through the network.
     * @param topic   name of the topic to subscribe to
     * @param handler callback handler for inbound events published to the topic
     */
    public void subscribe(@NotNull final String topic, @NotNull final TopicHandler handler) throws IOException {
        subscribe(topic, handler, null);
    }

    /**
     * Subscribes to a topic using handler as the callback for arriving events,
     * and additionally sets some limits on the inbound event processing.
     *
     * The method blocks until the subscription is forwarded to the relay. There
     * might be a small delay between subscription completion and start of event
     * delivery. This is caused by subscription propagation through the network.
     * @param topic   name of the topic to subscribe to
     * @param handler callback handler for inbound events published to the topic
     * @param limits  custom resource consumption limits for inbound events
     */
    public void subscribe(@NotNull final String topic, @NotNull final TopicHandler handler, @Nullable TopicLimits limits) throws IOException {
        Validators.validateTopicName(topic);
        if (limits == null) { limits = new TopicLimits(); }
        subscriber.subscribe(topic, handler, limits);
    }

    /**
     * Publishes an event asynchronously to topic. No guarantees are made that all
     * subscribers receive the message (best effort).
     *
     * The method blocks until the message is forwarded to the local Iris node.
     * @param topic name of the topic into which to publish the event
     * @param event binary data contents of the event to publish
     */
    public void publish(@NotNull final String topic, @NotNull final byte[] event) throws IOException {
        Validators.validateTopicName(topic);
        Validators.validatePublishPayload(event);

        subscriber.publish(topic, event);
    }

    /**
     * Unsubscribes from topic, receiving no more event notifications for it.
     *
     * The method blocks until the unsubscription is forwarded to the local Iris node.
     * @param topic name of the topic to unsubscribe from
     */
    public void unsubscribe(@NotNull final String topic) throws IOException {
        Validators.validateTopicName(topic);
        subscriber.unsubscribe(topic);
    }

    /**
     * Opens a direct tunnel to a member of a remote cluster, allowing pairwise-
     * exclusive, order-guaranteed and throttled message passing between them.
     *
     * The method blocks until the newly created tunnel is set up, or the time
     * limit is reached.
     *
     * The timeout unit is in milliseconds. Anything lower will fail with an error.
     * @param cluster name of the micro-service cluster to open a tunnel into
     * @param timeout milliseconds to wait for the tunnel construction to complete
     * @return active tunnel into a remote Iris micro-service
     */
    public Tunnel tunnel(@NotNull final String cluster, final long timeout) throws IOException, TimeoutException, InterruptedException {
        Validators.validateClusterAddress(cluster);
        return tunneler.tunnel(cluster, timeout);
    }

    /**
     * Gracefully terminates the connection removing all subscriptions and closing
     * all active tunnels.
     *
     * The call blocks until the connection tear-down is confirmed by the Iris node.
     */
    @Override public void close() throws IOException, InterruptedException {
        logger.loadContext();
        logger.info("Detaching from relay");
        logger.unloadContext();

        // Terminate the relay connection
        protocol.sendClose();
        runner.join();
    }

    // Notifies the application of the relay link going down.
    private void handleClose(Exception reason) {
        logger.loadContext();
        if (reason != null) {
            logger.error("Connection dropped", "reason", reason.getMessage());
        } else {
            logger.info("Successfully detached");
        }
        logger.unloadContext();

        // Notify the client of the drop if premature
        if (reason != null) {
            // Only server connections have registered handlers
            if (handler != null) {
                handler.handleDrop(reason);
            }
        }
        // Tear down the individual scheme implementations
        try {
            tunneler.close();
            subscriber.close();
            requester.close();
            broadcaster.close();
        } catch (InterruptedException ignored) {}
    }
}
