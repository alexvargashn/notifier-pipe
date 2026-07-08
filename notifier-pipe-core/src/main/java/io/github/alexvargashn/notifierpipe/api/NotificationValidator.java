package io.github.alexvargashn.notifierpipe.api;

import io.github.alexvargashn.notifierpipe.model.EmailNotification;
import io.github.alexvargashn.notifierpipe.model.Notification;
import io.github.alexvargashn.notifierpipe.model.PushNotification;
import io.github.alexvargashn.notifierpipe.model.SmsNotification;

import java.util.regex.Pattern;

/**
 * Validates notification payloads per type. Uses simple rules and no external libraries.
 * Validation failures are reported via {@link ValidationException}.
 */
public final class NotificationValidator {

    /** Simple email: local@domain.tld; allows letters, digits, dots, hyphens, underscore. */
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

    /** Normalized phone: optional +, then at least 10 digits. */
    private static final Pattern PHONE_NORMALIZED_PATTERN = Pattern.compile("^\\+?[0-9]{10,}$");

    private NotificationValidator() {
    }

    /**
     * Validates the notification and throws {@link ValidationException} if invalid.
     *
     * @param notification the notification to validate (non-null)
     * @throws ValidationException if validation fails
     */
    public static void validate(Notification notification) {
        switch (notification) {
            case EmailNotification n -> validateEmail(n);
            case SmsNotification n -> validateSms(n);
            case PushNotification n -> validatePush(n);
        }
    }

    private static void validateEmail(EmailNotification n) {
        if (n.to() == null || n.to().isBlank()) {
            throw new ValidationException("Email 'to' must be non-blank", n);
        }
        if (!EMAIL_PATTERN.matcher(n.to().trim()).matches()) {
            throw new ValidationException("Invalid email format: " + n.to(), n);
        }
        if (n.subject() == null || n.subject().isBlank()) {
            throw new ValidationException("Email subject must be non-blank", n);
        }
    }

    private static void validateSms(SmsNotification n) {
        if (n.phoneNumber() == null || n.phoneNumber().isBlank()) {
            throw new ValidationException("SMS phone number must be non-blank", n);
        }
        String normalized = n.phoneNumber().replaceAll("[\\s-]", "");
        if (!PHONE_NORMALIZED_PATTERN.matcher(normalized).matches()) {
            throw new ValidationException("Invalid phone number format (expected digits, optional +): " + n.phoneNumber(), n);
        }
    }

    private static void validatePush(PushNotification n) {
        if (n.deviceToken() == null || n.deviceToken().isBlank()) {
            throw new ValidationException("Push device token must be non-blank", n);
        }
    }
}
