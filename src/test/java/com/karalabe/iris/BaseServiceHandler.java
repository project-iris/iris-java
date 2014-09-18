// Copyright (c) 2014 Project Iris. All rights reserved.
//
// The current language binding is an official support library of the Iris
// cloud messaging framework, and as such, the same licensing terms apply.
// For details please see http://iris.karalabe.com/downloads#License
package com.karalabe.iris;

// Abstract wrapper around the ServiceHandler interface to store the connection.
public class BaseServiceHandler implements ServiceHandler {
    public Connection connection;

    @Override public void init(final Connection connection) {
        this.connection = connection;
    }
}
