package com.karalabe.iris.protocol.publish_subscribe;

import com.karalabe.iris.TopicHandler;
import com.karalabe.iris.TopicLimits;
import com.karalabe.iris.callback.CallbackHandlerRegistry;
import com.karalabe.iris.protocol.ProtocolBase;
import com.karalabe.iris.protocol.TransferBase;
import com.karalabe.iris.protocol.Validators;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class SubscribeTransfer extends TransferBase implements SubscribeApi {
    public SubscribeTransfer(@NotNull final ProtocolBase protocol, @NotNull final CallbackHandlerRegistry callbacks) { super(protocol, callbacks); }

    @Override public void subscribe(@NotNull final String topic, @NotNull final TopicHandler handler, final TopicLimits limits) throws IOException {
        Validators.validateTopic(topic);
/*
        final Long subscriptionId = addCallbackHandler(handler);// TODO is the topic the id?
        protocol.send(OpCode.SUBSCRIBE, () -> {
            protocol.sendVarint(subscriptionId); // TODO ???
            protocol.sendString(topic);
        });*/
    }

    @Override public void unsubscribe(@NotNull final String topic, @NotNull final TopicHandler handler) throws IOException {
        Validators.validateTopic(topic);
/*
        final Long subscriptionId = addCallbackHandler(handler);// TODO is the topic the id?
        protocol.send(OpCode.UNSUBSCRIBE, () -> {
            protocol.sendVarint(subscriptionId); // TODO
            protocol.sendString(topic);
        });*/
    }
}