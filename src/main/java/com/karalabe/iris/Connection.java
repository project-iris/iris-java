package com.karalabe.iris;

import com.karalabe.iris.protocol.*;
import com.karalabe.iris.protocol.TunnelExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.concurrent.TimeoutException;

/*
 * Message relay between the local app and the local iris node.
 **/
public class Connection implements AutoCloseable {
    private final ProtocolBase protocol;
    private final Thread       runner;

    // Application layer fields
    private final ServiceHandler handler;

    // Network layer fields
    private final HandshakeExecutor    handshaker;
    private final BroadcastExecutor    broadcaster;
    private final RequestExecutor      requester;
    private final SubscriptionExecutor subscriber;
    private final TeardownExecutor     teardowner;
    private final TunnelExecutor       tunneler;

    // Connects to the Iris network as a simple client.
    Connection(int port, @NotNull String clusterName, @Nullable ServiceHandler handler, @Nullable ServiceLimits limits) throws IOException {
        // Load the default service limits if none specified
        if (limits == null) { limits = new ServiceLimits(); }

        this.handler = handler;

        protocol = new ProtocolBase(port);

        handshaker = new HandshakeExecutor(protocol);
        broadcaster = new BroadcastExecutor(protocol, handler, limits);
        requester = new RequestExecutor(protocol, handler, limits);
        subscriber = new SubscriptionExecutor(protocol);
        teardowner = new TeardownExecutor(protocol, handler);
        tunneler = new TunnelExecutor(protocol);

        handshaker.init(clusterName);
        handshaker.handleInit();

        runner = new Thread(this::processMessages);
        runner.start();
    }

    public void broadcast(@NotNull final String cluster, @NotNull final byte[] message) throws IOException {
        Validators.validateRemoteClusterName(cluster);
        broadcaster.broadcast(cluster, message);
    }

    public byte[] request(@NotNull final String cluster, @NotNull final byte[] request, final long timeoutMillis) throws IOException, InterruptedException, RemoteException, TimeoutException {
        Validators.validateRemoteClusterName(cluster);
        return requester.request(cluster, request, timeoutMillis);
    }

    public void subscribe(@NotNull final String topic, @NotNull final TopicHandler handler) throws IOException {
        subscribe(topic, handler, null);
    }

    public void subscribe(@NotNull final String topic, @NotNull final TopicHandler handler, @Nullable TopicLimits limits) throws IOException {
        Validators.validateTopicName(topic);
        if (limits == null) { limits = new TopicLimits(); }
        subscriber.subscribe(topic, handler, limits);
    }

    public void publish(@NotNull final String topic, @NotNull final byte[] event) throws IOException {
        Validators.validateTopicName(topic);
        subscriber.publish(topic, event);
    }

    public void unsubscribe(@NotNull final String topic) throws IOException, InterruptedException {
        Validators.validateTopicName(topic);
        subscriber.unsubscribe(topic);
    }

    public Tunnel tunnel(@NotNull final String cluster, final long timeout) throws IOException {
        Validators.validateRemoteClusterName(cluster);
        return tunneler.tunnel(cluster, timeout);
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
                        //tunneler.handleTunnelBuild();
                        break;
                    case TUNNEL_CONFIRM:
                        //tunneler.handleTunnelConfirm();
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
                        throw new IllegalStateException(String.format("Illegal %s received: '%s'!", OpCode.class.getSimpleName(), opCode));
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            try {
                protocol.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override public void close() throws IOException, InterruptedException {
        if (runner != null) {
            teardowner.teardown();
            runner.join();
        }
    }
}
