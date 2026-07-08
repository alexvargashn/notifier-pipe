package io.github.alexvargashn.notifierpipe.providers;

/**
 * Utility for masking sensitive values (API keys, tokens) before logging.
 * Never log a raw credential.
 */
public final class CredentialMasking {

    private static final int VISIBLE = 4;
    private static final String MASK = "***";

    private CredentialMasking() {
    }

    /**
     * Masks a secret, showing at most the first {@value VISIBLE} characters.
     * Short or null secrets are fully masked.
     *
     * @param secret the sensitive value (may be null)
     * @return a log-safe representation
     */
    public static String mask(String secret) {
        if (secret == null || secret.length() <= VISIBLE) {
            return MASK;
        }
        return secret.substring(0, VISIBLE) + MASK;
    }
}
