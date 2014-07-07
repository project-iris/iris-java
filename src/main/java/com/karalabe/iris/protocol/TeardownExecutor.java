/*
 * Copyright © 2014 Project Iris. All rights reserved.
 *
 * The current language binding is an official support library of the Iris cloud messaging framework, and as such, the same licensing terms apply.
 * For details please see http://iris.karalabe.com/downloads#License
 */

package com.karalabe.iris.protocol;

import com.karalabe.iris.ProtocolException;
import com.karalabe.iris.ServiceHandler;
import org.jetbrains.annotations.Nullable;

public class TeardownExecutor extends ExecutorBase {
    private final ServiceHandler handler; // Callback handler for processing out of bound drops

    public TeardownExecutor(final ProtocolBase protocol, @Nullable final ServiceHandler handler) {
        super(protocol);
        this.handler = handler;
    }

    public void teardown() { protocol.send(OpCode.CLOSE, () -> {}); }

    public void handleTeardown() {
        final String reason = protocol.receiveString();
        if ((handler != null) && !reason.isEmpty()) {
            handler.handleDrop(new ProtocolException(reason));
        }
    }
}
