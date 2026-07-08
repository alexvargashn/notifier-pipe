package io.github.alexvargashn.notifierpipe.api;

import io.github.alexvargashn.notifierpipe.model.Notification;

import java.util.Objects;

/**
 * Decorator that adds retry-with-backoff to any {@link NotifierPipe}.
 * Only {@link DeliveryException} is retried; validation and missing-provider
 * errors are deterministic and fail fast.
 */
public final class RetryableNotifierPipe implements NotifierPipe {

    private final NotifierPipe delegate;
    private final RetryPolicy policy;

    /**
     * Wraps the given pipe with retry behavior according to the policy.
     *
     * @param delegate the underlying pipe (non-null)
     * @param policy   retry configuration (non-null)
     */
    public RetryableNotifierPipe(NotifierPipe delegate, RetryPolicy policy) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.policy = Objects.requireNonNull(policy, "policy");
    }

    @Override
    public void send(Notification notification) {
        DeliveryResult result = trySend(notification);
        if (result.isFailure()) {
            throw ((DeliveryResult.Failure) result).error();
        }
    }

    @Override
    public DeliveryResult trySend(Notification notification) {
        Objects.requireNonNull(notification, "notification");
        DeliveryException last = null;
        for (int attempt = 1; attempt <= policy.maxAttempts(); attempt++) {
            DeliveryResult result = delegate.trySend(notification);
            if (result.isSuccess()) {
                return result;
            }
            NotificationException error = ((DeliveryResult.Failure) result).error();
            if (error instanceof ValidationException || error instanceof NoProviderForNotificationException) {
                return result;
            }
            if (error instanceof DeliveryException deliveryError) {
                last = deliveryError;
                if (attempt < policy.maxAttempts()) {
                    sleep(policy.delayForRetry(attempt).toMillis());
                }
            } else {
                return result;
            }
        }
        return new DeliveryResult.Failure(last);
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
