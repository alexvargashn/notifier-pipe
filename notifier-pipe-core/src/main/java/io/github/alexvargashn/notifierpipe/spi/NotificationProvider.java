package io.github.alexvargashn.notifierpipe.spi;

import io.github.alexvargashn.notifierpipe.model.Notification;

/**
 * SPI for sending a single notification type. Each provider declares the concrete
 * notification type it supports and receives only that subtype when sending.
 *
 * @param <N> the notification subtype this provider supports (e.g. {@link io.github.alexvargashn.notifierpipe.model.EmailNotification})
 */
public interface NotificationProvider<N extends Notification> {

    /**
     * Returns the notification type this provider supports. Used by the pipe to route
     * notifications; must match the type parameter {@code N}.
     *
     * @return the supported notification class (non-null)
     */
    Class<N> supportedType();

    /**
     * Sends the given notification. Invoked only with instances of {@code N};
     * the pipe performs type-based routing before calling this method.
     *
     * @param notification the notification to send (non-null, type {@code N})
     * @return a receipt with provider name, message id, and timestamp
     */
    SendReceipt send(N notification);

    /**
     * Type-safe bridge for the pipe: checks that the notification is of the supported
     * type and delegates to {@link #send(Object)}. Implementations should not override this.
     *
     * @param notification any notification (non-null)
     * @return receipt from {@link #send(Object)}
     * @throws IllegalArgumentException if the notification is not an instance of the supported type
     */
    @SuppressWarnings("unchecked")
    default SendReceipt sendNotification(Notification notification) {
        if (!supportedType().isInstance(notification)) {
            throw new IllegalArgumentException(
                    "Provider " + getClass().getSimpleName() + " supports " + supportedType().getSimpleName()
                            + ", but received " + notification.getClass().getSimpleName());
        }
        return send((N) notification);
    }
}
