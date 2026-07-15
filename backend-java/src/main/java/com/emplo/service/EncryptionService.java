package com.emplo.service;

import com.emplo.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Encryption service for sensitive data (bank account numbers).
 * Uses AES-256-GCM with random IV for each encryption operation.
 * Key is derived from a SHA-256 hash of the configured encryption secret.
 *
 * Format: base64(IV[12] + ciphertext + authTag[16])
 */
@Service
@RequiredArgsConstructor
public class EncryptionService {

    private final AppProperties appProperties;
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128; // bits
    private static final SecureRandom RANDOM = new SecureRandom();

    private SecretKey getKey() {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] key = sha.digest(appProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(key, "AES"); // Full 256-bit key
        } catch (Exception e) {
            throw new RuntimeException("Failed to derive encryption key", e);
        }
    }

    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, getKey(), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // Prepend IV to ciphertext
            byte[] combined = ByteBuffer.allocate(iv.length + ciphertext.length)
                    .put(iv)
                    .put(ciphertext)
                    .array();

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    public String decrypt(String ciphertextBase64) {
        try {
            byte[] combined = Base64.getDecoder().decode(ciphertextBase64);

            // Extract IV (first 12 bytes)
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] ciphertext = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(combined, GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, getKey(), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] plaintext = cipher.doFinal(ciphertext);

            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // Fallback: try legacy ECB decryption for existing data
            return decryptLegacyEcb(ciphertextBase64);
        }
    }

    /**
     * Backward-compatible decryption for data encrypted with the old ECB mode.
     * Will be removed once all existing data is re-encrypted.
     */
    private String decryptLegacyEcb(String ciphertextBase64) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = sha.digest(appProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
            byte[] key128 = new byte[16];
            System.arraycopy(keyBytes, 0, key128, 0, 16);
            SecretKey legacyKey = new SecretKeySpec(key128, "AES");

            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, legacyKey);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(ciphertextBase64));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed (both GCM and legacy ECB)", e);
        }
    }

    public String maskAccountNumber(String ciphertext) {
        try {
            String plain = decrypt(ciphertext);
            return "XXXX" + (plain.length() >= 4 ? plain.substring(plain.length() - 4) : "");
        } catch (Exception e) {
            return "XXXX****";
        }
    }
}
