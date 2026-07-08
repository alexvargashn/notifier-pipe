package io.github.alexvargashn.notifierpipe;

import io.github.alexvargashn.notifierpipe.api.DeliveryResult;
import io.github.alexvargashn.notifierpipe.api.NoProviderForNotificationException;
import io.github.alexvargashn.notifierpipe.api.NotifierPipe;
import io.github.alexvargashn.notifierpipe.api.NotifierPipeConfig;
import io.github.alexvargashn.notifierpipe.model.Channel;
import io.github.alexvargashn.notifierpipe.model.EmailNotification;
import io.github.alexvargashn.notifierpipe.model.PushNotification;
import io.github.alexvargashn.notifierpipe.model.SmsNotification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link io.github.alexvargashn.notifierpipe.api.ConfiguredNotifierPipe}
 * built via {@link NotifierPipeConfig}: default provider selection per channel.
 */
@DisplayName("ConfiguredNotifierPipe")
class ConfiguredNotifierPipeTest {

    @Test
    @DisplayName("routes to the default provider registered for the channel")
    void usesDefaultProvider() {
        List<EmailNotification> sendgridReceived = StubProviders.newEmailReceived();
        List<EmailNotification> mailgunReceived = StubProviders.newEmailReceived();

        NotifierPipe pipe = NotifierPipeConfig.builder()
                .registerProvider(Channel.EMAIL, "sendgrid",
                        StubProviders.namedEmailStub(sendgridReceived, "SendGrid"))
                .registerProvider(Channel.EMAIL, "mailgun",
                        StubProviders.namedEmailStub(mailgunReceived, "Mailgun"))
                .setDefaultProvider(Channel.EMAIL, "sendgrid")
                .build();

        pipe.send(new EmailNotification("user@example.com", "Hi", "Body"));

        assertEquals(1, sendgridReceived.size());
        assertEquals(0, mailgunReceived.size());
    }

    @Test
    @DisplayName("switching default provider changes which implementation receives the notification")
    void switchesDefaultProvider() {
        List<EmailNotification> sendgridReceived = StubProviders.newEmailReceived();
        List<EmailNotification> mailgunReceived = StubProviders.newEmailReceived();

        NotifierPipe sendgridDefault = NotifierPipeConfig.builder()
                .registerProvider(Channel.EMAIL, "sendgrid",
                        StubProviders.namedEmailStub(sendgridReceived, "SendGrid"))
                .registerProvider(Channel.EMAIL, "mailgun",
                        StubProviders.namedEmailStub(mailgunReceived, "Mailgun"))
                .setDefaultProvider(Channel.EMAIL, "sendgrid")
                .build();

        NotifierPipe mailgunDefault = NotifierPipeConfig.builder()
                .registerProvider(Channel.EMAIL, "sendgrid",
                        StubProviders.namedEmailStub(sendgridReceived, "SendGrid"))
                .registerProvider(Channel.EMAIL, "mailgun",
                        StubProviders.namedEmailStub(mailgunReceived, "Mailgun"))
                .setDefaultProvider(Channel.EMAIL, "mailgun")
                .build();

        EmailNotification notification = new EmailNotification("user@example.com", "Hi", "Body");
        sendgridDefault.send(notification);
        mailgunDefault.send(notification);

        assertEquals(1, sendgridReceived.size());
        assertEquals(1, mailgunReceived.size());
    }

    @Test
    @DisplayName("trySend returns Success with receipt from the default provider")
    void trySendReturnsReceiptFromDefault() {
        List<EmailNotification> received = StubProviders.newEmailReceived();

        NotifierPipe pipe = NotifierPipeConfig.builder()
                .registerProvider(Channel.EMAIL, "sendgrid",
                        StubProviders.namedEmailStub(received, "SendGrid"))
                .setDefaultProvider(Channel.EMAIL, "sendgrid")
                .build();

        DeliveryResult result = pipe.trySend(
                new EmailNotification("user@example.com", "Hi", "Body"));

        assertTrue(result.isSuccess());
        assertInstanceOf(DeliveryResult.Success.class, result);
        assertEquals("SendGrid", ((DeliveryResult.Success) result).receipt().providerName());
    }

    @Test
    @DisplayName("send throws when channel has no registered provider")
    void throwsWhenChannelNotConfigured() {
        NotifierPipe pipe = NotifierPipeConfig.builder()
                .registerProvider(Channel.EMAIL, "sendgrid",
                        StubProviders.emailStub(StubProviders.newEmailReceived()))
                .setDefaultProvider(Channel.EMAIL, "sendgrid")
                .build();

        PushNotification push = new PushNotification("token", "Title", "Body", null);
        NoProviderForNotificationException e = assertThrows(
                NoProviderForNotificationException.class, () -> pipe.send(push));
        assertEquals(push, e.getNotification());
    }

    @Test
    @DisplayName("routes SMS and push through their respective default providers")
    void routesAllConfiguredChannels() {
        List<SmsNotification> smsReceived = StubProviders.newSmsReceived();
        List<PushNotification> pushReceived = StubProviders.newPushReceived();

        NotifierPipe pipe = NotifierPipeConfig.builder()
                .registerProvider(Channel.SMS, "twilio", StubProviders.smsStub(smsReceived))
                .setDefaultProvider(Channel.SMS, "twilio")
                .registerProvider(Channel.PUSH, "fcm", StubProviders.pushStub(pushReceived))
                .setDefaultProvider(Channel.PUSH, "fcm")
                .build();

        pipe.send(new SmsNotification("+15551234567", "Hello"));
        pipe.send(new PushNotification("device-token", "Alert", "Body", null));

        assertEquals(1, smsReceived.size());
        assertEquals(1, pushReceived.size());
    }
}
