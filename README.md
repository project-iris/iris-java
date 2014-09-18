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

Upon successful registration, Iris invokes the handler's [`init`](http://iris.karalabe.com/docs/iris-java.v1/com/karalabe/iris/ServiceHandler.html#init-com.karalabe.iris.Connection-) method with the live [`Connection`](http://iris.karalabe.com/docs/iris-java.v1/com/karalabe/iris/Connection.html) object - the service's client connection - through which the service itself can initiate outbound requests. The [`init`](http://iris.karalabe.com/docs/iris-java.v1/com/karalabe/iris/ServiceHandler.html#init-com.karalabe.iris.Connection-) is called only once and is synchronized before any other handler method is invoked.

### Messaging through Iris

Iris supports four messaging schemes: request/reply, broadcast, tunnel and publish/subscribe. The first three schemes always target a specific cluster: send a request to _one_ member of a cluster and wait for the reply; broadcast a message to _all_ members of a cluster; open a streamed, ordered and throttled communication tunnel to _one_ member of a cluster. The publish/subscribe is similar to broadcast, but _any_ member of the network may subscribe to the same topic, hence breaking cluster boundaries.

<img src="https://dl.dropboxusercontent.com/u/10435909/Iris/messaging_schemes.png" style="height: 175px; display: block; margin-left: auto; margin-right: auto;" \>

Presenting each primitive is out of scope, but for illustrative purposes the request/reply was included. Given the echo service registered above, we can send it requests and wait for replies through any client connection. Iris will automatically locate, route and load balanced between all services registered under the addressed name.

```java
final byte[] request = "Some request binary".getBytes();
try {
    final byte[] reply = conn.request("echo", request, 1000);
    System.out.println("Reply arrived: " + new String(reply));
} catch (TimeoutException | RemoteException e) {
    System.out.println("Failed to execute request: " + e.getMessage());
}
```

### Error handling

The binding uses the idiomatic Java error handling mechanisms of throwing checked exceptions whenever a failure occurs. However, there are a few common cases that need to be individually checkable, hence a few special exception types have been introduced.

Many operations - such as requests and tunnels - can time out. To allow checking for this particular failure, Iris throws a [`TimeoutException`](http://iris.karalabe.com/docs/iris-java.v1/com/karalabe/iris/exceptions/TimeoutException.html) in such scenarios. Similarly, connections, services and tunnels may fail, in the case of which all pending operations terminate with a [`ClosedException`](http://iris.karalabe.com/docs/iris-java.v1/com/karalabe/iris/exceptions/ClosedException.html).

Additionally, the requests/reply pattern supports sending back an error instead of a reply to the caller. To enable the originating node to check whether a request failed locally or remotely, all remote errors are wrapped in a [`RemoteException`](http://iris.karalabe.com/docs/iris-java.v1/com/karalabe/iris/exceptions/RemoteException.html).

```java
try {
    conn.request("echo", request, 1000);
} catch (TimeoutException e) {
    // Request timed out
} catch (ClosedException e) {
    // Connection terminated
} catch (RemoteException e) {
    // Request failed remotely
} catch (IOException e) {
    // Requesting failed locally
}
```

Lastly, if during the initialization of a registered service - while the [`init`](http://iris.karalabe.com/docs/iris-java.v1/com/karalabe/iris/ServiceHandler.html#init-com.karalabe.iris.Connection-) callback is running - the user wishes to abort the registration, he should throw an [`InitializationException`](http://iris.karalabe.com/docs/iris-java.v1/com/karalabe/iris/exceptions/InitializationException.html)

### Resource capping

To prevent the network from overwhelming an attached process, the binding places thread and memory limits on the broadcasts/requests inbound to a registered service as well as on the events received by a topic subscription. The thread limit defines the concurrent processing allowance, whereas the memory limit the maximal length of the pending queue.

The default values - listed below - can be overridden during service registration and topic subscription via [`ServiceLimits`](http://iris.karalabe.com/docs/iris-java.v1/com/karalabe/iris/ServiceLimits.html) and [`iris.TopicLimits`](http://iris.karalabe.com/docs/iris-java.v1/com/karalabe/iris/TopicLimits.html). Any unset fields will default to the preset ones.

```java
// Default limits of the threading and memory usage of a registered service.
public class ServiceLimits {
    public int broadcastThreads = 4 * Runtime.getRuntime().availableProcessors();
    public int broadcastMemory  = 64 * 1024 * 1024;
    public int requestThreads   = 4 * Runtime.getRuntime().availableProcessors();
    public int requestMemory    = 64 * 1024 * 1024;
}

// Default limits of the threading and memory usage of a subscription.
public class TopicLimits {
    public int eventThreads = 4 * Runtime.getRuntime().availableProcessors();
    public int eventMemory  = 64 * 1024 * 1024;
}
```

There is also a sanity limit on the input buffer of a tunnel, but it is not exposed through the API as tunnels are meant as structural primitives, not sensitive to load. This may change in the future.

### Logging

For logging purposes, the Java binding uses the [Simple Logging Facade for Java](http://www.slf4j.org/) (version1.7.7). This library is only an abstract façade defining the API, underneath which every developer can place her own preferred logger framework. For demonstrative purposes, we present the use with the [LOGBack](http://logback.qos.ch/) project, but others are analogous.

The suggested configuration is to collect _INFO_ level logs and print them to _stdout_. This level allows tracking life-cycle events such as client and service attachments, topic subscriptions and tunnel establishments. Further log entries can be requested by lowering the level to _DEBUG_, effectively printing all messages passing through the binding.

```xml
<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} %-5level %-40msg %mdc%n</pattern>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="STDOUT"/>
    </root>
</configuration>
```

Note, that Iris attaches quite a lot of contextual attributes (Mapped Diagnostic Context) to each log entry, enabling detailed tracking of events. These are printed - as can be seen above - with the help of the `%mdc` field in the log output pattern. Additionally, these MDCs are automatically injected into the logger context whenever a callback method of the [`ServiceHandler`](http://iris.karalabe.com/docs/iris-java.v1/com/karalabe/iris/ServiceHandler.html) or [`TopicHandler`](http://iris.karalabe.com/docs/iris-java.v1/com/karalabe/iris/TopicHandler.html) is invoked.

```java
@Override public byte[] handleRequest(byte[] request) throws RemoteException {
    LoggerFactory.getLogger("My Logger").info("My info log entry");
    //...
}
```

```
11:48:49.504 INFO  My info log entry                        remote_request=0, service=1
```

In certain cases, it might be desirable to have access to contextual attributes outside the callback functions (e.g. in the case of a simple client connection, there are no callbacks). For such scenarios, context injection can be specifically requested through an embedded logger (just don't forget to unload afterwards).

```java
try (final Connection conn = new Connection(55555)) {
    final Logger logger = LoggerFactory.getLogger("My Logger");

    conn.logger().loadContext();
    logger.debug("Debug entry, hidden by default");
    logger.info("Info entry, client context included");
    logger.warn("Warning entry");
    logger.error("Critical entry");
    conn.logger().unloadContext();
}
```

As you can see below, all log entries have been automatically tagged with the `client` attribute, set to the id of the current connection. Since the default log level is _INFO_, the `logger.debug` invocation has no effect. Additionally, arbitrarily many key-value pairs may be included in the entry using the [SLF4J MDC API](http://www.slf4j.org/api/org/slf4j/MDC.html).

```
12:20:14.654 INFO  Connecting new client                    client=1, relay_port=55555
12:20:14.691 INFO  Client connection established            client=1
12:20:14.692 INFO  Info entry, client context included      client=1
12:20:14.692 WARN  Warning entry                            client=1
12:20:14.692 ERROR Critical entry                           client=1
12:20:14.692 INFO  Detaching from relay                     client=1
12:20:14.692 INFO  Successfully detached                    client=1
```

### Additional goodies

You can find a teaser presentation, touching on all the key features of the library through a handful of challenges and their solutions. The recommended version is the [playground](http://play.iris.karalabe.com/talks/binds/java.v1.slide), containing modifiable and executable code snippets, but a [read only](http://iris.karalabe.com/talks/binds/java.v1.slide) one is also available.

  Contributions
-----------------

Currently my development aims are to stabilize the project and its language bindings. Hence, although I'm open and very happy for any and all contributions, the most valuable ones are tests, benchmarks and actual binding usage to reach a high enough quality.

Due to the already significant complexity of the project (Iris in general), I kindly ask anyone willing to pinch in to first file an [issue](https://github.com/project-iris/iris-java/issues) with their plans to achieve a best possible integration :).

Additionally, to prevent copyright disputes and such, a signed contributor license agreement is required to be on file before any material can be accepted into the official repositories. These can be filled online via either the [Individual Contributor License Agreement](http://iris.karalabe.com/icla) or the [Corporate Contributor License Agreement](http://iris.karalabe.com/ccla).
