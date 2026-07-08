package io.github.alexvargashn.notifierpipe.api;

import io.github.alexvargashn.notifierpipe.model.Notification;

/**
 * Thrown when no provider is registered for the concrete notification type being sent.
 * Allows callers to distinguish "no provider" from other failures and to include
 * the notification type in the message.
 */
public final class NoProviderForNotificationException extends NotificationException {

    private final Notification notification;

    public NoProviderForNotificationException(Notification notification) {
        super("No provider registered for notification type: " + notification.getClass().getSimpleName());
        this.notification = notification;
    }

    /**
     * Returns the notification that could not be sent.
     *
     * @return the notification instance (non-null)
     */
    public Notification getNotification() {
        return notification;
    }
}
