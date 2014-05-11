package com.karalabe.iris.protocols.publish_subscribe;

import com.karalabe.iris.OpCode;
import com.karalabe.iris.ProtocolBase;
import com.karalabe.iris.callback.CallbackHandlerRegistry;
import com.karalabe.iris.protocols.TransferBase;
import com.karalabe.iris.protocols.Validators;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class PublishTransfer extends TransferBase implements PublishApi {
    public PublishTransfer(@NotNull final ProtocolBase protocol, @NotNull final CallbackHandlerRegistry callbacks) { super(protocol, callbacks); }

    @Override public void publish(@NotNull final String topic, @NotNull final byte[] message) throws IOException {
        Validators.validateTopic(topic);
        Validators.validateMessage(message);

        protocol.send(OpCode.PUBLISH, () -> {
            protocol.sendString(topic);
            protocol.sendBinary(message);
        });
    }

    public void handle() throws IOException {
        try {
            final String topic = protocol.receiveString();
            final byte[] message = protocol.receiveBinary();

            final PublishCallbackHandler handler = callbacks.useCallbackHandler(PublishCallbackHandler.ID);
            handler.handleEvent(topic, message);
        }
        catch (IllegalArgumentException e) {
            throw new IOException(String.format("No %s found!", PublishCallbackHandler.class.getSimpleName()), e);
        }
    }
}