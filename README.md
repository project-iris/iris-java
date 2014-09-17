  Work in progress!
=====================

  Iris Java binding
===================

This is the official Java language binding for the Iris cloud messaging framework. Version `v1` of the binding is compatible with Iris `v0.3.0` and newer.

If you are unfamiliar with Iris, please read the next introductory section. It contains a short summary, as well as some valuable pointers on where you can discover more.

  Background
-------------------

Iris is an attempt at bringing the simplicity and elegance of cloud computing to the application layer. Consumer clouds provide unlimited virtual machines at the click of a button, but leaves it to developer to wire them together. Iris ensures that you can forget about networking challenges and instead focus on solving your own domain problems.

It is a completely decentralized messaging solution for simplifying the design and implementation of cloud services. Among others, Iris features zero-configuration (i.e. start it up and it will do its magic), semantic addressing (i.e. application use textual names to address each other), clusters as units (i.e. automatic load balancing between apps of the same name) and perfect secrecy (i.e. all network traffic is encrypted).

You can find further infos on the [Iris website](http://iris.karalabe.com) and details of the above features in the [core concepts](http://iris.karalabe.com/book/core_concepts) section of [the book of Iris](http://iris.karalabe.com/book). For the scientifically inclined, a small collection of [papers](http://iris.karalabe.com/papers) is also available featuring Iris. Slides and videos of previously given public presentations are published in the [talks](http://iris.karalabe.com/talks) page.

There is a growing community on Twitter [@iriscmf](https://twitter.com/iriscmf), Google groups [project-iris](https://groups.google.com/group/project-iris) and GitHub [project-iris](https://github.com/project-iris).

  Installation
----------------

To get the package, add the following Maven dependency ([details](http://search.maven.org/#artifactdetails%7Ccom.karalabe.iris%7Ciris%7C1.0.0-preview-3%7Cjar) for various build systems):

 - Group ID: com.karalabe.iris
 - Artifact ID: iris
 - Version: 1.0.0-preview-3

To import this package, add the following line to your code:

    import com.karalabe.iris.*;

  Quickstart
--------------

Iris uses a relaying architecture, where client applications do not communicate directly with one another, but instead delegate all messaging operations to a local relay process responsible for transferring the messages to the correct destinations. The first step hence to using Iris through any binding is setting up the local [_relay_ _node_](http://iris.karalabe.com/downloads). You can find detailed infos in the [Run, Forrest, Run](http://iris.karalabe.com/book/run_forrest_run) section of [the book of Iris](http://iris.karalabe.com/book), but a very simple way would be to start a _developer_ node.

    > iris -dev
    Entering developer mode
    Generating random RSA key... done.
    Generating random network name... done.

    2014/06/13 18:13:47 main: booting iris overlay...
    2014/06/13 18:13:47 scribe: booting with id 369650985814.
    2014/06/13 18:13:57 main: iris overlay converged with 0 remote connections.
    2014/06/13 18:13:57 main: booting relay service...
    2014/06/13 18:13:57 main: iris successfully booted, listening on port 55555.

Since it generates random credentials, a developer node will not be able to connect with other remote nodes in the network. However, it provides a quick solution to start developing without needing to configure a _network_ _name_ and associated _access_ _key_. Should you wish to interconnect multiple nodes, please provide the `-net` and `-rsa` flags.

### Attaching to the relay

After successfully booting, the relay opens a _local_ TCP endpoint (port `55555` by default, configurable using `-port`) through which arbitrarily many entities may attach. Each connecting entity may also decide whether it becomes a simple _client_ only consuming the services provided by other participants, or a full fledged _service_, also making functionality available to others for consumption.

Connecting as a client can be done trivially by invoking [`new Connection`](http://iris.karalabe.com/docs/iris-java.v1/com/karalabe/iris/Connection.html#Connection-int-) with the port number of the local relay's client endpoint. After the attachment is completed, a [`Connection`](http://iris.karalabe.com/docs/iris-java.v1/com/karalabe/iris/Connection.html) instance is returned through which messaging can begin. A client cannot accept inbound requests, broadcasts and tunnels, only initiate them.

```java
// Connect to the local relay (error handling omitted)
final Connection conn = new Connection(55555);

// Disconnect from the local relay
conn.close();
```

To provide functionality for consumption, an entity needs to register as a service. This is slightly more involved, as beside initiating a registration request, it also needs to specify a callback handler to process inbound events. First, the callback handler needs to implement the [`ServiceHandler`](http://iris.karalabe.com/docs/iris-java.v1/com/karalabe/iris/ServiceHandler.html) interface. After creating the handler, registration can commence by invoking [`new Service`](http://iris.karalabe.com/docs/iris-java.v1/com/karalabe/iris/Service.html#Service-int-java.lang.String-com.karalabe.iris.ServiceHandler-) with the port number of the local relay's client endpoint; sub-service cluster this entity will join as a member; handler itself to process inbound messages and an optional resource cap.

```java
// Implement all the methods defined by ServiceHandler (optional, defaults provided)
static class EchoHandler implements ServiceHandler {
    @Override public void init(Connection connection) throws InitializationException { }
    @Override public void handleBroadcast(byte[] message)                            { }
    @Override public byte[] handleRequest(byte[] request) throws RemoteException     { return request; }
    @Override public void handleTunnel(Tunnel tunnel)                                { }
    @Override public void handleDrop(Exception reason)                               { }
}

public static void main(String[] args) throws Exception {
    // Register a new service to the relay (error handling omitted)
    final Service service = new Service(55555, "echo", new EchoHandler());

    // Unregister the service
    service.close();
}
```

Upon successful registration, Iris invokes the handler's `init` method with the live [`Connection`](http://iris.karalabe.com/docs/iris-java.v1/com/karalabe/iris/Connection.html) object - the service's client connection - through which the service itself can initiate outbound requests. The `init` is called only once and is synchronized before any other handler method is invoked.

  Contributions
-----------------

Currently my development aims are to stabilize the project and its language bindings. Hence, although I'm open and very happy for any and all contributions, the most valuable ones are tests, benchmarks and actual binding usage to reach a high enough quality.

Due to the already significant complexity of the project (Iris in general), I kindly ask anyone willing to pinch in to first file an [issue](https://github.com/project-iris/iris-java/issues) with their plans to achieve a best possible integration :).

Additionally, to prevent copyright disputes and such, a signed contributor license agreement is required to be on file before any material can be accepted into the official repositories. These can be filled online via either the [Individual Contributor License Agreement](http://iris.karalabe.com/icla) or the [Corporate Contributor License Agreement](http://iris.karalabe.com/ccla).
