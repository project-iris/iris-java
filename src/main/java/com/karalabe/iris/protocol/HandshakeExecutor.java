// Copyright (c) 2014 Project Iris. All rights reserved.
//
// The current language binding is an official support library of the Iris
// cloud messaging framework, and as such, the same licensing terms apply.
// For details please see http://iris.karalabe.com/downloads#License
package com.karalabe.iris.protocol;

import java.io.IOException;
import java.net.ProtocolException;

public class HandshakeExecutor extends ExecutorBase {
    private static final String PROTOCOL_VERSION = "v1.0-draft2";
    private static final String CLIENT_MAGIC     = "iris-client-magic";
    private static final String RELAY_MAGIC      = "iris-relay-magic";

    public HandshakeExecutor(final ProtocolBase protocol) { super(protocol); }

    public void init(final String cluster) throws IOException {
        protocol.send(OpCode.INIT, () -> {
            protocol.sendString(CLIENT_MAGIC);
            protocol.sendString(PROTOCOL_VERSION);
            protocol.sendString(cluster);
        });
    }

    public String handleInit() throws IOException {
        final OpCode opCode = OpCode.valueOf(protocol.receiveByte());
        switch (opCode) {
            case INIT: {
                verifyMagic();

                final String version = protocol.receiveString();
                return version;
            }
            case DENY: {
                verifyMagic();

                final String reason = protocol.receiveString();
                throw new ProtocolException(String.format("Connection denied: %s!", reason));
            }
            default:
                throw new ProtocolException(String.format("Protocol violation, invalid init response opcode: %s!", opCode));
        }
    }

    private void verifyMagic() throws IOException {
        final String relayMagic = protocol.receiveString();
        if (!RELAY_MAGIC.equals(relayMagic)) {
            throw new ProtocolException(String.format("Protocol violation, invalid relay magic: %s!", relayMagic));
        }
    }
}
