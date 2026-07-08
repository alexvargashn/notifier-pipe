package io.github.alexvargashn.notifierpipe.api;

import io.github.alexvargashn.notifierpipe.model.Notification;
import io.github.alexvargashn.notifierpipe.spi.SendReceipt;

import java.util.Objects;

/**
 * Immutable result of a send attempt via {@link io.github.alexvargashn.notifierpipe.api.NotifierPipe#trySend(Notification)}.
 * Either success (no exception) or failure (with a {@link NotificationException}).
 */
public sealed interface DeliveryResult permits DeliveryResult.Success, DeliveryResult.Failure {

    /**
     * Successful delivery with provider receipt for traceability.
     *
     * @param receipt provider-assigned message id, name, and timestamp (non-null)
     */
    record Success(SendReceipt receipt) implements DeliveryResult {

        public Success {
            Objects.requireNonNull(receipt, "receipt");
        }
    }

    /**
     * Delivery failed; the cause is in {@link #error()}.
     *
     * @param error the exception (non-null)
     */
    record Failure(NotificationException error) implements DeliveryResult {

        public Failure {
            if (error == null) {
                throw new IllegalArgumentException("error must be non-null");
            }
        }
    }

    /**
     * Returns true if this result is success.
     */
    default boolean isSuccess() {
        return this instanceof Success;
    }

    /**
     * Returns true if this result is failure.
     */
    default boolean isFailure() {
        return this instanceof Failure;
    }
}
