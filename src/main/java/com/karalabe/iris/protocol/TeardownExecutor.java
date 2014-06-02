// Copyright (c) 2014 Project Iris. All rights reserved.
//
// The current language binding is an official support library of the Iris
// cloud messaging framework, and as such, the same licensing terms apply.
// For details please see http://iris.karalabe.com/downloads#License
package com.karalabe.iris.protocol;

import com.karalabe.iris.ServiceHandler;

import java.io.IOException;

public class TeardownExecutor extends ExecutorBase {
    private final ServiceHandler    handler; // Callback handler for processing out of bound drops

    public TeardownExecutor(final ProtocolBase protocol, final ServiceHandler handler) {
        super(protocol);
        this.handler = handler;
    }

    public void handleTeardown() throws IOException {
        final String reason = protocol.receiveString();
        if (!reason.isEmpty()) {
            handler.handleDrop(new RuntimeException(reason));
        }
    }
}
