package io.github.alexvargashn.notifierpipe;

import io.github.alexvargashn.notifierpipe.api.DefaultNotifierPipe;
import io.github.alexvargashn.notifierpipe.api.DeliveryException;
import io.github.alexvargashn.notifierpipe.api.DeliveryResult;
import io.github.alexvargashn.notifierpipe.api.NoProviderForNotificationException;
import io.github.alexvargashn.notifierpipe.api.NotifierPipe;
import io.github.alexvargashn.notifierpipe.api.ValidationException;
import io.github.alexvargashn.notifierpipe.model.EmailNotification;
import io.github.alexvargashn.notifierpipe.model.PushNotification;
import io.github.alexvargashn.notifierpipe.model.SmsNotification;
import io.github.alexvargashn.notifierpipe.spi.NotificationProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for DefaultNotifierPipe: routing, validation, and delivery failure handling.
 */
@DisplayName("DefaultNotifierPipe")
class DefaultNotifierPipeTest {

    private List<EmailNotification> emailReceived;
    private List<SmsNotification> smsReceived;
    private List<PushNotification> pushReceived;
    private NotifierPipe pipe;

    @BeforeEach
    void setUp() {
        emailReceived = StubProviders.newEmailReceived();
        smsReceived = StubProviders.newSmsReceived();
        pushReceived = StubProviders.newPushReceived();
        pipe = new DefaultNotifierPipe(List.of(
                StubProviders.emailStub(emailReceived),
                StubProviders.smsStub(smsReceived),
                StubProviders.pushStub(pushReceived)
        ));
    }

    @Nested
    @DisplayName("routing")
    class Routing {

        @Test
        @DisplayName("routes EmailNotification to email provider")
        void routesEmail() {
            EmailNotification n = new EmailNotification("user@example.com", "Subject", "Body");
            pipe.send(n);
            assertEquals(1, emailReceived.size());
            assertEquals("user@example.com", emailReceived.get(0).to());
            assertEquals("Subject", emailReceived.get(0).subject());
            assertEquals(0, smsReceived.size());
            assertEquals(0, pushReceived.size());
        }

        @Test
        @DisplayName("routes SmsNotification to SMS provider")
        void routesSms() {
            SmsNotification n = new SmsNotification("+15551234567", "Hello");
            pipe.send(n);
            assertEquals(1, smsReceived.size());
            assertEquals("+15551234567", smsReceived.get(0).phoneNumber());
            assertEquals("Hello", smsReceived.get(0).message());
            assertEquals(0, emailReceived.size());
            assertEquals(0, pushReceived.size());
        }

        @Test
        @DisplayName("routes PushNotification to push provider")
        void routesPush() {
            PushNotification n = new PushNotification("token-1", "Title", "Body", null);
            pipe.send(n);
            assertEquals(1, pushReceived.size());
            assertEquals("token-1", pushReceived.get(0).deviceToken());
            assertEquals("Title", pushReceived.get(0).title());
            assertEquals(0, emailReceived.size());
            assertEquals(0, smsReceived.size());
        }

        @Test
        @DisplayName("trySend returns Success when routing succeeds")
        void trySendReturnsSuccess() {
            EmailNotification n = new EmailNotification("a@b.co", "Subj", "Body");
            DeliveryResult result = pipe.trySend(n);
            assertTrue(result.isSuccess());
            assertInstanceOf(DeliveryResult.Success.class, result);
            assertEquals("EmailStub", ((DeliveryResult.Success) result).receipt().providerName());
            assertEquals(1, emailReceived.size());
        }
    }

    @Nested
    @DisplayName("validation errors")
    class ValidationErrors {

        @Test
        @DisplayName("send throws ValidationException for invalid email format")
        void invalidEmailFormat() {
            EmailNotification n = new EmailNotification("not-an-email", "Subject", "Body");
            ValidationException e = assertThrows(ValidationException.class, () -> pipe.send(n));
            assertTrue(e.getMessage().contains("Invalid email format"));
            assertEquals(n, e.getNotification());
            assertEquals(0, emailReceived.size());
        }

        @Test
        @DisplayName("send throws ValidationException for blank email subject")
        void blankEmailSubject() {
            EmailNotification n = new EmailNotification("user@example.com", "  ", "Body");
            ValidationException e = assertThrows(ValidationException.class, () -> pipe.send(n));
            assertTrue(e.getMessage().contains("subject"));
            assertEquals(0, emailReceived.size());
        }

        @Test
        @DisplayName("send throws ValidationException for invalid phone number")
        void invalidPhoneNumber() {
            SmsNotification n = new SmsNotification("short", "Message");
            ValidationException e = assertThrows(ValidationException.class, () -> pipe.send(n));
            assertTrue(e.getMessage().contains("phone") || e.getMessage().contains("Invalid"));
            assertEquals(n, e.getNotification());
            assertEquals(0, smsReceived.size());
        }

        @Test
        @DisplayName("send throws ValidationException for blank push device token")
        void blankPushToken() {
            PushNotification n = new PushNotification("  ", "Title", "Body", null);
            ValidationException e = assertThrows(ValidationException.class, () -> pipe.send(n));
            assertTrue(e.getMessage().contains("device token"));
            assertEquals(0, pushReceived.size());
        }

        @Test
        @DisplayName("trySend returns Failure(ValidationException) for invalid email")
        void trySendReturnsFailureForValidation() {
            EmailNotification n = new EmailNotification("bad", "Subj", "Body");
            DeliveryResult result = pipe.trySend(n);
            assertTrue(result.isFailure());
            assertInstanceOf(DeliveryResult.Failure.class, result);
            assertInstanceOf(ValidationException.class, ((DeliveryResult.Failure) result).error());
            assertEquals(0, emailReceived.size());
        }
    }

    @Nested
    @DisplayName("delivery failures")
    class DeliveryFailures {

        @Test
        @DisplayName("send throws DeliveryException when provider fails")
        void sendThrowsDeliveryException() {
            NotifierPipe failingPipe = new DefaultNotifierPipe(List.of(
                    StubProviders.failingEmailStub(),
                    StubProviders.smsStub(smsReceived),
                    StubProviders.pushStub(pushReceived)
            ));
            EmailNotification n = new EmailNotification("user@example.com", "Subj", "Body");
            DeliveryException e = assertThrows(DeliveryException.class, () -> failingPipe.send(n));
            assertTrue(e.getMessage().contains("Simulated") || e.getMessage().contains("Delivery failed"));
            assertEquals(n, e.getNotification());
        }

        @Test
        @DisplayName("trySend returns Failure(DeliveryException) when provider fails")
        void trySendReturnsFailureForDelivery() {
            NotifierPipe failingPipe = new DefaultNotifierPipe(List.of(
                    StubProviders.failingEmailStub(),
                    StubProviders.smsStub(smsReceived),
                    StubProviders.pushStub(pushReceived)
            ));
            EmailNotification n = new EmailNotification("user@example.com", "Subj", "Body");
            DeliveryResult result = failingPipe.trySend(n);
            assertTrue(result.isFailure());
            assertInstanceOf(DeliveryException.class, ((DeliveryResult.Failure) result).error());
        }
    }

    @Nested
    @DisplayName("no provider")
    class NoProvider {

        @Test
        @DisplayName("send throws NoProviderForNotificationException when no provider for type")
        void sendThrowsWhenNoProvider() {
            NotifierPipe emailAndSmsOnly = new DefaultNotifierPipe(List.of(
                    StubProviders.emailStub(emailReceived),
                    StubProviders.smsStub(smsReceived)
            ));
            PushNotification n = new PushNotification("token", "T", "B", null);
            NoProviderForNotificationException e = assertThrows(NoProviderForNotificationException.class,
                    () -> emailAndSmsOnly.send(n));
            assertEquals(n, e.getNotification());
            assertTrue(e.getMessage().contains("PushNotification"));
        }

        @Test
        @DisplayName("trySend returns Failure(NoProviderForNotificationException) when no provider")
        void trySendReturnsFailureWhenNoProvider() {
            NotifierPipe emailAndSmsOnly = new DefaultNotifierPipe(List.of(
                    StubProviders.emailStub(emailReceived),
                    StubProviders.smsStub(smsReceived)
            ));
            PushNotification n = new PushNotification("token", "T", "B", null);
            DeliveryResult result = emailAndSmsOnly.trySend(n);
            assertTrue(result.isFailure());
            assertInstanceOf(NoProviderForNotificationException.class, ((DeliveryResult.Failure) result).error());
        }
    }
}
