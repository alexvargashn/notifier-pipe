package io.github.alexvargashn.notifierpipe.model;

import java.util.Objects;

/**
 * Email notification payload.
 *
 * @param to      recipient email address (non-null)
 * @param subject email subject (non-null)
 * @param body    email body (non-null)
 */
public record EmailNotification(String to, String subject, String body) implements Notification {

    public EmailNotification {
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(body, "body");
    }

    @Override
    public Channel channel() {
        return Channel.EMAIL;
    }
}
