package com.karalabe.iris.protocol.publish_subscribe;

import com.karalabe.iris.protocol.OpCode;
import com.karalabe.iris.protocol.ProtocolBase;
import com.karalabe.iris.callback.CallbackHandlerRegistry;
import com.karalabe.iris.protocol.TransferBase;
import com.karalabe.iris.protocol.Validators;
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