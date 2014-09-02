package com.karalabe.iris;

import com.karalabe.iris.protocol.RelayProtocol;
import com.karalabe.iris.schemes.*;
import com.karalabe.iris.schemes.TunnelExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.concurrent.TimeoutException;

/*
 * Message relay between the local app and the local iris node.
 **/
public class Connection implements AutoCloseable {
    private final RelayProtocol protocol;
    private final Thread        runner;

    // Application layer fields
    private final ServiceHandler handler;

    // Communication pattern implementers
    private final BroadcastScheme broadcaster;
    private final RequestScheme   requester;
    private final PublishScheme   subscriber;
    //private final TunnelExecutor  tunneler;

    // Connects to the Iris network as a simple client.
    Connection(int port, @NotNull String cluster, @Nullable ServiceHandler handler, @Nullable ServiceLimits limits) throws IOException {
        // Load the default service limits if none specified
        if (limits == null) { limits = new ServiceLimits(); }

        this.handler = handler;

        protocol = new RelayProtocol(port, cluster);

        // Create the individual message pattern implementations
        broadcaster = new BroadcastScheme(protocol, handler, limits);
        requester = new RequestScheme(protocol, handler, limits);
        subscriber = new PublishScheme(protocol);
        //tunneler = new TunnelExecutor(protocol, handler);

        // Start processing inbound network packets
        runner = new Thread(new Runnable() {
            @Override public void run() {
                protocol.process(handler, broadcaster, requester, subscriber);
            }
        });
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

    public Tunnel tunnel(@NotNull final String cluster, final long timeout) throws IOException, TimeoutException, InterruptedException {
        Validators.validateRemoteClusterName(cluster);
        return null;//new Tunnel(tunneler.tunnel(cluster, timeout));
    }

    @Override public void close() throws IOException, InterruptedException {
        // Terminate the relay connection
        if (runner != null) {
            protocol.sendClose();
            runner.join();
        }
        // Tear down the individual scheme implementations
        broadcaster.close();
        requester.close();
        subscriber.close();
    }
}
