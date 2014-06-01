package com.karalabe.iris.protocol.request_reply;

import com.karalabe.iris.protocol.OpCode;
import com.karalabe.iris.protocol.ProtocolBase;
import com.karalabe.iris.callback.CallbackHandlerRegistry;
import com.karalabe.iris.protocol.TransferBase;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class ReplyTransfer extends TransferBase {
    public ReplyTransfer(@NotNull final ProtocolBase protocol, @NotNull final CallbackHandlerRegistry callbacks) { super(protocol, callbacks); }

    public void reply(final long requestId, final byte[] request) throws IOException {
        protocol.send(OpCode.REPLY, () -> {
            protocol.sendVarint(requestId);
            protocol.sendBinary(request);
        });
    }

    public void handle() throws IOException {
        try {
            final long requestId = protocol.receiveVarint();
            final boolean hasTimedOut = protocol.receiveBoolean();

            final ReplyCallbackHandler handler = callbacks.useCallbackHandler(requestId);
            if (hasTimedOut) {
                handler.handleEvent(requestId, null);
            } else {

                final byte[] reply = protocol.receiveBinary();
                handler.handleEvent(requestId, reply);
            }
        }
        catch (IllegalArgumentException e) {
            throw new IOException(String.format("No %s found!", ReplyCallbackHandler.class.getSimpleName()), e);
        }
    }
}