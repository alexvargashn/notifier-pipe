package io.github.alexvargashn.notifierpipe.spi;

import java.time.Instant;
import java.util.Objects;

/**
 * Receipt returned by a provider after a successful simulated send.
 * Carries traceability metadata (provider name, message id, timestamp).
 *
 * @param providerName human-readable provider identifier (non-null)
 * @param messageId    provider-assigned message identifier (non-null)
 * @param sentAt       timestamp when the send was accepted (non-null)
 */
public record SendReceipt(String providerName, String messageId, Instant sentAt) {

    public SendReceipt {
        Objects.requireNonNull(providerName, "providerName");
        Objects.requireNonNull(messageId, "messageId");
        Objects.requireNonNull(sentAt, "sentAt");
    }
}
