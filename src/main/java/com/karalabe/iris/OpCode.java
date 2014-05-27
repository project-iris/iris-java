package com.karalabe.iris;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * Internal enumeration for the packet types used.
 */
@SuppressWarnings("MagicNumber") public enum OpCode {
    /*@formatter:off*/
    INIT (0),         /** Connection initialization */
    DENY (1),         /** Connection ???*/
    CLOSE(2),         /** Connection closing */

    BROADCAST(3),     /** Application broadcast */
    REQUEST  (4),     /** Application request */
    REPLY    (5),     /** Application reply */

    SUBSCRIBE  (6),   /** Topic subscription */
    UNSUBSCRIBE(7),   /** Topic subscription removal */
    PUBLISH    (8),   /** Topic publish */

    TUN_BUILD   (9),  /** Tunnel ???*/
    TUN_CONFIRM (10), /** Tunnel ???*/
    TUN_ALLOW   (11), /** Tunnel ???*/
    TUN_TRANSFER(12), /** Tunnel ???*/
    TUN_CLOSE   (13); /** Tunnel closing */
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