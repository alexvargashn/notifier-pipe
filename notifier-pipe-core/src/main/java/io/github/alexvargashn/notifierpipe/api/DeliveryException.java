package io.github.alexvargashn.notifierpipe.api;

import io.github.alexvargashn.notifierpipe.model.Notification;

/**
 * Thrown when notification delivery fails (e.g. provider error, network failure, rate limit).
 * Indicates validation passed but the send operation failed.
 *
 * @param notification the notification that could not be delivered (may be null)
 */
public final class DeliveryException extends NotificationException {

    private final Notification notification;

    public DeliveryException(String message) {
        super(message);
        this.notification = null;
    }

    public DeliveryException(String message, Throwable cause) {
        super(message, cause);
        this.notification = null;
    }

    public DeliveryException(String message, Notification notification) {
        super(message);
        this.notification = notification;
    }

    public DeliveryException(String message, Notification notification, Throwable cause) {
        super(message, cause);
        this.notification = notification;
    }

    /**
     * Returns the notification that could not be delivered, or null if not applicable.
     */
    public Notification getNotification() {
        return notification;
    }
}
