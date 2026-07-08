package io.github.alexvargashn.notifierpipe.providers;

import io.github.alexvargashn.notifierpipe.model.EmailNotification;
import io.github.alexvargashn.notifierpipe.spi.NotificationProvider;
import io.github.alexvargashn.notifierpipe.spi.SendReceipt;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Simulated SendGrid email provider. Models SendGrid's v3 Mail Send API
 * (POST /v3/mail/send) without performing real HTTP calls. The API key is
 * validated at construction and never logged in the clear.
 */
public final class SendGridEmailProvider implements NotificationProvider<EmailNotification> {

    private static final String PROVIDER_NAME = "SendGrid";

    private final String apiKey;

    /**
     * Creates a provider with the given SendGrid API key.
     *
     * @param apiKey SendGrid API key (non-blank)
     * @throws IllegalArgumentException if the API key is null or blank
     */
    public SendGridEmailProvider(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("SendGrid API key must be non-blank");
        }
        this.apiKey = apiKey;
    }

    @Override
    public Class<EmailNotification> supportedType() {
        return EmailNotification.class;
    }

    @Override
    public SendReceipt send(EmailNotification notification) {
        Objects.requireNonNull(notification, "notification");
        String messageId = "SG-" + UUID.randomUUID();
        System.out.printf(
                "[SendGrid] POST /v3/mail/send auth=Bearer %s to=%s subject=%s -> 202 Accepted id=%s%n",
                CredentialMasking.mask(apiKey), notification.to(), notification.subject(), messageId);
        return new SendReceipt(PROVIDER_NAME, messageId, Instant.now());
    }
}
