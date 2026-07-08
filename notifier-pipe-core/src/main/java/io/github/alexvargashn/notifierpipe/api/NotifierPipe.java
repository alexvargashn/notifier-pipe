package io.github.alexvargashn.notifierpipe.api;

import io.github.alexvargashn.notifierpipe.model.Notification;

/**
 * Entry point for sending notifications. Supports two styles: throwing on error or returning a result.
 *
 * @throws NotificationException (and subtypes) from {@link #send(Notification)} on validation
 *         or delivery failure, or when no provider is registered
 */
public interface NotifierPipe {

    /**
     * Validates and sends the notification. Throws on validation failure, missing provider, or delivery failure.
     *
     * @param notification the notification to send (non-null)
     * @throws ValidationException if the notification payload is invalid
     * @throws NoProviderForNotificationException if no provider is registered for the notification type
     * @throws DeliveryException if delivery fails
     */
    void send(Notification notification);

    /**
     * Validates and sends the notification. Returns a result instead of throwing; use for flow control without exceptions.
     *
     * @param notification the notification to send (non-null)
     * @return {@link DeliveryResult.Success} on success, {@link DeliveryResult.Failure} with the exception on failure
     */
    DeliveryResult trySend(Notification notification);
}
