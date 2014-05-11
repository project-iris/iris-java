package com.karalabe.iris;

import com.karalabe.iris.callback.CallbackHandlerRegistry;
import com.karalabe.iris.callback.CallbackRegistry;
import com.karalabe.iris.callback.StaticCallbackHandler;
import com.karalabe.iris.protocols.Tunnel.TunnelApi;
import com.karalabe.iris.protocols.Tunnel.TunnelCallbackHandlers;
import com.karalabe.iris.protocols.Tunnel.TunnelTransfer;
import com.karalabe.iris.protocols.Validators;
import com.karalabe.iris.protocols.broadcast.BroadcastApi;
import com.karalabe.iris.protocols.broadcast.BroadcastTransfer;
import com.karalabe.iris.protocols.publish_subscribe.*;
import com.karalabe.iris.protocols.request_reply.ReplyTransfer;
import com.karalabe.iris.protocols.request_reply.RequestApi;
import com.karalabe.iris.protocols.request_reply.RequestCallbackHandler;
import com.karalabe.iris.protocols.request_reply.RequestTransfer;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ProtocolException;
import java.net.Socket;

/*
 * Message relay between the local app and the local iris node.
 **/
public class Connection implements CallbackRegistry, AutoCloseable, SubscribeApi, PublishApi, TunnelApi, BroadcastApi, RequestApi {
    private static final String VERSION = "v1.0";

    private final Socket                  socket;  // Network connection to the iris node
    private final ProtocolBase            protocol;
    private final CallbackHandlerRegistry callbacks;

    private final BroadcastTransfer broadcastTransfer;
    private final RequestTransfer   requestTransfer;
    private final ReplyTransfer     replyTransfer;
    private final PublishTransfer   publishTransfer;
    private final SubscribeTransfer subscribeTransfer;
    private final TunnelTransfer    tunnelTransfer;

    public Connection(int port, @NotNull String clusterName) throws IOException {
        socket = new Socket(InetAddress.getLoopbackAddress(), port);

        protocol = new ProtocolBase(socket.getInputStream(), socket.getOutputStream());
        callbacks = new CallbackHandlerRegistry();

        broadcastTransfer = new BroadcastTransfer(protocol, callbacks);
        requestTransfer = new RequestTransfer(protocol, callbacks);
        replyTransfer = new ReplyTransfer(protocol, callbacks);
        publishTransfer = new PublishTransfer(protocol, callbacks);
        subscribeTransfer = new SubscribeTransfer(protocol, callbacks);
        tunnelTransfer = new TunnelTransfer(protocol, callbacks);

        init(clusterName);
        handleInit();
    }

    public void addCallbackHandler(@NotNull final StaticCallbackHandler callbackHandler) {
        callbacks.addCallbackHandler(callbackHandler);
    }

    private void init(@NotNull final String clusterName) throws IOException {
        Validators.validateClusterName(clusterName);

        protocol.send(OpCode.INIT, () -> {
            protocol.sendString(VERSION);
            protocol.sendString(clusterName);
        });
    }

    private void handleInit() throws IOException {
        if (protocol.receiveByte() != OpCode.INIT.getOrdinal()) { throw new ProtocolException("Protocol violation"); }
    }

    @Override public void broadcast(@NotNull final String clusterName, @NotNull final byte[] message) throws IOException {
        broadcastTransfer.broadcast(clusterName, message);
    }

    @Override public void request(@NotNull final String clusterName, @NotNull final byte[] request, final long timeOutMillis, RequestCallbackHandler callbackHandler) throws IOException {
        requestTransfer.request(clusterName, request, timeOutMillis, callbackHandler);
    }

    @Override public void publish(@NotNull final String topic, @NotNull final byte[] message) throws IOException {
        publishTransfer.publish(topic, message);
    }

    @Override public void subscribe(@NotNull final String topic, @NotNull final SubscriptionHandler handler) throws IOException {
        subscribeTransfer.subscribe(topic, handler);
    }

    @Override public void unsubscribe(@NotNull final String topic, @NotNull final SubscriptionHandler handler) throws IOException {
        subscribeTransfer.unsubscribe(topic, handler);
    }

    @Override public void tunnel(@NotNull final String clusterName, final long timeOutMillis, TunnelCallbackHandlers callbackHandlers) throws IOException {
        tunnelTransfer.tunnel(clusterName, timeOutMillis, callbackHandlers);
    }

    public void handle() throws Exception {
        // TODO handle all messages
        // Producer/consumer?
        try {
            final OpCode opCode = OpCode.valueOf(protocol.receiveByte());
            switch (opCode) {
                case BROADCAST:
                    broadcastTransfer.handle();
                    break;

                case REQUEST:
                    requestTransfer.handle();
                    break;
                case REPLY:
                    replyTransfer.handle();
                    break;

                case PUBLISH:
                    publishTransfer.handle();
                    break;

                case TUNNEL_REQUEST:
                    tunnelTransfer.handleTunnelRequest();
                    break;
                case TUNNEL_REPLY:
                    tunnelTransfer.handleTunnelReply();
                    break;
                case TUNNEL_DATA:
                    tunnelTransfer.handleTunnelData();
                    break;
                case TUNNEL_ACK:
                    tunnelTransfer.handleTunnelAck();
                    break;
                case TUNNEL_CLOSE:
                    tunnelTransfer.handleTunnelClose();
                    break;

                case CLOSE:
                    return;

                default:
                case INIT:
                    throw new IllegalStateException(String.format("Illegal %s received: '%s'!", OpCode.class.getSimpleName(), opCode));
            }
        }
        finally {
            close();
        }
    }

    private void sendClose() throws IOException {
        protocol.send(OpCode.TUNNEL_CLOSE, () -> {});
    }

    @Override public void close() throws Exception {
        protocol.close();
        socket.close();
    }
}