package com.campusmedical.common.util;

import com.campusmedical.common.exception.SettingsEncryptionException;
import com.campusmedical.common.exception.ValidationException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class FernetCipher {

    private static final byte VERSION = (byte) 0x80;

    private final byte[] signingKey;
    private final byte[] encryptionKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public FernetCipher(String encodedKey) {
        if (encodedKey == null || encodedKey.trim().isEmpty()) {
            throw new SettingsEncryptionException("后端缺少 SETTINGS_ENCRYPTION_KEY，无法保存或测试 LLM 配置");
        }

        byte[] keyBytes;
        try {
            keyBytes = Base64.getUrlDecoder().decode(encodedKey.trim());
        } catch (IllegalArgumentException exception) {
            throw new SettingsEncryptionException("SETTINGS_ENCRYPTION_KEY 格式无效");
        }

        if (keyBytes.length != 32) {
            throw new SettingsEncryptionException("SETTINGS_ENCRYPTION_KEY 格式无效");
        }

        this.signingKey = Arrays.copyOfRange(keyBytes, 0, 16);
        this.encryptionKey = Arrays.copyOfRange(keyBytes, 16, 32);
    }

    public String encrypt(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException("敏感配置不能为空");
        }

        try {
            byte[] iv = new byte[16];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(
                Cipher.ENCRYPT_MODE,
                new SecretKeySpec(encryptionKey, "AES"),
                new IvParameterSpec(iv)
            );
            byte[] ciphertext = cipher.doFinal(value.trim().getBytes(StandardCharsets.UTF_8));

            byte[] timestamp = ByteBuffer.allocate(8).putLong(System.currentTimeMillis() / 1000L).array();
            byte[] payload = new byte[1 + timestamp.length + iv.length + ciphertext.length];
            payload[0] = VERSION;
            System.arraycopy(timestamp, 0, payload, 1, timestamp.length);
            System.arraycopy(iv, 0, payload, 1 + timestamp.length, iv.length);
            System.arraycopy(ciphertext, 0, payload, 1 + timestamp.length + iv.length, ciphertext.length);

            byte[] signature = sign(payload);
            byte[] token = new byte[payload.length + signature.length];
            System.arraycopy(payload, 0, token, 0, payload.length);
            System.arraycopy(signature, 0, token, payload.length, signature.length);
            return Base64.getUrlEncoder().encodeToString(token);
        } catch (GeneralSecurityException exception) {
            throw new SettingsEncryptionException("系统敏感配置加密失败");
        }
    }

    public String decrypt(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new ValidationException("缺少已保存的敏感配置");
        }

        try {
            byte[] raw = Base64.getUrlDecoder().decode(token.trim());
            if (raw.length < 1 + 8 + 16 + 32 || raw[0] != VERSION) {
                throw new SettingsEncryptionException("已保存的敏感配置无法解密，请重新配置");
            }

            int signatureOffset = raw.length - 32;
            byte[] payload = Arrays.copyOfRange(raw, 0, signatureOffset);
            byte[] expectedSignature = Arrays.copyOfRange(raw, signatureOffset, raw.length);
            byte[] actualSignature = sign(payload);
            if (!MessageDigest.isEqual(expectedSignature, actualSignature)) {
                throw new SettingsEncryptionException("已保存的敏感配置无法解密，请重新配置");
            }

            byte[] iv = Arrays.copyOfRange(raw, 9, 25);
            byte[] ciphertext = Arrays.copyOfRange(raw, 25, signatureOffset);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(
                Cipher.DECRYPT_MODE,
                new SecretKeySpec(encryptionKey, "AES"),
                new IvParameterSpec(iv)
            );
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (SettingsEncryptionException exception) {
            throw exception;
        } catch (IllegalArgumentException | GeneralSecurityException exception) {
            throw new SettingsEncryptionException("已保存的敏感配置无法解密，请重新配置");
        }
    }

    public static String mask(String value) {
        String cleaned = value == null ? "" : value.trim();
        if (cleaned.length() <= 8) {
            return cleaned.replaceAll(".", "*");
        }
        StringBuilder builder = new StringBuilder();
        builder.append(cleaned.substring(0, 4));
        for (int index = 0; index < cleaned.length() - 8; index++) {
            builder.append('*');
        }
        builder.append(cleaned.substring(cleaned.length() - 4));
        return builder.toString();
    }

    private byte[] sign(byte[] payload) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(signingKey, "HmacSHA256"));
        return mac.doFinal(payload);
    }
}
