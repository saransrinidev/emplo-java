package com.emplo.service;

import com.emplo.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

/**
 * Encryption service for sensitive data (bank account numbers).
 * Uses AES encryption with key derived from JWT secret.
 */
@Service
@RequiredArgsConstructor
public class EncryptionService {

    private final AppProperties appProperties;

    private SecretKey getKey() throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] key = sha.digest(appProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
        key = Arrays.copyOf(key, 16); // AES-128
        return new SecretKeySpec(key, "AES");
    }

    public String encrypt(String plaintext) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, getKey());
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    public String decrypt(String ciphertext) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, getKey());
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(ciphertext));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
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
