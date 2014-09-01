// Copyright (c) 2014 Project Iris. All rights reserved.
//
// The current language binding is an official support library of the Iris
// cloud messaging framework, and as such, the same licensing terms apply.
// For details please see http://iris.karalabe.com/downloads#License
package com.karalabe.iris.protocol;

import org.jetbrains.annotations.NotNull;

public abstract class ExecutorBase implements AutoCloseable {
    protected final ProtocolBase protocol;

    protected ExecutorBase(@NotNull final ProtocolBase protocol) {
        this.protocol = protocol;
    }

    @Override public void close() throws Exception {}
}
