/*
 * Copyright © 2014 Project Iris. All rights reserved.
 *
 * The current language binding is an official support library of the Iris cloud messaging framework, and as such, the same licensing terms apply.
 * For details please see http://iris.karalabe.com/downloads#License
 */

package com.karalabe.iris;

public class ServiceLimits {
    // Broadcast handlers to execute concurrently
    public int broadcastThreads = 4 * Runtime.getRuntime().availableProcessors();

    // Memory allowance for pending broadcasts
    public int broadcastMemory = 64 * 1024 * 1024;

    // Request handlers to execute concurrently
    public int requestThreads = 4 * Runtime.getRuntime().availableProcessors();

    // Memory allowance for pending requests
    public int requestMemory = 64 * 1024 * 1024;
}
