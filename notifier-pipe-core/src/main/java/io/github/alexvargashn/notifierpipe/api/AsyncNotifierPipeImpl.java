package io.github.alexvargashn.notifierpipe.api;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import io.github.alexvargashn.notifierpipe.model.Notification;

/**
 * Decorator that adds async send to any NotifierPipe using the given Executor.
 * Synchronous send/trySend delegate directly; async methods run on the executor
 * and handle exceptions via CompletableFuture.
 */
final class AsyncNotifierPipeImpl implements AsyncNotifierPipe {

    private final NotifierPipe delegate;
    private final Executor executor;

    AsyncNotifierPipeImpl(NotifierPipe delegate, Executor executor) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    @Override
    public void send(Notification notification) {
        delegate.send(notification);
    }

    @Override
    public DeliveryResult trySend(Notification notification) {
        return delegate.trySend(notification);
    }

    @Override
    public CompletableFuture<Void> sendAsync(Notification notification) {
        Objects.requireNonNull(notification, "notification");
        return CompletableFuture.runAsync(() -> delegate.send(notification), executor);
    }

    @Override
    public CompletableFuture<List<DeliveryResult>> sendBatchAsync(List<Notification> notifications) {
        Objects.requireNonNull(notifications, "notifications");
        List<CompletableFuture<DeliveryResult>> futures = notifications.stream()
                .<CompletableFuture<DeliveryResult>>map(n -> n == null
                        ? CompletableFuture.completedFuture(new DeliveryResult.Failure(new ValidationException("null notification")))
                        : CompletableFuture.supplyAsync(() -> delegate.trySend(n), executor))
                .toList();
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                .thenApply(v -> futures.stream().map(CompletableFuture::join).toList());
    }
}
