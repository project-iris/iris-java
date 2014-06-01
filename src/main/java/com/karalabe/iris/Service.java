package com.karalabe.iris;

import com.karalabe.iris.protocols.Validators;

import java.io.IOException;

public class Service implements AutoCloseable {
    private Connection     conn;
    private ServiceHandler handler;

    // Connects to the Iris network and registers a new service instance as a member
    // of the specified service cluster.
    public Service(final int port, final String cluster, final ServiceHandler handler) throws IOException {
        Validators.validateLocalClusterName(cluster);

        conn = new Connection(port, cluster, handler);
        this.handler = handler;
    }

    // Unregisters the service instance from the Iris network.
    @Override public void close() throws Exception {
        conn.close();
    }
}
