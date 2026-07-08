package io.github.alexvargashn.notifierpipe.api;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import io.github.alexvargashn.notifierpipe.model.Notification;
import io.github.alexvargashn.notifierpipe.spi.NotificationProvider;
import io.github.alexvargashn.notifierpipe.spi.SendReceipt;

/**
 * Routes notifications to the provider that supports their concrete type.
 * Validates before sending; distinguishes validation vs delivery failures.
 */
public class DefaultNotifierPipe implements NotifierPipe {

    private final Map<Class<? extends Notification>, NotificationProvider<?>> providersByType;

    /**
     * Registers the given providers. Each provider's {@link NotificationProvider#supportedType()}
     * must be unique; duplicate types cause {@link IllegalArgumentException}.
     *
     * @param providers collection of providers (non-null; no null elements)
     * @throws IllegalArgumentException if more than one provider declares the same supported type
     */
    public DefaultNotifierPipe(Collection<? extends NotificationProvider<?>> providers) {
        Objects.requireNonNull(providers, "providers");
        this.providersByType = new HashMap<>();
        for (NotificationProvider<?> p : providers) {
            Objects.requireNonNull(p, "provider");
            Class<? extends Notification> type = Objects.requireNonNull(p.supportedType(), "provider.supportedType()");
            if (this.providersByType.put(type, p) != null) {
                throw new IllegalArgumentException("Duplicate provider for notification type: " + type.getSimpleName());
            }
        }
    }

    @Override
    public void send(Notification notification) {
        Objects.requireNonNull(notification, "notification");
        NotificationValidator.validate(notification);
        NotificationProvider<?> provider = providersByType.get(notification.getClass());
        if (provider == null) {
            throw new NoProviderForNotificationException(notification);
        }
        try {
            provider.sendNotification(notification);
        } catch (NotificationException e) {
            throw e;
        } catch (Exception e) {
            throw new DeliveryException("Delivery failed: " + e.getMessage(), notification, e);
        }
    }

    @Override
    public DeliveryResult trySend(Notification notification) {
        Objects.requireNonNull(notification, "notification");
        try {
            NotificationValidator.validate(notification);
            NotificationProvider<?> provider = providersByType.get(notification.getClass());
            if (provider == null) {
                throw new NoProviderForNotificationException(notification);
            }
            SendReceipt receipt = provider.sendNotification(notification);
            return new DeliveryResult.Success(receipt);
        } catch (NotificationException e) {
            return new DeliveryResult.Failure(e);
        } catch (Exception e) {
            return new DeliveryResult.Failure(
                    new DeliveryException("Delivery failed: " + e.getMessage(), notification, e));
        }
    }
}
