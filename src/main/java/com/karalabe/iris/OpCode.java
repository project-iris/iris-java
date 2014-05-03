package com.karalabe.iris;

/**
 * Internal enumeration for the packet types used.
 * @author Robert Roth
 */
enum OpCode {
	/** Connection initialization */
	INIT(0),
	/** Application broadcast */
	BROADCAST (1),
	/** Application request */
	REQUEST(2),
	/** Application reply */
	REPLY(3),
	/** Topic subscription */
	SUBSCRIBE(4),
	/** Topic publish */
	PUBLISH(5),
	/** Topic subscription removal */
	UNSUBSCRIBE(6),
	/** Connection closing */
	CLOSE(7),
	/** Tunnel building request */
	TUNNEL_REQUEST(8),
	/** Tunnel building reply */
	TUNNEL_REPLY(9),
	/** Tunnel data transfer */
	TUNNEL_DATA(10),
	/** Tunnel data acknowledgment */
	TUNNEL_ACK(11),
	/** Tunnel closing */
	TUNNEL_CLOSE(12);
	
	int ordinal;
	OpCode(int ordinal) {
		this.ordinal = ordinal;
	}
}
