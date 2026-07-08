package io.github.alexvargashn.notifierpipe.model;

import java.util.Map;
import java.util.Objects;

/**
 * Push notification payload (e.g. FCM, APNs).
 *
 * @param deviceToken target device token (non-null)
 * @param title       notification title (non-null)
 * @param body        notification body (non-null)
 * @param data        optional key-value payload; null is treated as empty (unmodifiable)
 */
public record PushNotification(
        String deviceToken,
        String title,
        String body,
        Map<String, String> data
) implements Notification {

    public PushNotification {
        Objects.requireNonNull(deviceToken, "deviceToken");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(body, "body");
        data = (data == null) ? Map.of() : Map.copyOf(data);
    }

    @Override
    public Channel channel() {
        return Channel.PUSH;
    }
}
