package com.karalabe.iris;

import org.jetbrains.annotations.NotNull;

// TODO we might want to break these into separate classes instead of an interface with *unrelated* methods
public abstract class Connection2 implements AutoCloseable { // FIXME rename to something meaningful

    /* Broadcasts a message to all applications of type app. No guarantees are made that all recipients receive the message (best effort).
       The call blocks until the message is sent to the relay, returning an iris.Error in case of a failure. */
    public void broadcast(@NotNull final String clusterName, @NotNull byte[] message) throws Exception {
        throw new IllegalStateException("Not implemented!");
    }

    /* Executes a synchronous request to app, load balanced between all the active ones, returning the received reply.
       In case of a failure, the function returns a nil reply with an iris.Error stating the reason.
       The timeout unit is in milliseconds. Setting anything smaller will result in a panic! */
    @NotNull public byte[] request(@NotNull final String clusterName, @NotNull byte[] request, long timeout) throws Exception {
        throw new IllegalStateException("Not implemented!");
    }

    /* Subscribes to topic, using handler as the callback for arriving events.
       The method blocks until the subscription is forwarded to the relay, or an error occurs, in which case an iris.Error is returned.
       Double subscription is considered a programming error and will result in a panic! */
    public void subscribe(@NotNull final String topic, SubscriptionHandler handler) throws Exception {
        throw new IllegalStateException("Not implemented!");
    }

    /* Publishes an event asynchronously to topic. No guarantees are made that all subscribers receive the message (best effort).
       The method does blocks until the message is forwarded to the relay, or an error occurs, in which case an iris.Error is returned. */
    public void publish(@NotNull final String topic, @NotNull byte[] message) throws Exception {
        throw new IllegalStateException("Not implemented!");
    }

    /* Unsubscribes from topic, receiving no more event notifications for it.
       The method does blocks until the unsubscription is forwarded to the relay, or an error occurs, in which case an iris.Error is returned.
       Unsubscribing from a topic not subscribed to is considered a programming error and will result in a panic! */
    public void unsubscribe(@NotNull final String topic) throws Exception {
        throw new IllegalStateException("Not implemented!");
    }

    /* Opens a direct tunnel to an instance of app, allowing pairwise-exclusive and order-guaranteed message passing between them.
       The method blocks until either the newly created tunnel is set up, or an error occurs, in which case a nil tunnel and an iris.Error is returned.
       The timeout unit is in milliseconds. Setting anything smaller will result in a panic! */
    public Tunnel tunnel(@NotNull final String clusterName, long timeout) throws Exception {
        throw new IllegalStateException("Not implemented!");
    }

    /* Gracefully terminates the connection removing all subscriptions and closing all tunnels.
       The call blocks until the connection is torn down or an error occurs.*/
    @Override public void close() throws Exception {}
}