package com.minhtriet79.goldminer.domain.account;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {

    @Test
    void hashProducesDeterministic64CharLowercaseSha256Hex() {
        String h1 = PasswordHasher.hash("gold1234");
        String h2 = PasswordHasher.hash("gold1234");
        String h3 = PasswordHasher.hash("gold1235");

        assertEquals(64, h1.length());
        assertEquals(h1, h2);
        assertNotEquals(h1, h3);
        assertTrue(h1.matches("^[0-9a-f]{64}$"));
    }

    @Test
    void hashRejectsNullPassword() {
        assertThrows(IllegalArgumentException.class, () -> PasswordHasher.hash(null));
    }

    @Test
    void verifyMatchesValidAndRejectsInvalidInputs() {
        String hash = PasswordHasher.hash("secret-pass");

        assertTrue(PasswordHasher.verify("secret-pass", hash));
        assertTrue(PasswordHasher.verify("secret-pass", hash.toUpperCase()));
        assertFalse(PasswordHasher.verify("wrong-pass", hash));
        assertFalse(PasswordHasher.verify(null, hash));
        assertFalse(PasswordHasher.verify("secret-pass", null));
        assertFalse(PasswordHasher.verify("secret-pass", ""));
    }
}
