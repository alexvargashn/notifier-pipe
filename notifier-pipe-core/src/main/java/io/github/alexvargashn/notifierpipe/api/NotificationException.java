package io.github.alexvargashn.notifierpipe.api;

/**
 * Base exception for all notification-related errors. Subtypes distinguish between
 * validation failures (invalid payload before sending) and delivery failures
 * (error while sending or from the provider).
 */
public abstract class NotificationException extends RuntimeException {

    protected NotificationException(String message) {
        super(message);
    }

    protected NotificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
