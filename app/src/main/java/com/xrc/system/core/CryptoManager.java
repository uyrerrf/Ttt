package com.xrc.system.core;

import android.util.Base64;
import android.util.Log;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.ECGenParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class CryptoManager {
    private static final String TAG = Constants.TAG + ":Crypto";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;
    private static final int AES_KEY_SIZE = 256;

    private final SecureRandom secureRandom;
    private KeyPair rsaKeyPair;

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public CryptoManager() {
        this.secureRandom = new SecureRandom();
        initRSA();
    }

    private void initRSA() {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048, secureRandom);
            rsaKeyPair = gen.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "RSA init failed", e);
        }
    }

    public String generateAESKey() {
        try {
            KeyGenerator gen = KeyGenerator.getInstance("AES");
            gen.init(AES_KEY_SIZE, secureRandom);
            SecretKey key = gen.generateKey();
            return Base64.encodeToString(key.getEncoded(), Base64.NO_WRAP);
        } catch (Exception e) {
            Log.e(TAG, "AES key gen failed", e);
            byte[] fallback = new byte[32];
            secureRandom.nextBytes(fallback);
            return Base64.encodeToString(fallback, Base64.NO_WRAP);
        }
    }

    public String encryptAES(String plaintext, String keyB64) {
        try {
            byte[] keyBytes = Base64.decode(keyB64, Base64.NO_WRAP);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            return Base64.encodeToString(combined, Base64.NO_WRAP);
        } catch (Exception e) {
            Log.e(TAG, "AES encrypt failed", e);
            return null;
        }
    }

    public String decryptAES(String ciphertext, String keyB64) {
        try {
            byte[] keyBytes = Base64.decode(keyB64, Base64.NO_WRAP);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            byte[] combined = Base64.decode(ciphertext, Base64.NO_WRAP);
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            byte[] encrypted = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, GCM_IV_LENGTH, encrypted, 0, encrypted.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            Log.e(TAG, "AES decrypt failed", e);
            return null;
        }
    }

    public String hashSHA256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            Log.e(TAG, "Hash failed", e);
            return "";
        }
    }

    public byte[] generateRandomBytes(int length) {
        byte[] bytes = new byte[length];
        secureRandom.nextBytes(bytes);
        return bytes;
    }

    public String getRSAPublicKey() {
        if (rsaKeyPair == null) return "";
        return Base64.encodeToString(rsaKeyPair.getPublic().getEncoded(), Base64.NO_WRAP);
    }
}
