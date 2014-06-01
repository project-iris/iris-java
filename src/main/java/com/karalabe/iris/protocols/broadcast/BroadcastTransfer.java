package com.karalabe.iris.protocols.broadcast;

import com.karalabe.iris.OpCode;
import com.karalabe.iris.ProtocolBase;
import com.karalabe.iris.callback.CallbackHandlerRegistry;
import com.karalabe.iris.protocols.TransferBase;
import com.karalabe.iris.protocols.Validators;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class BroadcastTransfer extends TransferBase implements BroadcastAPI {
    public BroadcastTransfer(@NotNull final ProtocolBase protocol, @NotNull final CallbackHandlerRegistry callbacks) { super(protocol, callbacks); }

    @Override public void broadcast(@NotNull final String clusterName, @NotNull final byte[] message) throws IOException {
        Validators.validateClusterName(clusterName);
        Validators.validateMessage(message);

        protocol.send(OpCode.BROADCAST, () -> {
            protocol.sendString(clusterName);
            protocol.sendBinary(message);
        });
    }

    public void handle() throws IOException {
        try {
            final byte[] message = protocol.receiveBinary();

            final BroadcastCallbackHandler handler = callbacks.useCallbackHandler(BroadcastCallbackHandler.ID);
            handler.handleEvent(message);
        }
        catch (IllegalArgumentException e) {
            throw new IOException(String.format("No %s found!", BroadcastCallbackHandler.class.getSimpleName()), e);
        }
    }
}