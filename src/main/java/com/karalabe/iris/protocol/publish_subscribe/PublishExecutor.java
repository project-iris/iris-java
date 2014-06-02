package com.karalabe.iris.protocol.publish_subscribe;

import com.karalabe.iris.protocol.ExecutorBase;
import com.karalabe.iris.protocol.ProtocolBase;
import org.jetbrains.annotations.NotNull;

public class PublishExecutor extends ExecutorBase {
    public PublishExecutor(@NotNull final ProtocolBase protocol) { super(protocol); }

    @Override public void close() throws Exception {

    }

    /*public void publish(@NotNull final String topic, @NotNull final byte[] message) throws IOException {
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
    }*/
}