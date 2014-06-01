package com.karalabe.iris;

import com.karalabe.iris.callback.CallbackHandlerRegistry;
import com.karalabe.iris.callback.CallbackRegistry;
import com.karalabe.iris.callback.StaticCallbackHandler;
import com.karalabe.iris.protocols.Validators;
import com.karalabe.iris.protocols.broadcast.BroadcastAPI;
import com.karalabe.iris.protocols.broadcast.BroadcastTransfer;
import com.karalabe.iris.protocols.publish_subscribe.PublishApi;
import com.karalabe.iris.protocols.publish_subscribe.PublishTransfer;
import com.karalabe.iris.protocols.publish_subscribe.SubscribeApi;
import com.karalabe.iris.protocols.publish_subscribe.SubscribeTransfer;
import com.karalabe.iris.protocols.request_reply.ReplyTransfer;
import com.karalabe.iris.protocols.request_reply.RequestApi;
import com.karalabe.iris.protocols.request_reply.RequestCallbackHandler;
import com.karalabe.iris.protocols.request_reply.RequestTransfer;
import com.karalabe.iris.protocols.tunnel.TunnelApi;
import com.karalabe.iris.protocols.tunnel.TunnelCallbackHandlers;
import com.karalabe.iris.protocols.tunnel.TunnelTransfer;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ProtocolException;
import java.net.Socket;

/*
 * Message relay between the local app and the local iris node.
 **/
public class Connection implements CallbackRegistry, AutoCloseable, SubscribeApi, PublishApi, TunnelApi, BroadcastAPI, RequestApi {
    private static final String PROTOCOL_VERSION = "v1.0-draft2";
    private static final String CLIENT_MAGIC     = "iris-client-magic";
    private static final String RELAY_MAGIC      = "iris-relay-magic";

    private final Socket                  socket;  // Network connection to the iris node
    private final ProtocolBase            protocol;
    private final CallbackHandlerRegistry callbacks;

    private final BroadcastTransfer broadcastTransfer;
    private final RequestTransfer   requestTransfer;
    private final ReplyTransfer     replyTransfer;
    private final PublishTransfer   publishTransfer;
    private final SubscribeTransfer subscribeTransfer;
    private final TunnelTransfer    tunnelTransfer;

    public Connection(final int relayPort) throws IOException {
        this(relayPort, "", null);
    }

    Connection(int port, @NotNull String clusterName, ServiceHandler handler) throws IOException {
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
        Validators.validateLocalClusterName(clusterName);

        protocol.send(OpCode.INIT, () -> {
            protocol.sendString(CLIENT_MAGIC);
            protocol.sendString(PROTOCOL_VERSION);
            protocol.sendString(clusterName);
        });
    }

    private String handleInit() throws IOException {
        final OpCode opCode = OpCode.valueOf(protocol.receiveByte());

        switch (opCode) {
            case INIT: {
                verifyMagic();

                final String version = protocol.receiveString();
                return version;
            }

            case DENY: {
                verifyMagic();

                final String reason = protocol.receiveString();
                throw new ProtocolException(String.format("Connection denied: %s", reason));
            }

            default:
                throw new ProtocolException(String.format("Protocol violation: invalid init response opcode: %s", opCode));
        }
    }

    private void verifyMagic() throws IOException {
        final String relayMagic = protocol.receiveString();
        if (!RELAY_MAGIC.equals(relayMagic)) { throw new ProtocolException(String.format("Protocol violation: invalid relay magic: %s", relayMagic)); }
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

    @Override public void subscribe(@NotNull final String topic, @NotNull final TopicHandler handler) throws IOException {
        subscribeTransfer.subscribe(topic, handler);
    }

    @Override public void unsubscribe(@NotNull final String topic, @NotNull final TopicHandler handler) throws IOException {
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

                case TUNNEL_BUILD:
                    tunnelTransfer.handleTunnelBuild();
                    break;
                case TUNNEL_CONFIRM:
                    tunnelTransfer.handleTunnelConfirm();
                    break;
                case TUNNEL_ALLOW:
                    tunnelTransfer.handleTunnelAllow();
                    break;
                case TUNNEL_TRANSFER:
                    tunnelTransfer.handleTunnelTransfer();
                    break;
                case TUNNEL_CLOSE:
                    tunnelTransfer.handleTunnelClose();
                    break;

                case CLOSE:
                    return;

                default:
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