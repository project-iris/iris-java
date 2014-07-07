/*
 * Copyright © 2014 Project Iris. All rights reserved.
 *
 * The current language binding is an official support library of the Iris cloud messaging framework, and as such, the same licensing terms apply.
 * For details please see http://iris.karalabe.com/downloads#License
 */

package com.karalabe.iris;

public class ProtocolException extends RuntimeException {
    public ProtocolException(final String message) { super(message); }
    public ProtocolException(final String message, final Throwable cause) { super(message, cause); }
    public ProtocolException(final Throwable cause) { super(cause); }
}
