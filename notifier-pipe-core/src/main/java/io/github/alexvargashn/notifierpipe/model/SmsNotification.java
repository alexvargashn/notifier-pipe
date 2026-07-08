package io.github.alexvargashn.notifierpipe.model;

import java.util.Objects;

/**
 * SMS notification payload.
 *
 * @param phoneNumber recipient phone number (non-null)
 * @param message     SMS text (non-null)
 */
public record SmsNotification(String phoneNumber, String message) implements Notification {

    public SmsNotification {
        Objects.requireNonNull(phoneNumber, "phoneNumber");
        Objects.requireNonNull(message, "message");
    }

    @Override
    public Channel channel() {
        return Channel.SMS;
    }
}
