package com.karalabe.iris.protocols.tunnel;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public interface TunnelApi {
    /* Opens a direct tunnel to an instance of app, allowing pairwise-exclusive and order-guaranteed message passing between them.
     * The method blocks until either the newly created tunnel is set up, or an error occurs, in which case a nil tunnel and an iris.Error is returned.
     * The timeOutMillis unit is in milliseconds. Setting anything smaller will result in a panic! */
    void tunnel(@NotNull final String clusterName, long timeOutMillis, TunnelCallbackHandlers callbackHandlers) throws IOException;
}