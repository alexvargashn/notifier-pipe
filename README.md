# notifier-pipe

**notifier-pipe** is a framework-agnostic Java library that unifies sending notifications across multiple channels (Email, SMS, Push) through a single API. Provider choice (e.g. SendGrid vs Mailgun for email) is configuration-only, so business code stays unchanged when you switch or add providers.

## What problem it solves

- **One API** for all channels: call `pipe.send(notification)` regardless of whether the payload is email, SMS, or push.
- **Provider-agnostic code**: use keys like `"sendgrid"` or `"mailgun"` in configuration; no provider-specific logic in application code.
- **Clear error model**: validation errors (invalid email/phone) vs delivery failures (provider/network) are distinct; you can use exceptions (`send`) or results (`trySend`).
- **Optional async and retry**: synchronous by default; wrap with `NotifierPipes.async(pipe)` or `new RetryableNotifierPipe(pipe, RetryPolicy.defaultPolicy())` when needed.

The library does **not** implement real HTTP calls to providers; bundled providers simulate realistic API requests and log masked credentials. You can also implement `NotificationProvider` yourself for custom integrations.

---

## Requirements

- **Java 21** or higher
- **Maven 3.x** or **Gradle** (for building and dependency management)

---

## Installation

The library is built with Maven. Until it is published to a public registry, install it locally:

```bash
mvn clean install
```

Then depend on it:

### Maven

```xml
<dependency>
  <groupId>io.github.alexvargashn</groupId>
  <artifactId>notifier-pipe-core</artifactId>
  <version>1.0.0</version>
</dependency>
```

### Gradle (Groovy DSL)

```groovy
repositories {
  mavenLocal()
}

dependencies {
  implementation 'io.github.alexvargashn:notifier-pipe-core:1.0.0'
}
```

### Gradle (Kotlin DSL)

```kotlin
repositories {
  mavenLocal()
}

dependencies {
  implementation("io.github.alexvargashn:notifier-pipe-core:1.0.0")
}
```

See [RELEASE.md](RELEASE.md) for optional Maven Central publishing steps when you are ready to release.

---

## Quick Start

Configuration is **Java-only** (no YAML or properties files). Build a `NotifierPipe` and send notifications.

```java
import io.github.alexvargashn.notifierpipe.api.NotifierPipe;
import io.github.alexvargashn.notifierpipe.api.NotifierPipeConfig;
import io.github.alexvargashn.notifierpipe.model.Channel;
import io.github.alexvargashn.notifierpipe.model.EmailNotification;
import io.github.alexvargashn.notifierpipe.model.SmsNotification;
import io.github.alexvargashn.notifierpipe.model.PushNotification;
import io.github.alexvargashn.notifierpipe.providers.FcmPushProvider;
import io.github.alexvargashn.notifierpipe.providers.SendGridEmailProvider;
import io.github.alexvargashn.notifierpipe.providers.TwilioSmsProvider;

String sendGridKey = System.getenv("SENDGRID_API_KEY");
String twilioSid = System.getenv("TWILIO_ACCOUNT_SID");
String twilioToken = System.getenv("TWILIO_AUTH_TOKEN");
String twilioFrom = System.getenv("TWILIO_FROM_NUMBER");
String fcmKey = System.getenv("FCM_SERVER_KEY");

NotifierPipe pipe = NotifierPipeConfig.builder()
    .registerProvider(Channel.EMAIL, "sendgrid", new SendGridEmailProvider(sendGridKey))
    .setDefaultProvider(Channel.EMAIL, "sendgrid")
    .registerProvider(Channel.SMS, "twilio", new TwilioSmsProvider(twilioSid, twilioToken, twilioFrom))
    .setDefaultProvider(Channel.SMS, "twilio")
    .registerProvider(Channel.PUSH, "fcm", new FcmPushProvider(fcmKey))
    .setDefaultProvider(Channel.PUSH, "fcm")
    .build();

pipe.send(new EmailNotification("user@example.com", "Welcome", "Hello, world."));
pipe.send(new SmsNotification("+15551234567", "Your code is 1234"));
pipe.send(new PushNotification("device-token", "Alert", "New message", null));
```

---

## Configuring Email, SMS, and Push providers

Use `NotifierPipeConfig.builder()` to register provider instances by channel and key, set defaults, and call `build()`. No extra setup; full control over which providers are used.

### One provider per channel

```java
NotifierPipe pipe = NotifierPipeConfig.builder()
    .registerProvider(Channel.EMAIL, "sendgrid", new SendGridEmailProvider(apiKey))
    .setDefaultProvider(Channel.EMAIL, "sendgrid")
    .registerProvider(Channel.SMS, "twilio", new TwilioSmsProvider(sid, token, from))
    .setDefaultProvider(Channel.SMS, "twilio")
    .registerProvider(Channel.PUSH, "fcm", new FcmPushProvider(serverKey))
    .setDefaultProvider(Channel.PUSH, "fcm")
    .build();
```

### Multiple providers per channel (e.g. Email)

Register several providers under different keys and choose the default:

```java
NotifierPipe pipe = NotifierPipeConfig.builder()
    .registerProvider(Channel.EMAIL, "sendgrid", new SendGridEmailProvider(sendGridKey))
    .registerProvider(Channel.EMAIL, "mailgun", myMailgunProvider)
    .setDefaultProvider(Channel.EMAIL, "sendgrid")
    // ... SMS, PUSH ...
    .build();
```

To switch the default (e.g. to Mailgun), change only the config: `setDefaultProvider(Channel.EMAIL, "mailgun")` and rebuild the pipe. No changes to code that calls `pipe.send(...)`.

### Bundled simulated providers

| Class | Models | Credentials |
|-------|--------|-------------|
| `SendGridEmailProvider` | SendGrid v3 `POST /v3/mail/send` | API key |
| `TwilioSmsProvider` | Twilio `POST .../Messages.json` | Account SID, auth token, from number |
| `FcmPushProvider` | FCM `POST /fcm/send` | Server key |

All providers validate credentials at construction and mask them in log output via `CredentialMasking`.

### Provider contract

Each provider implements `NotificationProvider<N>` for one notification type:

- **Email**: `NotificationProvider<EmailNotification>` — `supportedType()` returns `EmailNotification.class`.
- **SMS**: `NotificationProvider<SmsNotification>` — `supportedType()` returns `SmsNotification.class`.
- **Push**: `NotificationProvider<PushNotification>` — `supportedType()` returns `PushNotification.class`.

The pipe routes by the notification's concrete type; providers receive only that subtype in `send(N notification)` and return a `SendReceipt` with `providerName`, `messageId`, and `sentAt`.

---

## Adding a new channel or provider without changing core code

### Adding a new provider for an existing channel

1. Implement `NotificationProvider<N>` for the existing type (e.g. `EmailNotification`).
2. Register it with a new key: `registerProvider(Channel.EMAIL, "my-provider", myProvider)`.
3. Optionally set it as default: `setDefaultProvider(Channel.EMAIL, "my-provider")`.

No changes to the library's core routing logic.

### Adding a new channel (e.g. Slack)

1. Extend the sealed `Notification` hierarchy with a new record (e.g. `SlackNotification`) and add it to the `permits` clause.
2. Implement `NotificationProvider<YourNotificationType>`.
3. Register the provider via `NotifierPipeConfig.builder().registerProvider(...).setDefaultProvider(...)`.

New **providers** are added by implementing the SPI and registering them. New **channels** require extending the notification model and the config's channel–type mapping once; after that, new providers for that channel are configuration-only.

---

## API overview

| Type | Description |
|------|-------------|
| **`NotifierPipe`** | Main entry point. `send(Notification)` throws on failure; `trySend(Notification)` returns `DeliveryResult`. |
| **`NotifierPipeConfig`** | Builder for Java-based configuration: register providers by channel and key, set default per channel, `build()` or `buildAsync()`. |
| **`Notification`** | Sealed interface; implementations: `EmailNotification`, `SmsNotification`, `PushNotification`. Channel is derived from the type. |
| **`NotificationProvider<N>`** | SPI: `supportedType()` and `send(N)` returning `SendReceipt`. Implement for each provider. |
| **`SendReceipt`** | `providerName`, `messageId`, `sentAt` — returned on successful send. |
| **`DeliveryResult`** | Sealed: `Success(SendReceipt)` or `Failure(NotificationException)`. Returned by `trySend`. |
| **`NotificationException`** | Base for `ValidationException`, `DeliveryException`, `NoProviderForNotificationException`. |
| **`RetryableNotifierPipe`** | Decorator: retries only `DeliveryException` with exponential backoff (`RetryPolicy`). |
| **`AsyncNotifierPipe`** | Extends `NotifierPipe` with `sendAsync` and `sendBatchAsync`. Use `NotifierPipes.async(pipe)` or `buildAsync()`. |
| **`NotifierPipes`** | Factory: `async(NotifierPipe)` and `async(NotifierPipe, Executor)`. |

### Notification types

- **`EmailNotification(to, subject, body)`** — `to` and subject are validated (format / non-blank).
- **`SmsNotification(phoneNumber, message)`** — phone number validated (digits, optional `+`, min length).
- **`PushNotification(deviceToken, title, body, data)`** — `data` is optional; device token must be non-blank.

### Errors

- **`ValidationException`** — Invalid payload (e.g. bad email/phone); no delivery attempted.
- **`NoProviderForNotificationException`** — No provider registered for the notification's type.
- **`DeliveryException`** — Delivery attempted but failed (provider/network); retried by `RetryableNotifierPipe`.

---

## Security best practices for credentials

- Never hardcode API keys. Read them from environment variables or a secret manager and pass them to the provider constructors in Java code.
- Providers validate credentials at construction and never log them in the clear (see `CredentialMasking`).
- Credentials never appear in `Notification` payloads or in `SendReceipt` results.
- Limit scope of credentials to the minimum required (e.g. send-only API keys).
- Keep dependencies up to date for any HTTP or crypto libraries used inside custom provider implementations.

---

## Design decisions & trade-offs

- **Sealed `Notification` + exhaustive switch:** compiler-enforced handling of every channel; adding a channel is a deliberate act, not an accident.
- **Class-based routing in the SPI:** providers declare `supportedType()` as a `Class<?>` rather than routing by `Channel` enum alone. This keeps the send path type-safe (each provider receives only its subtype) while the `Channel` enum is used only in configuration.
- **Two-level Strategy:** channel selection and provider selection are separate axes, so switching provider (SendGrid → Mailgun) is configuration-only.
- **Dual API (`send` throws, `trySend` returns `DeliveryResult`):** exceptions for straight-line flow, Result for batch/functional handling.
- **Async and retry as decorators:** keeps the synchronous core minimal; cross-cutting concerns compose without changing `DefaultNotifierPipe`.
- **`SendReceipt` on success:** `trySend` returns provider name, message id, and timestamp for traceability without coupling callers to provider internals.

## What's not implemented yet (and why)

- **Templates:** valuable but optional; prioritized channels, configuration, error model, and tests first.
- **Pub-Sub status events:** would justify the "pipe" name for lifecycle events (`SENDING`/`SENT`/`FAILED`); deferred to keep the first cut focused.
- **ServiceLoader discovery:** not included; `NotifierPipeConfig` is the supported configuration path.

---

## Build and test

From the project root:

```bash
mvn clean verify
```

Tests live in `notifier-pipe-core` and use JUnit 5 with stub and simulated providers (no real network calls).

### Docker demo (optional)

```bash
docker build -t notifier-pipe-demo .
docker run --rm notifier-pipe-demo
```

The image uses Eclipse Temurin Java 21, builds the library with Maven in a multi-stage build, and runs `NotifierPipeConfigExample` (Email, SMS, and Push via named providers; output goes to stdout).
