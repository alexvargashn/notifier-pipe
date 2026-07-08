package io.github.alexvargashn.notifierpipe;

import io.github.alexvargashn.notifierpipe.api.DeliveryException;
import io.github.alexvargashn.notifierpipe.model.EmailNotification;
import io.github.alexvargashn.notifierpipe.model.Notification;
import io.github.alexvargashn.notifierpipe.model.PushNotification;
import io.github.alexvargashn.notifierpipe.model.SmsNotification;
import io.github.alexvargashn.notifierpipe.spi.NotificationProvider;
import io.github.alexvargashn.notifierpipe.spi.SendReceipt;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Stub providers for unit tests. Record received notifications; no real network calls.
 */
final class StubProviders {

    private StubProviders() {
    }

    /** Records each EmailNotification sent. */
    static NotificationProvider<EmailNotification> emailStub(List<EmailNotification> received) {
        return new NotificationProvider<>() {
            @Override
            public Class<EmailNotification> supportedType() {
                return EmailNotification.class;
            }

            @Override
            public SendReceipt send(EmailNotification notification) {
                received.add(notification);
                return stubReceipt("EmailStub");
            }
        };
    }

    /** Records each SmsNotification sent. */
    static NotificationProvider<SmsNotification> smsStub(List<SmsNotification> received) {
        return new NotificationProvider<>() {
            @Override
            public Class<SmsNotification> supportedType() {
                return SmsNotification.class;
            }

            @Override
            public SendReceipt send(SmsNotification notification) {
                received.add(notification);
                return stubReceipt("SmsStub");
            }
        };
    }

    /** Records each PushNotification sent. */
    static NotificationProvider<PushNotification> pushStub(List<PushNotification> received) {
        return new NotificationProvider<>() {
            @Override
            public Class<PushNotification> supportedType() {
                return PushNotification.class;
            }

            @Override
            public SendReceipt send(PushNotification notification) {
                received.add(notification);
                return stubReceipt("PushStub");
            }
        };
    }

    /** Records each EmailNotification sent with a custom provider name in the receipt. */
    static NotificationProvider<EmailNotification> namedEmailStub(
            List<EmailNotification> received, String providerName) {
        return new NotificationProvider<EmailNotification>() {
            @Override
            public Class<EmailNotification> supportedType() {
                return EmailNotification.class;
            }

            @Override
            public SendReceipt send(EmailNotification notification) {
                received.add(notification);
                return stubReceipt(providerName);
            }
        };
    }

    /** Fails every send with a DeliveryException (simulates delivery failure). */
    static NotificationProvider<EmailNotification> failingEmailStub() {
        return new NotificationProvider<>() {
            @Override
            public Class<EmailNotification> supportedType() {
                return EmailNotification.class;
            }

            @Override
            public SendReceipt send(EmailNotification notification) {
                throw new DeliveryException("Simulated delivery failure", notification);
            }
        };
    }

    /** Returns a new mutable list; use with emailStub(received). */
    static List<EmailNotification> newEmailReceived() {
        return new ArrayList<>();
    }

    static List<SmsNotification> newSmsReceived() {
        return new ArrayList<>();
    }

    static List<PushNotification> newPushReceived() {
        return new ArrayList<>();
    }

    private static SendReceipt stubReceipt(String providerName) {
        return new SendReceipt(providerName, "stub-" + UUID.randomUUID(), Instant.now());
    }
}
