package io.github.alexvargashn.notifierpipe.providers;

import io.github.alexvargashn.notifierpipe.model.PushNotification;
import io.github.alexvargashn.notifierpipe.spi.NotificationProvider;
import io.github.alexvargashn.notifierpipe.spi.SendReceipt;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Simulated Firebase Cloud Messaging (FCM) push provider. Models the legacy HTTP API
 * (POST https://fcm.googleapis.com/fcm/send) without performing real HTTP calls.
 * The server key is validated at construction and never logged in the clear.
 */
public final class FcmPushProvider implements NotificationProvider<PushNotification> {

    private static final String PROVIDER_NAME = "FCM";

    private final String serverKey;

    /**
     * Creates a provider with the given FCM server key.
     *
     * @param serverKey FCM server key (non-blank)
     * @throws IllegalArgumentException if the server key is null or blank
     */
    public FcmPushProvider(String serverKey) {
        if (serverKey == null || serverKey.isBlank()) {
            throw new IllegalArgumentException("FCM server key must be non-blank");
        }
        this.serverKey = serverKey;
    }

    @Override
    public Class<PushNotification> supportedType() {
        return PushNotification.class;
    }

    @Override
    public SendReceipt send(PushNotification notification) {
        Objects.requireNonNull(notification, "notification");
        String messageId = UUID.randomUUID().toString();
        String dataSummary = notification.data().isEmpty() ? "{}" : notification.data().toString();
        System.out.printf(
                "[FCM] POST /fcm/send Authorization=key=%s to=%s "
                        + "notification.title=%s notification.body=%s data=%s -> 200 OK id=%s%n",
                CredentialMasking.mask(serverKey),
                notification.deviceToken(),
                notification.title(),
                notification.body(),
                dataSummary,
                messageId);
        return new SendReceipt(PROVIDER_NAME, messageId, Instant.now());
    }
}
