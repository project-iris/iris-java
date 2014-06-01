// Copyright (c) 2014 Project Iris. All rights reserved.
//
// The current language binding is an official support library of the Iris
// cloud messaging framework, and as such, the same licensing terms apply.
// For details please see http://iris.karalabe.com/downloads#License
package com.karalabe.iris.protocol;

import java.util.Arrays;
import java.util.stream.Stream;

// Relay protocol packet opcodes.
@SuppressWarnings("MagicNumber") public enum OpCode {
    /*@formatter:off*/
    INIT (0x00),           // Out: connection initiation            | In: connection acceptance
    DENY (0x01),           // Out: <never sent>                     | In: connection refusal
    CLOSE(0x02),           // Out: connection tear-down initiation  | In: connection tear-down notification

    BROADCAST(0x03),       // Out: application broadcast initiation | In: application broadcast delivery
    REQUEST  (0x04),       // Out: application request initiation   | In: application request delivery
    REPLY    (0x05),       // Out: application reply initiation     | In: application reply delivery

    SUBSCRIBE  (0x06),     // Out: topic subscription               | In: <never received>
    UNSUBSCRIBE(0x07),     // Out: topic subscription removal       | In: <never received>
    PUBLISH    (0x08),     // Out: topic event publish              | In: topic event delivery

    TUNNEL_BUILD   (0x09), // Out: tunnel construction request      | In: tunnel initiation
    TUNNEL_CONFIRM (0x0a), // Out: tunnel confirmation              | In: tunnel construction result
    TUNNEL_ALLOW   (0x0b), // Out: tunnel transfer allowance        | In: <same as out>
    TUNNEL_TRANSFER(0x0c), // Out: tunnel data exchange             | In: <same as out>
    TUNNEL_CLOSE   (0x0d); // Out: tunnel termination request       | In: tunnel termination notification
    /*@formatter:on*/

    private final byte ordinal;

    OpCode(int ordinal) {
        this.ordinal = (byte) ordinal;
    }

    public byte getOrdinal() {
        return ordinal;
    }

    public static OpCode valueOf(int opCodeOrdinal) {
        try (final Stream<OpCode> stream = Arrays.stream(values())) {
            return stream.filter(opCode -> (opCode.ordinal == opCodeOrdinal)).findFirst()
                         .orElseThrow(() -> new IllegalArgumentException(String.format("No %s found for %d!", OpCode.class.getSimpleName(), opCodeOrdinal)));
        }
    }
}