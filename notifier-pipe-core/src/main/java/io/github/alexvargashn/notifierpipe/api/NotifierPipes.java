package io.github.alexvargashn.notifierpipe.api;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * Factory for optional async support. Wraps any {@link NotifierPipe} with an
 * {@link AsyncNotifierPipe} that uses the given or default executor.
 */
public final class NotifierPipes {

    private NotifierPipes() {
    }

    /**
     * Returns an AsyncNotifierPipe that delegates to the given pipe and uses
     * {@link ForkJoinPool#commonPool()} for async operations.
     *
     * @param pipe the underlying NotifierPipe (non-null)
     * @return an AsyncNotifierPipe wrapping the given pipe
     */
    public static AsyncNotifierPipe async(NotifierPipe pipe) {
        return async(Objects.requireNonNull(pipe, "pipe"), ForkJoinPool.commonPool());
    }

    /**
     * Returns an AsyncNotifierPipe that delegates to the given pipe and uses
     * the specified executor for async operations.
     *
     * @param pipe     the underlying NotifierPipe (non-null)
     * @param executor the executor for sendAsync and sendBatchAsync (non-null)
     * @return an AsyncNotifierPipe wrapping the given pipe
     */
    public static AsyncNotifierPipe async(NotifierPipe pipe, Executor executor) {
        return new AsyncNotifierPipeImpl(
                Objects.requireNonNull(pipe, "pipe"),
                Objects.requireNonNull(executor, "executor"));
    }
}
