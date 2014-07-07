/*
 * Copyright © 2014 Project Iris. All rights reserved.
 *
 * The current language binding is an official support library of the Iris cloud messaging framework, and as such, the same licensing terms apply.
 * For details please see http://iris.karalabe.com/downloads#License
 */

package com.karalabe.iris;

import com.karalabe.iris.protocol.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeoutException;

/*
 * Message relay between the local app and the local iris node.
 **/
public class Connection implements AutoCloseable {
    private final ProtocolBase protocol;
    private final Thread       runner;

    /** Application layer fields */
    private final ServiceHandler handler;

    /** Network layer fields */
    private final HandshakeExecutor    handshaker;
    private final BroadcastExecutor    broadcaster;
    private final RequestExecutor      requester;
    private final SubscriptionExecutor subscriber;
    private final TeardownExecutor     teardowner;
    private final TunnelExecutor       tunneler;

    /** Connects to the Iris network as a simple client. */
    Connection(int port, @NotNull String clusterName, @Nullable ServiceHandler handler, @Nullable ServiceLimits limits) {
        if (limits == null) { limits = new ServiceLimits(); }

        this.handler = handler;

        protocol = new ProtocolBase(port);

        handshaker = new HandshakeExecutor(protocol);
        broadcaster = new BroadcastExecutor(protocol, handler, limits);
        requester = new RequestExecutor(protocol, handler, limits);
        subscriber = new SubscriptionExecutor(protocol);
        teardowner = new TeardownExecutor(protocol, handler);
        tunneler = new TunnelExecutor(protocol, handler);

        handshaker.init(clusterName);
        handshaker.handleInit();

        runner = new Thread(this::processMessages);
        runner.start();
    }

    public void broadcast(@NotNull final String cluster, @NotNull final byte... message) {
        Validators.validateRemoteClusterName(cluster);
        broadcaster.broadcast(cluster, message);
    }

    public byte[] request(@NotNull final String cluster, final long timeoutMillis, @NotNull final byte... request) throws InterruptedException, TimeoutException {
        Validators.validateRemoteClusterName(cluster);
        return requester.request(cluster, request, timeoutMillis);
    }

    public void subscribe(@NotNull final String topic, @NotNull final TopicHandler handler) {
        subscribe(topic, handler, null);
    }

    public void subscribe(@NotNull final String topic, @NotNull final TopicHandler handler, @Nullable TopicLimits limits) {
        Validators.validateTopicName(topic);
        if (limits == null) { limits = new TopicLimits(); }
        subscriber.subscribe(topic, handler, limits);
    }

    public void publish(@NotNull final String topic, @NotNull final byte... event) {
        Validators.validateTopicName(topic);
        subscriber.publish(topic, event);
    }

    public void unsubscribe(@NotNull final String topic) throws InterruptedException {
        Validators.validateTopicName(topic);
        subscriber.unsubscribe(topic);
    }

    public Tunnel tunnel(@NotNull final String cluster, final long timeout) throws TimeoutException, InterruptedException {
        Validators.validateRemoteClusterName(cluster);
        return new Tunnel(tunneler.tunnel(cluster, timeout));
    }

    private void processMessages() {
        try {
            while (true) {
                final OpCode opCode = OpCode.valueOf(protocol.receiveByte());
                switch (opCode) {
                    case BROADCAST:
                        broadcaster.handleBroadcast();
                        break;

                    case REQUEST:
                        requester.handleRequest();
                        break;
                    case REPLY:
                        requester.handleReply();
                        break;

                    case PUBLISH:
                        subscriber.handlePublish();
                        break;

                    case TUNNEL_BUILD:
                        tunneler.handleTunnelInit();
                        break;
                    case TUNNEL_CONFIRM:
                        tunneler.handleTunnelConfirm();
                        break;
                    case TUNNEL_ALLOW:
                        //tunneler.handleTunnelAllow();
                        break;
                    case TUNNEL_TRANSFER:
                        //tunneler.handleTunnelTransfer();
                        break;
                    case TUNNEL_CLOSE:
                        //tunneler.handleTunnelClose();
                        break;

                    case CLOSE:
                        teardowner.handleTeardown();
                        return;

                    default:
                        throw new ProtocolException(String.format("Illegal %s received: '%s'!", OpCode.class.getSimpleName(), opCode));
                }
            }
        } finally {
            protocol.close();
        }
    }

    @Override public void close() throws InterruptedException {
        teardowner.teardown();
        runner.join();
    }
}
