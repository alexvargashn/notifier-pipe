package io.github.alexvargashn.notifierpipe.api;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.github.alexvargashn.notifierpipe.model.Notification;

/**
 * Optional asynchronous extension of {@link NotifierPipe}. Sends notifications on an
 * {@link java.util.concurrent.Executor}; exceptions in async flows complete the future
 * exceptionally with the same {@link NotificationException} types as the sync API.
 * <p>
 * Obtain via {@link NotifierPipes#async(NotifierPipe)} (default executor) or
 * {@link NotifierPipes#async(NotifierPipe, java.util.concurrent.Executor)}, or from
 * {@link NotifierPipeConfig.Builder#buildAsync()}.
 */
public interface AsyncNotifierPipe extends NotifierPipe {

    /**
     * Sends the notification asynchronously. Completes normally on success, or completes
     * exceptionally with {@link ValidationException}, {@link NoProviderForNotificationException},
     * or {@link DeliveryException} on failure.
     *
     * @param notification the notification to send (non-null)
     * @return a future that completes when send finishes (success or failure)
     */
    CompletableFuture<Void> sendAsync(Notification notification);

    /**
     * Sends all notifications asynchronously. Each item is sent via {@link NotifierPipe#trySend(Notification)}
     * on the configured executor; the future completes with one result per notification in order.
     * Exceptions are captured as {@link DeliveryResult.Failure} per item; the future itself
     * does not complete exceptionally.
     *
     * @param notifications list of notifications (non-null; null elements are skipped or treated as errors)
     * @return a future that completes with the list of results in the same order as the input
     */
    CompletableFuture<List<DeliveryResult>> sendBatchAsync(List<Notification> notifications);
}
