package io.github.alexvargashn.notifierpipe.providers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link CredentialMasking}.
 */
@DisplayName("CredentialMasking")
class CredentialMaskingTest {

    @Test
    @DisplayName("masks null and short secrets fully")
    void masksShortSecrets() {
        assertEquals("***", CredentialMasking.mask(null));
        assertEquals("***", CredentialMasking.mask("abc"));
        assertEquals("***", CredentialMasking.mask("abcd"));
    }

    @Test
    @DisplayName("shows first four characters of longer secrets")
    void masksLongSecrets() {
        assertEquals("SG.d***", CredentialMasking.mask("SG.demo-key-for-simulation"));
    }
}
