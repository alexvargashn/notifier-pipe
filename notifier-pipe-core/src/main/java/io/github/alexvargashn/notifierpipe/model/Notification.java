package io.github.alexvargashn.notifierpipe.model;

/**
 * Sealed interface representing a notification that can be sent through a channel.
 * Channel is derived from the concrete type (Email, SMS, Push); callers do not pass it manually.
 * Permitted types: {@link EmailNotification}, {@link SmsNotification}, {@link PushNotification}.
 */
public sealed interface Notification
        permits EmailNotification, SmsNotification, PushNotification {

    /**
     * Returns the channel for this notification. Derived from the notification type.
     *
     * @return the channel (EMAIL, SMS, or PUSH)
     */
    Channel channel();
}
