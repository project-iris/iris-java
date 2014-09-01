// Copyright (c) 2014 Project Iris. All rights reserved.
//
// The current language binding is an official support library of the Iris
// cloud messaging framework, and as such, the same licensing terms apply.
// For details please see http://iris.karalabe.com/downloads#License
package com.karalabe.iris.protocol;

import com.karalabe.iris.ServiceHandler;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.rmi.RemoteException;

public class TeardownExecutor extends ExecutorBase {
    private final ServiceHandler handler; // Callback handler for processing out of bound drops

    public TeardownExecutor(final ProtocolBase protocol, @Nullable final ServiceHandler handler) {
        super(protocol);
        this.handler = handler;
    }

    public void teardown() throws IOException {
        protocol.send(OpCode.CLOSE, () -> {});
    }

    public void handleTeardown() throws IOException {
        final String reason = protocol.receiveString();
        if (!reason.isEmpty()) {
            handler.handleDrop(new RemoteException(reason));
        }
    }
}
