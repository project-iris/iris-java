package com.karalabe.iris.protocol.request_reply;

import com.karalabe.iris.protocol.OpCode;
import com.karalabe.iris.protocol.ProtocolBase;
import com.karalabe.iris.callback.CallbackHandlerRegistry;
import com.karalabe.iris.protocol.TransferBase;
import com.karalabe.iris.protocol.Validators;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class RequestTransfer extends TransferBase implements RequestApi {
    public RequestTransfer(@NotNull final ProtocolBase protocol, @NotNull final CallbackHandlerRegistry callbacks) { super(protocol, callbacks); }

    @Override public void request(@NotNull final String clusterName, @NotNull byte[] request, long timeOutMillis, RequestCallbackHandler callbackHandler) throws IOException {
        Validators.validateRemoteClusterName(clusterName);
        Validators.validateMessage(request);

        final Long requestId = addCallbackHandler(callbackHandler);
        protocol.send(OpCode.REQUEST, () -> {
            protocol.sendVarint(requestId);
            protocol.sendString(clusterName);
            protocol.sendBinary(request);
            protocol.sendVarint(timeOutMillis);
        });
    }

    public void handle() throws IOException {
        try {
            final long requestId = protocol.receiveVarint();
            final byte[] message = protocol.receiveBinary();

            final RequestCallbackHandler handler = callbacks.useCallbackHandler(requestId);
            handler.handleEvent(requestId, message);
        }
        catch (IllegalArgumentException e) {
            throw new IOException(String.format("No %s found!", RequestCallbackHandler.class.getSimpleName()), e);
        }
    }
}