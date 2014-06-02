package com.karalabe.iris;

import com.karalabe.iris.callback.CallbackHandlerRegistry;
import com.karalabe.iris.callback.StaticCallbackHandler;
import com.karalabe.iris.protocol.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/*
 * Message relay between the local app and the local iris node.
 **/
public class Connection implements AutoCloseable {
    private final ProtocolBase            protocol;
    private final Thread                  runner;
    private final CallbackHandlerRegistry callbacks;

    // Application layer fields
    private final ServiceHandler handler;

    // Network layer fields
    private final HandshakeExecutor    handshaker;
    private final BroadcastExecutor    broadcaster;
    private final RequestExecutor      requester;
    private final SubscriptionExecutor subscriber;
    private final TeardownExecutor     teardowner;

    //private final PublishExecutor   publishTransfer;
    //private final SubscribeExecutor subscribeTransfer;
    //private final TunnelExecutor    tunnelTransfer;

    public Connection(final int relayPort) throws IOException {
        this(relayPort, "", null, null);
    }

    Connection(int port, @NotNull String clusterName, ServiceHandler handler, ServiceLimits limits) throws IOException {
        // Load the default service limits if none specified
        if (limits == null) {
            limits = new ServiceLimits();
        }

        this.handler = handler;

        protocol = new ProtocolBase(port);
        callbacks = new CallbackHandlerRegistry();

        handshaker = new HandshakeExecutor(protocol);
        broadcaster = new BroadcastExecutor(protocol, handler, limits);
        requester = new RequestExecutor(protocol, handler, limits);
        subscriber = new SubscriptionExecutor(protocol);
        teardowner = new TeardownExecutor(protocol, handler);

        //publishTransfer = new PublishExecutor(protocol);
        //subscribeTransfer = new SubscribeExecutor(protocol);
        //tunnelTransfer = new TunnelExecutor(protocol);

        handshaker.init(clusterName);
        handshaker.handleInit();

        runner = new Thread(() -> {
            processMessages();
        });
        runner.start();
    }

    public void addCallbackHandler(@NotNull final StaticCallbackHandler callbackHandler) {
        callbacks.addCallbackHandler(callbackHandler);
    }

    public void broadcast(@NotNull final String cluster, @NotNull final byte[] message) throws IOException {
        Validators.validateRemoteClusterName(cluster);
        broadcaster.broadcast(cluster, message);
    }

    public byte[] request(@NotNull final String cluster, @NotNull final byte[] request, final long timeoutMillis) throws IOException, InterruptedException, RemoteException, TimeoutException {
        Validators.validateRemoteClusterName(cluster);
        return requester.request(cluster, request, timeoutMillis);
    }

    public void subscribe(@NotNull final String topic, @NotNull final TopicHandler handler, TopicLimits limits) throws IOException {
        Validators.validateTopic(topic);
        if (limits == null) {
            limits = new TopicLimits();
        }
        subscriber.subscribe(topic, handler, limits);
    }

    public void publish(@NotNull final String topic, @NotNull final byte[] event) throws IOException {
        Validators.validateTopic(topic);
        subscriber.publish(topic, event);
    }

    public void unsubscribe(@NotNull final String topic) throws IOException, InterruptedException {
        Validators.validateTopic(topic);
        subscriber.unsubscribe(topic);
    }
/*
    @Override public void tunnel(@NotNull final String clusterName, final long timeOutMillis, TunnelCallbackHandlers callbackHandlers) throws IOException {
        tunnelTransfer.tunnel(clusterName, timeOutMillis, callbackHandlers);
    }*/

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
                        //tunnelTransfer.handleTunnelBuild();
                        break;
                    case TUNNEL_CONFIRM:
                        //tunnelTransfer.handleTunnelConfirm();
                        break;
                    case TUNNEL_ALLOW:
                        //tunnelTransfer.handleTunnelAllow();
                        break;
                    case TUNNEL_TRANSFER:
                        //tunnelTransfer.handleTunnelTransfer();
                        break;
                    case TUNNEL_CLOSE:
                        //tunnelTransfer.handleTunnelClose();
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
