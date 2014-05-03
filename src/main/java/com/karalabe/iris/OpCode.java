package com.karalabe.iris;

/**
 * Internal enumeration for the packet types used.
 */
/*@formatter:off*/
enum OpCode {
    INIT((byte)0),           /** Connection initialization */
    BROADCAST((byte)1),      /** Application broadcast */
    REQUEST((byte)2),        /** Application request */
    REPLY((byte)3),          /** Application reply */
    SUBSCRIBE((byte)4),      /** Topic subscription */
    PUBLISH((byte)5),        /** Topic publish */
    UNSUBSCRIBE((byte)6),    /** Topic subscription removal */
    CLOSE((byte)7),          /** Connection closing */
    TUNNEL_REQUEST((byte)8), /** Tunnel building request */
    TUNNEL_REPLY((byte)9),   /** Tunnel building reply */
    TUNNEL_DATA((byte)10),   /** Tunnel data transfer */
    TUNNEL_ACK((byte)11),    /** Tunnel data acknowledgment */
    TUNNEL_CLOSE((byte)12);  /** Tunnel closing */

    private final byte ordinal;

    private OpCode(byte ordinal) {
        this.ordinal = ordinal;
    }

    public byte getOrdinal() {
        return ordinal;
    }
}
