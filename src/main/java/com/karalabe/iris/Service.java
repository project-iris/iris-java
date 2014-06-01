package com.karalabe.iris;

import java.io.IOException;

public class Service implements AutoCloseable {
    private Connection conn;

    public Service(final int port, final String cluster, final ServiceHandler handler) throws IOException {
        conn = new Connection(port, cluster, handler);
    }

    @Override public void close() throws Exception {
        conn.close();
    }
}
