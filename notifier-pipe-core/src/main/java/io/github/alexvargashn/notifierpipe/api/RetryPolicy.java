package io.github.alexvargashn.notifierpipe.api;

import java.time.Duration;

/**
 * Retry configuration with exponential backoff.
 *
 * @param maxAttempts       total attempts including the first (>= 1)
 * @param initialDelay      delay before the first retry
 * @param backoffMultiplier multiplier applied to the delay after each retry (>= 1.0)
 * @param maxDelay          cap on the delay between attempts
 */
public record RetryPolicy(
        int maxAttempts,
        Duration initialDelay,
        double backoffMultiplier,
        Duration maxDelay
) {
    public RetryPolicy {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be >= 1");
        }
        if (backoffMultiplier < 1.0) {
            throw new IllegalArgumentException("backoffMultiplier must be >= 1.0");
        }
    }

    /**
     * Returns a sensible default: 3 attempts, 200 ms initial delay, 2x backoff, 5 s cap.
     *
     * @return default retry policy
     */
    public static RetryPolicy defaultPolicy() {
        return new RetryPolicy(3, Duration.ofMillis(200), 2.0, Duration.ofSeconds(5));
    }

    /**
     * Delay before the given retry number (1-based).
     *
     * @param retryNumber retry attempt number (1 = first retry after initial failure)
     * @return delay before that retry
     */
    public Duration delayForRetry(int retryNumber) {
        long ms = (long) (initialDelay.toMillis() * Math.pow(backoffMultiplier, retryNumber - 1));
        return Duration.ofMillis(Math.min(ms, maxDelay.toMillis()));
    }
}
