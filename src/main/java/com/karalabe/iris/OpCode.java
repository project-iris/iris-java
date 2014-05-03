package com.karalabe.iris;

/**
 * Internal enumeration for the packet types used.
 */
/*@formatter:off*/
@SuppressWarnings("MagicNumber")
enum OpCode {
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

    private final byte ordinal;

    OpCode(int ordinal) {
        this.ordinal = (byte) ordinal;
    }

    public byte getOrdinal() {
        return ordinal;
    }
}