package io.github.alexvargashn.notifierpipe.api;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

import io.github.alexvargashn.notifierpipe.model.Channel;
import io.github.alexvargashn.notifierpipe.model.EmailNotification;
import io.github.alexvargashn.notifierpipe.model.Notification;
import io.github.alexvargashn.notifierpipe.model.PushNotification;
import io.github.alexvargashn.notifierpipe.model.SmsNotification;
import io.github.alexvargashn.notifierpipe.spi.NotificationProvider;

/**
 * Java-based configuration for building a {@link NotifierPipe} with multiple
 * providers per channel and a selectable default per channel. Configuration
 * is done entirely in code; no external config files.
 * <p>
 * Example:
 * <pre>{@code
 * NotifierPipe pipe = NotifierPipeConfig.builder()
 *     .registerProvider(Channel.EMAIL, "sendgrid", sendgridProvider)
 *     .registerProvider(Channel.EMAIL, "mailgun", mailgunProvider)
 *     .setDefaultProvider(Channel.EMAIL, "sendgrid")
 *     .registerProvider(Channel.SMS, "twilio", twilioProvider)
 *     .setDefaultProvider(Channel.SMS, "twilio")
 *     .build();
 * }</pre>
 */
public final class NotifierPipeConfig {

    private static final Map<Channel, Class<? extends Notification>> CHANNEL_TO_TYPE = Map.of(
            Channel.EMAIL, EmailNotification.class,
            Channel.SMS, SmsNotification.class,
            Channel.PUSH, PushNotification.class
    );

    private NotifierPipeConfig() {
    }

    /**
     * Returns a new builder for configuring and building a NotifierPipe.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for registering providers by channel and key, setting defaults per channel,
     * and producing a {@link NotifierPipe} instance.
     */
    public static final class Builder {

        private final Map<Channel, Map<String, NotificationProvider<?>>> providersByChannelAndKey;
        private final Map<Channel, String> defaultProviderKeyByChannel;
        private Executor asyncExecutor;

        private Builder() {
            this.providersByChannelAndKey = new EnumMap<>(Channel.class);
            this.defaultProviderKeyByChannel = new EnumMap<>(Channel.class);
            this.asyncExecutor = null;
        }

        /**
         * Sets the executor used for async operations when building via {@link #buildAsync()}.
         * If not set, {@link ForkJoinPool#commonPool()} is used.
         *
         * @param executor the executor for sendAsync and sendBatchAsync (non-null)
         * @return this builder for chaining
         */
        public Builder executor(Executor executor) {
            this.asyncExecutor = Objects.requireNonNull(executor, "executor");
            return this;
        }

        /**
         * Registers a provider for a channel under the given key. The provider's
         * {@link NotificationProvider#supportedType()} must match the notification
         * type for the channel (e.g. EmailNotification for EMAIL).
         *
         * @param channel     the channel (e.g. EMAIL, SMS, PUSH)
         * @param providerKey a unique key for this provider (e.g. "sendgrid", "twilio")
         * @param provider    the provider instance (non-null)
         * @return this builder for chaining
         * @throws IllegalArgumentException if the provider's supported type does not match the channel,
         *                                  or if the key is null or empty
         */
        public Builder registerProvider(Channel channel, String providerKey, NotificationProvider<?> provider) {
            Objects.requireNonNull(channel, "channel");
            Objects.requireNonNull(provider, "provider");
            if (providerKey == null || providerKey.isBlank()) {
                throw new IllegalArgumentException("providerKey must be non-null and non-blank");
            }
            Class<? extends Notification> expectedType = CHANNEL_TO_TYPE.get(channel);
            if (expectedType != null) {
                Class<?> actualType = Objects.requireNonNull(provider.supportedType(), "provider.supportedType()");
                if (!expectedType.equals(actualType)) {
                    throw new IllegalArgumentException(
                            "Channel " + channel + " requires provider for " + expectedType.getSimpleName()
                                    + ", but provider supports " + actualType.getSimpleName());
                }
            }
            providersByChannelAndKey
                    .computeIfAbsent(channel, k -> new HashMap<>())
                    .put(providerKey, provider);
            return this;
        }

        /**
         * Sets the default provider key for a channel. The key must have been
         * registered for that channel via {@link #registerProvider(Channel, String, NotificationProvider)}.
         * At build time, every channel that has at least one provider must have a default set.
         *
         * @param channel     the channel
         * @param providerKey the key of the provider to use as default for this channel
         * @return this builder for chaining
         */
        public Builder setDefaultProvider(Channel channel, String providerKey) {
            Objects.requireNonNull(channel, "channel");
            Objects.requireNonNull(providerKey, "providerKey");
            defaultProviderKeyByChannel.put(channel, providerKey);
            return this;
        }

        /**
         * Builds an immutable configuration and returns a NotifierPipe that uses
         * the default provider per channel when sending.
         *
         * @return a NotifierPipe instance
         * @throws IllegalStateException if a channel has providers but no default set,
         *                                or if the default key is not registered for that channel
         */
        public NotifierPipe build() {
            for (Map.Entry<Channel, Map<String, NotificationProvider<?>>> e : providersByChannelAndKey.entrySet()) {
                Channel channel = e.getKey();
                Map<String, NotificationProvider<?>> providers = e.getValue();
                if (providers.isEmpty()) {
                    continue;
                }
                String defaultKey = defaultProviderKeyByChannel.get(channel);
                if (defaultKey == null || defaultKey.isBlank()) {
                    throw new IllegalStateException(
                            "Channel " + channel + " has registered providers but no default provider set. "
                                    + "Call setDefaultProvider(" + channel + ", \"key\") for one of: "
                                    + String.join(", ", providers.keySet()));
                }
                if (!providers.containsKey(defaultKey)) {
                    throw new IllegalStateException(
                            "Default provider key \"" + defaultKey + "\" for channel " + channel
                                    + " is not registered. Registered keys: "
                                    + String.join(", ", providers.keySet()));
                }
            }
            Map<Channel, Map<String, NotificationProvider<?>>> immutableProviders = new EnumMap<>(Channel.class);
            providersByChannelAndKey.forEach((ch, map) -> immutableProviders.put(ch, Map.copyOf(map)));
            return new ConfiguredNotifierPipe(immutableProviders, new EnumMap<>(defaultProviderKeyByChannel));
        }

        /**
         * Builds the same configuration as {@link #build()} but returns an {@link AsyncNotifierPipe}
         * that uses the executor set via {@link #executor(Executor)}, or
         * {@link ForkJoinPool#commonPool()} if none was set.
         *
         * @return an AsyncNotifierPipe instance
         * @throws IllegalStateException if a channel has providers but no default set,
         *                                or if the default key is not registered for that channel
         */
        public AsyncNotifierPipe buildAsync() {
            NotifierPipe pipe = build();
            Executor executor = asyncExecutor != null ? asyncExecutor : ForkJoinPool.commonPool();
            return NotifierPipes.async(pipe, executor);
        }
    }
}
