package com.karalabe.iris.protocol.tunnel;

import com.karalabe.iris.protocol.OpCode;
import com.karalabe.iris.protocol.ProtocolBase;
import com.karalabe.iris.callback.CallbackHandlerRegistry;
import com.karalabe.iris.protocol.TransferBase;
import com.karalabe.iris.protocol.Validators;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class TunnelTransfer extends TransferBase implements TunnelApi {
    public TunnelTransfer(@NotNull final ProtocolBase protocol, @NotNull final CallbackHandlerRegistry callbacks) { super(protocol, callbacks); }

    @Override public void tunnel(@NotNull final String clusterName, final long timeOutMillis, TunnelCallbackHandlers callbackHandlers) throws IOException {
        Validators.validateRemoteClusterName(clusterName);

        final int bufferSize = 0; // TODO
        final long id = addCallbackHandler(callbackHandlers);
        protocol.send(OpCode.TUNNEL_BUILD, () -> {
            protocol.sendVarint(id);
            protocol.sendString(clusterName);
            protocol.sendVarint(bufferSize); // TODO buf?
            protocol.sendVarint(timeOutMillis);
        });
    }

    public void handleTunnelBuild() throws IOException {
        try {
            final long id = protocol.receiveVarint(); // TODO tmpId?
            final long bufferSize = protocol.receiveVarint();

            final TunnelCallbackHandlers handler = callbacks.useCallbackHandler(id);
            handler.handleTunnelRequest(id, bufferSize);
        }
        catch (IllegalArgumentException e) {
            System.err.printf("No %s found!%n", TunnelCallbackHandlers.class.getSimpleName());
        }
    }

    public void sendTunnelConfirm(final long tempId, final long tunnelId, final long bufferSize) throws IOException {
        protocol.send(OpCode.TUNNEL_CONFIRM, () -> {
            protocol.sendVarint(tempId);     // TODO huh?
            protocol.sendVarint(tunnelId);
            protocol.sendVarint(bufferSize); // TODO buf?
        });
    }

    public void handleTunnelConfirm() throws IOException {
        try {
            final long id = protocol.receiveVarint();
            final boolean hasTimedOut = protocol.receiveBoolean();

            final TunnelCallbackHandlers handler = callbacks.useCallbackHandler(id);
            if (hasTimedOut) {
                handler.handleTunnelReply(id, 0, true);
            } else {

                final long bufferSize = protocol.receiveVarint();
                handler.handleTunnelReply(id, bufferSize, false);
            }
        }
        catch (IllegalArgumentException e) {
            System.err.printf("No %s found!%n", TunnelCallbackHandlers.class.getSimpleName());
        }
    }

    public void sendTunnelTransfer(final long tunnelId, @NotNull final byte[] message) throws IOException {
        protocol.send(OpCode.TUNNEL_TRANSFER, () -> {
            protocol.sendVarint(tunnelId);
            protocol.sendBinary(message);
        });
    }

    public void handleTunnelTransfer() throws IOException {
        try {
            final long id = protocol.receiveVarint();
            final byte[] message = protocol.receiveBinary();

            final TunnelCallbackHandlers handler = callbacks.useCallbackHandler(id);
            handler.handleTunnelData(id, message);
        }
        catch (IllegalArgumentException e) {
            System.err.printf("No %s found!%n", TunnelCallbackHandlers.class.getSimpleName());
        }
    }

    public void sendTunnelAllow(final long tunnelId) throws IOException {
        protocol.send(OpCode.TUNNEL_ALLOW, () -> protocol.sendVarint(tunnelId));
    }

    public void handleTunnelAllow() throws IOException {
        try {
            final long id = protocol.receiveVarint();

            final TunnelCallbackHandlers handler = callbacks.useCallbackHandler(id);
            handler.handleTunnelAck(id);
        }
        catch (IllegalArgumentException e) {
            System.err.printf("No %s found!%n", TunnelCallbackHandlers.class.getSimpleName());
        }
    }

    public void sendTunnelClose(final long tunnelId) throws IOException {
        protocol.send(OpCode.TUNNEL_CLOSE, () -> protocol.sendVarint(tunnelId));
    }

    public void handleTunnelClose() throws IOException {
        try {
            final long id = protocol.receiveVarint();

            final TunnelCallbackHandlers handler = callbacks.useCallbackHandler(id);
            handler.handleTunnelClose(id);
        }
        catch (IllegalArgumentException e) {
            System.err.printf("No %s found!%n", TunnelCallbackHandlers.class.getSimpleName());
        }
    }
}