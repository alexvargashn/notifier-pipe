package io.github.alexvargashn.notifierpipe.api;

import java.util.Map;
import java.util.Objects;

import io.github.alexvargashn.notifierpipe.model.Channel;
import io.github.alexvargashn.notifierpipe.model.Notification;
import io.github.alexvargashn.notifierpipe.spi.NotificationProvider;
import io.github.alexvargashn.notifierpipe.spi.SendReceipt;

/**
 * NotifierPipe implementation that dispatches to a default provider per channel.
 * Validates before sending; distinguishes validation vs delivery failures.
 */
public final class ConfiguredNotifierPipe implements NotifierPipe {

    private final Map<Channel, Map<String, NotificationProvider<?>>> providersByChannelAndKey;
    private final Map<Channel, String> defaultProviderKeyByChannel;

    ConfiguredNotifierPipe(
            Map<Channel, Map<String, NotificationProvider<?>>> providersByChannelAndKey,
            Map<Channel, String> defaultProviderKeyByChannel) {
        this.providersByChannelAndKey = Map.copyOf(providersByChannelAndKey);
        this.defaultProviderKeyByChannel = Map.copyOf(defaultProviderKeyByChannel);
    }

    @Override
    public void send(Notification notification) {
        Objects.requireNonNull(notification, "notification");
        NotificationValidator.validate(notification);
        Channel channel = notification.channel();
        String defaultKey = defaultProviderKeyByChannel.get(channel);
        if (defaultKey == null) {
            throw new NoProviderForNotificationException(notification);
        }
        Map<String, NotificationProvider<?>> providers = providersByChannelAndKey.get(channel);
        if (providers == null) {
            throw new NoProviderForNotificationException(notification);
        }
        NotificationProvider<?> provider = providers.get(defaultKey);
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
            Channel channel = notification.channel();
            String defaultKey = defaultProviderKeyByChannel.get(channel);
            if (defaultKey == null) {
                throw new NoProviderForNotificationException(notification);
            }
            Map<String, NotificationProvider<?>> providers = providersByChannelAndKey.get(channel);
            if (providers == null) {
                throw new NoProviderForNotificationException(notification);
            }
            NotificationProvider<?> provider = providers.get(defaultKey);
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
