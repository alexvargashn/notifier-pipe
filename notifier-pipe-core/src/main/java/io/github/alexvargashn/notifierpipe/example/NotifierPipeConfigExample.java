package io.github.alexvargashn.notifierpipe.example;

import io.github.alexvargashn.notifierpipe.api.NotifierPipe;
import io.github.alexvargashn.notifierpipe.api.NotifierPipeConfig;
import io.github.alexvargashn.notifierpipe.model.Channel;
import io.github.alexvargashn.notifierpipe.model.EmailNotification;
import io.github.alexvargashn.notifierpipe.model.PushNotification;
import io.github.alexvargashn.notifierpipe.model.SmsNotification;
import io.github.alexvargashn.notifierpipe.providers.FcmPushProvider;
import io.github.alexvargashn.notifierpipe.providers.SendGridEmailProvider;
import io.github.alexvargashn.notifierpipe.providers.TwilioSmsProvider;

/**
 * Demonstrates configuring NotifierPipe with named providers and switching the
 * default provider without changing business code. Credentials are read from
 * environment variables; all configuration is done in Java.
 */
public final class NotifierPipeConfigExample {

    private NotifierPipeConfigExample() {
    }

    /**
     * Builds a NotifierPipe with SendGrid as the default email provider and sends
     * one notification per channel. To use a different email provider, change the
     * config to {@code setDefaultProvider(Channel.EMAIL, "mailgun")} and register
     * an alternative implementation.
     *
     * @param args command-line arguments (unused)
     */
    public static void main(String[] args) {
        String sendGridKey = envOrDemo("SENDGRID_API_KEY", "SG.demo-key-for-simulation");
        String twilioSid = envOrDemo("TWILIO_ACCOUNT_SID", "ACdemo0000000000000000000000000000");
        String twilioToken = envOrDemo("TWILIO_AUTH_TOKEN", "demo-auth-token-value");
        String twilioFrom = envOrDemo("TWILIO_FROM_NUMBER", "+15550001111");
        String fcmKey = envOrDemo("FCM_SERVER_KEY", "AAAA-demo-fcm-server-key");

        NotifierPipe pipe = NotifierPipeConfig.builder()
                .registerProvider(Channel.EMAIL, "sendgrid", new SendGridEmailProvider(sendGridKey))
                .setDefaultProvider(Channel.EMAIL, "sendgrid")
                .registerProvider(Channel.SMS, "twilio", new TwilioSmsProvider(twilioSid, twilioToken, twilioFrom))
                .setDefaultProvider(Channel.SMS, "twilio")
                .registerProvider(Channel.PUSH, "fcm", new FcmPushProvider(fcmKey))
                .setDefaultProvider(Channel.PUSH, "fcm")
                .build();

        pipe.send(new EmailNotification("user@example.com", "Hello", "Welcome."));
        pipe.send(new SmsNotification("+15551234567", "Your code is 1234"));
        pipe.send(new PushNotification("device-token-1", "Alert", "New message", null));
    }

    private static String envOrDemo(String name, String demoValue) {
        String value = System.getenv(name);
        return (value != null && !value.isBlank()) ? value : demoValue;
    }
}
