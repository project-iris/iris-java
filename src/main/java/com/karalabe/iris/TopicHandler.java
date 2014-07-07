/*
 * Copyright © 2014 Project Iris. All rights reserved.
 *
 * The current language binding is an official support library of the Iris cloud messaging framework, and as such, the same licensing terms apply.
 * For details please see http://iris.karalabe.com/downloads#License
 */

package com.karalabe.iris;

import org.jetbrains.annotations.NotNull;

/** Callback interface for processing events from a single subscribed topic. */
public interface TopicHandler {
    /** Callback invoked whenever an event is published to the topic subscribed to by this particular handler. */
    default void handleEvent(@NotNull final byte... event) {}
}
