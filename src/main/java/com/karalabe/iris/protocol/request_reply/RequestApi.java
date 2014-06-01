package com.karalabe.iris.protocol.request_reply;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public interface RequestApi {
    /* Executes a synchronous request to app, load balanced between all the active ones, returning the received reply.
     * In case of a failure, the function returns a nil reply with an iris.Error stating the reason.
     * The timeOutMillis unit is in milliseconds. Setting anything smaller will result in a panic! */
    void request(@NotNull final String clusterName, @NotNull byte[] request, long timeOutMillis, RequestCallbackHandler callbackHandler) throws IOException;
}