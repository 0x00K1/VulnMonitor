package com.vulnmonitor.utils;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class EncryptionUtil {
    private static final String KEY = "0123456789abcdef"; // 16-byte key for AES-128 # System.getenv("SESSION_ENCRYPTION_KEY");
    private static final String ALGORITHM = "AES";

    /**
     * Encrypts a plaintext string using AES encryption.
     *
     * @param plaintext The text to encrypt.
     * @return The encrypted text in Base64 encoding.
     * @throws Exception If encryption fails.
     */
    public static String encrypt(String plaintext) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(KEY.getBytes(), ALGORITHM);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);
        byte[] encrypted = cipher.doFinal(plaintext.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    /**
     * Decrypts a Base64-encoded encrypted string using AES decryption.
     *
     * @param encryptedText The encrypted text in Base64 encoding.
     * @return The decrypted plaintext.
     * @throws Exception If decryption fails.
     */
    public static String decrypt(String encryptedText) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(KEY.getBytes(), ALGORITHM);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, keySpec);
        byte[] decodedBytes = Base64.getDecoder().decode(encryptedText);
        byte[] decrypted = cipher.doFinal(decodedBytes);
        return new String(decrypted, "UTF-8");
    }
}