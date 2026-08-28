package com.iorgana.droidhelpers.crypto;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import com.orhanobut.logger.Logger;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;


/**
 * ************************************************************************
 * CryptoUtil
 * ************************************************************************
 * - Methods for encryption, decryption, and cryptographic operations.
 *
 * [AES key]
 * - The AES key lives in the Android Keystore and is fetched by alias.
 * - There is no string key to pass, mismatch, or leak. Callers that used
 *   to pass a key no longer do; the key is internal.
 * - The key is hardware-backed on most devices and non-exportable, so the
 *   ciphertext is meaningful even on a rooted device.
 * - The key is per-install. It does not survive a reinstall, and some
 *   devices drop it on a lock-screen credential change. When that happens
 *   old rows stop decrypting; callers must treat a failed decrypt as
 *   absent data, never as a value.
 * ------------------------------------------------------------------------
 * @author Rochdi Wafik
 * @lastUpdate 28-08-2026
 */
public class CryptoUtil {
    private static final String TAG = "__StringCrypto";

    // Use GCM for Authenticated Encryption (prevents malleability/padding oracles)
    public static final String CIPHER_TRANS = "AES/GCM/NoPadding";

    // GCM standard IV length is 12 bytes (96 bits)
    private static final int GCM_IV_LENGTH = 12;
    // GCM authentication tag length is 16 bytes (128 bits)
    private static final int GCM_TAG_LENGTH = 128;

    // Android Keystore
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    // Alias of the AES key used by this library. Changing it orphans existing data.
    private static final String KEY_ALIAS = "iorgana_droidhelpers_aes_key";

    /**
     * ************************************************************************
     * getOrCreateKey() (Private)
     * ************************************************************************
     * - Return the library's AES key from the Android Keystore, creating it
     *   on first use.
     * ------------------------------------------------------------------------
     * @return The 256-bit AES key held in the Keystore.
     * @throws Exception if the Keystore is unavailable or key creation fails.
     */
    private static SecretKey getOrCreateKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
        keyStore.load(null);

        KeyStore.Entry entry = keyStore.getEntry(KEY_ALIAS, null);
        if (entry instanceof KeyStore.SecretKeyEntry) {
            return ((KeyStore.SecretKeyEntry) entry).getSecretKey();
        }

        KeyGenerator keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
        keyGenerator.init(new KeyGenParameterSpec.Builder(
                KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build());
        return keyGenerator.generateKey();
    }

    /**
     * ************************************************************************
     * cipherEncrypt() (Bytes)
     * ************************************************************************
     * - Encrypt byte data using AES/GCM/NoPadding.
     * ------------------------------------------------------------------------
     * @param plainBytes Bytes data to be encrypted.
     * @return Byte array containing [IV (12 bytes)] + [Ciphertext], or null on failure.
     */
    public static byte[] cipherEncrypt(byte[] plainBytes) {
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_TRANS);
            // A Keystore GCM key generates its own IV; we read it back rather than supply one.
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey());
            byte[] iv = cipher.getIV();
            byte[] cipherText = cipher.doFinal(plainBytes);

            // Prepend the IV so decryption can extract it
            return ByteBuffer.allocate(iv.length + cipherText.length)
                    .put(iv)
                    .put(cipherText)
                    .array();
        } catch (Exception e) {
            Logger.e(TAG + " cipherEncrypt(): unable to encrypt bytes: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * ************************************************************************
     * cipherDecrypt() (Bytes)
     * ************************************************************************
     * - Decrypt byte data that was encrypted with cipherEncrypt().
     * ------------------------------------------------------------------------
     * @param cipherBytes Byte array containing [IV (12 bytes)] + [Ciphertext].
     * @return Decrypted data, or null if the input is unreadable.
     */
    public static byte[] cipherDecrypt(byte[] cipherBytes) {
        try {
            if (cipherBytes == null || cipherBytes.length <= GCM_IV_LENGTH) {
                return null;
            }

            Cipher cipher = Cipher.getInstance(CIPHER_TRANS);
            // Extract the 12-byte IV from the front of the payload
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, cipherBytes, 0, GCM_IV_LENGTH);
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), gcmSpec);

            return cipher.doFinal(cipherBytes, GCM_IV_LENGTH, cipherBytes.length - GCM_IV_LENGTH);
        } catch (Exception e) {
            Logger.e(TAG + " cipherDecrypt(): unable to decrypt bytes: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * ************************************************************************
     * cipherEncrypt() (String)
     * ************************************************************************
     * - Encrypt a string using AES/GCM/NoPadding and return Base64.
     * ------------------------------------------------------------------------
     * @param plainText String data to be encrypted.
     * @return Base64 string of the IV + Ciphertext, or null on failure.
     */
    public static String cipherEncrypt(String plainText) {
        if (plainText == null) return null;
        try {
            byte[] aesData = cipherEncrypt(plainText.getBytes(StandardCharsets.UTF_8));
            if (aesData == null) return null;
            return Base64.encodeToString(aesData, Base64.NO_WRAP);
        } catch (Exception e) {
            e.printStackTrace();
            Logger.e(TAG + " cipherEncrypt(): unable to encrypt string, " + e.getMessage());
            return null;
        }
    }

    /**
     * ************************************************************************
     * cipherDecrypt() (String)
     * ************************************************************************
     * - Decrypt a Base64 string that was encrypted with cipherEncrypt().
     * ------------------------------------------------------------------------
     * @param encryptedText Base64 string containing IV + Ciphertext.
     * @return Decrypted string data, or null if the input is unreadable.
     */
    public static String cipherDecrypt(String encryptedText) {
        if (encryptedText == null) return null;
        try {
            byte[] decoded = Base64.decode(encryptedText, Base64.NO_WRAP);
            byte[] aesDecrypted = cipherDecrypt(decoded);
            if (aesDecrypted == null) return null;
            return new String(aesDecrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
            Logger.e(TAG + " cipherDecrypt(): unable to decrypt string, " + e.getMessage());
            return null;
        }
    }

    /**
     * ************************************************************************
     * xorEncrypt()
     * ************************************************************************
     * - XOR-based obfuscation of data.
     * - WARNING: OBFUSCATION ONLY, NOT CRYPTOGRAPHICALLY SECURE.
     * - XOR is trivially reversible. Do NOT use for sensitive data.
     * ------------------------------------------------------------------------
     * @param data      The data to obfuscate.
     * @param secretKey The secret key for XOR operation.
     * @return Base64-encoded obfuscated string.
     */
    public static String xorEncrypt(String data, String secretKey) {
        if (data == null) {
            Log.e(TAG, "xorEncrypt(): null data");
            return null;
        }
        char[] key = secretKey.toCharArray();
        StringBuilder output = new StringBuilder();

        for (int i = 0; i < data.length(); i++) {
            output.append((char) (data.charAt(i) ^ key[i % key.length]));
        }

        byte[] result = output.toString().getBytes();
        return Base64.encodeToString(result, Base64.NO_WRAP | Base64.DEFAULT);
    }

    /**
     * ************************************************************************
     * xorDecrypt()
     * ************************************************************************
     * - Reverse XOR obfuscation to retrieve original data.
     * - WARNING: OBFUSCATION ONLY, NOT CRYPTOGRAPHICALLY SECURE.
     * ------------------------------------------------------------------------
     * @param data      The Base64-encoded obfuscated data.
     * @param secretKey The secret key used for obfuscation.
     * @return The deobfuscated string.
     */
    public static String xorDecrypt(String data, String secretKey) {
        if (data == null) {
            Log.e(TAG, "xorDecrypt(): null data");
            return null;
        }

        byte[] encrypted_bytes = Base64.decode(data, Base64.DEFAULT);
        String input = new String(encrypted_bytes);
        char[] key = secretKey.toCharArray();
        StringBuilder output = new StringBuilder();

        for (int i = 0; i < input.length(); i++) {
            output.append((char) (input.charAt(i) ^ key[i % key.length]));
        }

        return output.toString();
    }

    /**
     * ************************************************************************
     * xorEncryptDecrypt()
     * ************************************************************************
     * - XOR obfuscation/deobfuscation (reversible with same key).
     * - WARNING: OBFUSCATION ONLY, NOT CRYPTOGRAPHICALLY SECURE.
     * ------------------------------------------------------------------------
     * @param data      The data to obfuscate or deobfuscate.
     * @param secretKey The secret key for XOR operation.
     * @return The result of XOR operation.
     */
    public static String xorEncryptDecrypt(String data, String secretKey) {
        if (data == null) {
            Log.e(TAG, "xorEncryptDecrypt(): null data");
            return null;
        }

        char[] key = secretKey.toCharArray();
        StringBuilder output = new StringBuilder();

        for (int i = 0; i < data.length(); i++) {
            output.append((char) (data.charAt(i) ^ key[i % key.length]));
        }

        return output.toString();
    }

    /**
     * ************************************************************************
     * verifyRSASignature()
     * ************************************************************************
     * - Verify an RSA signature of the given data using the provided public key.
     * ------------------------------------------------------------------------
     * @param rawResponseData The raw response data bytes.
     * @param signatureBase64 The Base64-encoded signature.
     * @param publicKeyBase64 The Base64-encoded RSA public key.
     * @return true if the signature is valid, false otherwise.
     */
    public static boolean verifyRSASignature(byte[] rawResponseData, String signatureBase64, String publicKeyBase64) {
        try {
            byte[] keyBytes = Base64.decode(publicKeyBase64, Base64.NO_WRAP);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(spec);

            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(publicKey);
            sig.update(rawResponseData);

            byte[] signatureBytes = Base64.decode(signatureBase64, Base64.NO_WRAP);
            boolean isValid = sig.verify(signatureBytes);
            Logger.i(TAG + " verifyRSASignature(): isValid=" + isValid);
            return isValid;
        } catch (Exception e) {
            // fail closed, any exception means "don't trust this"
            Logger.e(TAG + " verifyRSASignature(): Exception, " + e.getMessage());
            return false;
        }
    }
}