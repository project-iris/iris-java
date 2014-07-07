/*
 * Copyright © 2014 Project Iris. All rights reserved.
 *
 * The current language binding is an official support library of the Iris cloud messaging framework, and as such, the same licensing terms apply.
 * For details please see http://iris.karalabe.com/downloads#License
 */

package com.karalabe.iris.protocol;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/** Relay protocol packet opcodes. */
@SuppressWarnings("MagicNumber") public enum OpCode {
    /*@formatter:off*/
    INIT (0x0),           /** Out: connection initiation            | In: connection acceptance */
    DENY (0x1),           /** Out: <never sent>                     | In: connection refusal */
    CLOSE(0x2),           /** Out: connection tear-down initiation  | In: connection tear-down notification */

    BROADCAST(0x3),       /** Out: application broadcast initiation | In: application broadcast delivery */
    REQUEST  (0x4),       /** Out: application request initiation   | In: application request delivery */
    REPLY    (0x5),       /** Out: application reply initiation     | In: application reply delivery */

    SUBSCRIBE  (0x6),     /** Out: topic subscription               | In: <never received> */
    UNSUBSCRIBE(0x7),     /** Out: topic subscription removal       | In: <never received> */
    PUBLISH    (0x8),     /** Out: topic event publish              | In: topic event delivery */

    TUNNEL_BUILD   (0x9), /** Out: tunnel construction request      | In: tunnel initiation */
    TUNNEL_CONFIRM (0xa), /** Out: tunnel confirmation              | In: tunnel construction result */
    TUNNEL_ALLOW   (0xb), /** Out: tunnel transfer allowance        | In: <same as out> */
    TUNNEL_TRANSFER(0xc), /** Out: tunnel data exchange             | In: <same as out> */
    TUNNEL_CLOSE   (0xd); /** Out: tunnel termination request       | In: tunnel termination notification */
    /*@formatter:on*/

    private final byte ordinal;

    OpCode(int ordinal) { this.ordinal = (byte) ordinal; }

    public byte getOrdinal() { return ordinal; }

    private static final Map<Integer, OpCode> OPCODE_CACHE = new ConcurrentHashMap<>(values().length); // TODO trove4j would be even faster here

    public static OpCode valueOf(int opCodeOrdinal) {
        return OPCODE_CACHE.computeIfAbsent(opCodeOrdinal, o -> {
            try (final Stream<OpCode> stream = Arrays.stream(values())) {
                return stream.filter(opCode -> (opCode.ordinal == opCodeOrdinal)).findFirst()
                             .orElseThrow(() -> new IllegalArgumentException(String.format("No %s found for %d!", OpCode.class.getSimpleName(), opCodeOrdinal)));
            }
        });
    }
}