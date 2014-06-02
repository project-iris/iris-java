package com.karalabe.iris.protocol.publish_subscribe;

import com.karalabe.iris.TopicHandler;
import com.karalabe.iris.TopicLimits;
import com.karalabe.iris.protocol.ExecutorBase;
import com.karalabe.iris.protocol.ProtocolBase;
import com.karalabe.iris.protocol.Validators;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class SubscribeExecutor extends ExecutorBase {
    public SubscribeExecutor(@NotNull final ProtocolBase protocol) { super(protocol); }

    @Override public void close() throws Exception {

    }

    public void subscribe(@NotNull final String topic, @NotNull final TopicHandler handler, final TopicLimits limits) throws IOException {
        Validators.validateTopic(topic);
/*
        final Long subscriptionId = addCallbackHandler(handler);// TODO is the topic the id?
        protocol.send(OpCode.SUBSCRIBE, () -> {
            protocol.sendVarint(subscriptionId); // TODO ???
            protocol.sendString(topic);
        });*/
    }

    public void unsubscribe(@NotNull final String topic, @NotNull final TopicHandler handler) throws IOException {
        Validators.validateTopic(topic);
/*
        final Long subscriptionId = addCallbackHandler(handler);// TODO is the topic the id?
        protocol.send(OpCode.UNSUBSCRIBE, () -> {
            protocol.sendVarint(subscriptionId); // TODO
            protocol.sendString(topic);
        });*/
    }
}