package io.github.alexvargashn.notifierpipe.api;

import io.github.alexvargashn.notifierpipe.model.Notification;

/**
 * Thrown when a notification fails validation before being sent (e.g. invalid email format,
 * invalid phone number). Indicates the payload is invalid; no delivery was attempted.
 *
 * @param notification the notification that failed validation (may be null if validation failed before construction)
 */
public final class ValidationException extends NotificationException {

    private final Notification notification;

    public ValidationException(String message) {
        super(message);
        this.notification = null;
    }

    public ValidationException(String message, Notification notification) {
        super(message);
        this.notification = notification;
    }

    /**
     * Returns the notification that failed validation, or null if not applicable.
     */
    public Notification getNotification() {
        return notification;
    }
}
