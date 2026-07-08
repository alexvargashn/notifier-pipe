package io.github.alexvargashn.notifierpipe.providers;

import io.github.alexvargashn.notifierpipe.model.SmsNotification;
import io.github.alexvargashn.notifierpipe.spi.NotificationProvider;
import io.github.alexvargashn.notifierpipe.spi.SendReceipt;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Simulated Twilio SMS provider. Models Twilio's Messages API
 * (POST /2010-04-01/Accounts/{Sid}/Messages.json) without performing real HTTP calls.
 * Credentials are validated at construction and never logged in the clear.
 */
public final class TwilioSmsProvider implements NotificationProvider<SmsNotification> {

    private static final String PROVIDER_NAME = "Twilio";

    private final String accountSid;
    private final String authToken;
    private final String fromNumber;

    /**
     * Creates a provider with Twilio account credentials and a sender number.
     *
     * @param accountSid Twilio account SID (non-blank)
     * @param authToken  Twilio auth token (non-blank)
     * @param fromNumber sender phone number in E.164 format (non-blank)
     * @throws IllegalArgumentException if any credential is null or blank
     */
    public TwilioSmsProvider(String accountSid, String authToken, String fromNumber) {
        if (accountSid == null || accountSid.isBlank()) {
            throw new IllegalArgumentException("Twilio account SID must be non-blank");
        }
        if (authToken == null || authToken.isBlank()) {
            throw new IllegalArgumentException("Twilio auth token must be non-blank");
        }
        if (fromNumber == null || fromNumber.isBlank()) {
            throw new IllegalArgumentException("Twilio from number must be non-blank");
        }
        this.accountSid = accountSid;
        this.authToken = authToken;
        this.fromNumber = fromNumber;
    }

    @Override
    public Class<SmsNotification> supportedType() {
        return SmsNotification.class;
    }

    @Override
    public SendReceipt send(SmsNotification notification) {
        Objects.requireNonNull(notification, "notification");
        String messageId = "SM" + UUID.randomUUID().toString().replace("-", "");
        System.out.printf(
                "[Twilio] POST /2010-04-01/Accounts/%s/Messages.json auth=%s:%s "
                        + "To=%s From=%s Body=%s -> 201 Created sid=%s%n",
                accountSid,
                CredentialMasking.mask(accountSid),
                CredentialMasking.mask(authToken),
                notification.phoneNumber(),
                fromNumber,
                notification.message(),
                messageId);
        return new SendReceipt(PROVIDER_NAME, messageId, Instant.now());
    }
}
