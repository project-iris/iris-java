package com.karalabe.iris;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * Internal enumeration for the packet types used.
 */
@SuppressWarnings("MagicNumber") public enum OpCode {
    /*@formatter:off*/
    INIT(0),           /** Connection initialization */
    BROADCAST(1),      /** Application broadcast */
    REQUEST(2),        /** Application request */
    REPLY(3),          /** Application reply */
    SUBSCRIBE(4),      /** Topic subscription */
    PUBLISH(5),        /** Topic publish */
    UNSUBSCRIBE(6),    /** Topic subscription removal */
    CLOSE(7),          /** Connection closing */
    TUNNEL_REQUEST(8), /** Tunnel building request */
    TUNNEL_REPLY(9),   /** Tunnel building reply */
    TUNNEL_DATA(10),   /** Tunnel data transfer */
    TUNNEL_ACK(11),    /** Tunnel data acknowledgment */
    TUNNEL_CLOSE(12);  /** Tunnel closing */
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