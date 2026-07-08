package io.github.alexvargashn.notifierpipe;

import io.github.alexvargashn.notifierpipe.api.DeliveryException;
import io.github.alexvargashn.notifierpipe.api.DeliveryResult;
import io.github.alexvargashn.notifierpipe.api.DefaultNotifierPipe;
import io.github.alexvargashn.notifierpipe.api.NotifierPipe;
import io.github.alexvargashn.notifierpipe.api.RetryPolicy;
import io.github.alexvargashn.notifierpipe.api.RetryableNotifierPipe;
import io.github.alexvargashn.notifierpipe.api.ValidationException;
import io.github.alexvargashn.notifierpipe.model.EmailNotification;
import io.github.alexvargashn.notifierpipe.spi.NotificationProvider;
import io.github.alexvargashn.notifierpipe.spi.SendReceipt;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link RetryableNotifierPipe}: retry on delivery failure only.
 */
@DisplayName("RetryableNotifierPipe")
class RetryableNotifierPipeTest {

    @Test
    @DisplayName("succeeds after transient delivery failures")
    void succeedsAfterRetries() {
        AtomicInteger attempts = new AtomicInteger();
        NotifierPipe delegate = new DefaultNotifierPipe(List.of(failingUntil(attempts, 2)));
        NotifierPipe pipe = new RetryableNotifierPipe(delegate, fastPolicy(3));

        EmailNotification notification = new EmailNotification("user@example.com", "Hi", "Body");
        assertDoesNotThrow(() -> pipe.send(notification));
        assertEquals(3, attempts.get());
    }

    @Test
    @DisplayName("exhausts attempts and throws DeliveryException")
    void exhaustsAttempts() {
        AtomicInteger attempts = new AtomicInteger();
        NotifierPipe delegate = new DefaultNotifierPipe(List.of(failingUntil(attempts, 10)));
        NotifierPipe pipe = new RetryableNotifierPipe(delegate, fastPolicy(3));

        EmailNotification notification = new EmailNotification("user@example.com", "Hi", "Body");
        DeliveryException e = assertThrows(DeliveryException.class, () -> pipe.send(notification));
        assertTrue(e.getMessage().contains("Simulated"));
        assertEquals(3, attempts.get());
    }

    @Test
    @DisplayName("does not retry ValidationException")
    void doesNotRetryValidation() {
        AtomicInteger attempts = new AtomicInteger();
        NotifierPipe delegate = new DefaultNotifierPipe(List.of(failingUntil(attempts, 10)));
        NotifierPipe pipe = new RetryableNotifierPipe(delegate, fastPolicy(3));

        EmailNotification notification = new EmailNotification("not-an-email", "Hi", "Body");
        assertThrows(ValidationException.class, () -> pipe.send(notification));
        assertEquals(0, attempts.get());
    }

    @Test
    @DisplayName("trySend returns Success with receipt after retries")
    void trySendReturnsReceipt() {
        AtomicInteger attempts = new AtomicInteger();
        NotifierPipe delegate = new DefaultNotifierPipe(List.of(failingUntil(attempts, 1)));
        NotifierPipe pipe = new RetryableNotifierPipe(delegate, fastPolicy(3));

        DeliveryResult result = pipe.trySend(new EmailNotification("user@example.com", "Hi", "Body"));
        assertTrue(result.isSuccess());
        assertInstanceOf(DeliveryResult.Success.class, result);
        assertEquals("FlakyStub", ((DeliveryResult.Success) result).receipt().providerName());
        assertEquals(2, attempts.get());
    }

    private static RetryPolicy fastPolicy(int maxAttempts) {
        return new RetryPolicy(maxAttempts, Duration.ofMillis(1), 1.0, Duration.ofMillis(1));
    }

    private static NotificationProvider<EmailNotification> failingUntil(AtomicInteger attempts, int failCount) {
        return new NotificationProvider<EmailNotification>() {
            @Override
            public Class<EmailNotification> supportedType() {
                return EmailNotification.class;
            }

            @Override
            public SendReceipt send(EmailNotification notification) {
                int n = attempts.incrementAndGet();
                if (n <= failCount) {
                    throw new DeliveryException("Simulated transient failure", notification);
                }
                return new SendReceipt("FlakyStub", "msg-" + n, Instant.now());
            }
        };
    }
}
