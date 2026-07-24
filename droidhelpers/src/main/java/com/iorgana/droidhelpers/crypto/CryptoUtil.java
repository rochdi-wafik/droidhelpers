package com.iorgana.droidhelpers.crypto;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import com.orhanobut.logger.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import javax.crypto.Cipher;

import javax.crypto.KeyGenerator;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;


/**
 * ************************************************************************
 * CryptoUtil
 * ************************************************************************
 * - Methods for encryption and decryption
 * // TODO: 7/24/2026 Maybe we need to change the class name?
 */
public class CryptoUtil {
    private static final String TAG = "__StringCrypto";

    // Use GCM for Authenticated Encryption (prevents malleability/padding oracles)
    public static final String CIPHER_TRANS = "AES/GCM/NoPadding";
    public static final String CIPHER_ALGO = "AES";

    // GCM standard IV length is 12 bytes (96 bits)
    private static final int GCM_IV_LENGTH = 12;
    // GCM authentication tag length is 16 bytes (128 bits)
    private static final int GCM_TAG_LENGTH = 128;

    /**
     * Helper to derive a robust 256-bit key from any given string.
     * Replaces the vulnerable 16-byte array truncation.
     */
    private static SecretKeySpec deriveKey(String key) throws NoSuchAlgorithmException {
        // Hashing the string ensures we always get a valid 32-byte (256-bit) key,
        // mitigating the risk of short or poorly-sized passwords.
        // Note: For true password-based encryption, PBKDF2/HKDF with a stored random salt is preferred.
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = digest.digest(key.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(keyBytes, CIPHER_ALGO);
    }

    /**
     * ----------------------------------------------------------------------------
     * Cipher Encrypt (Bytes)
     * ----------------------------------------------------------------------------
     * @param plainBytes bytes data to be encrypted
     * @param key key used for encryption
     * @return byte array containing [IV (12 bytes)] + [Ciphertext]
     */
    public static byte[] cipherEncrypt(byte[] plainBytes, final String key) {
        try {
            SecretKeySpec skeySpec = deriveKey(key);
            Cipher cipher = Cipher.getInstance(CIPHER_TRANS);

            // Generate a random IV for every encryption call (prevents deterministic patterns)
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);

            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, gcmSpec);

            byte[] cipherText = cipher.doFinal(plainBytes);

            // Prepend the IV to the encrypted data so we can extract it for decryption
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);
            return byteBuffer.array();

        } catch (Exception e) {
            Logger.e(TAG + " cipherEncrypt(): unable to encrypt bytes: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * ----------------------------------------------------------------------------
     * Cipher Decrypt (Bytes)
     * ---------------------------------------------------------------------------
     * @param cipherBytes byte array containing [IV (12 bytes)] + [Ciphertext]
     * @param key must be the same key used for encryption
     * @return decrypted data
     */
    public static byte[] cipherDecrypt(byte[] cipherBytes, final String key) {
        try {
            if (cipherBytes == null || cipherBytes.length < GCM_IV_LENGTH) {
                throw new IllegalArgumentException("Invalid cipher bytes: Too short to contain IV");
            }

            SecretKeySpec skeySpec = deriveKey(key);
            Cipher cipher = Cipher.getInstance(CIPHER_TRANS);

            // Extract the 12-byte IV from the front of the payload
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, cipherBytes, 0, GCM_IV_LENGTH);
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, gcmSpec);

            // Decrypt the remaining bytes
            return cipher.doFinal(cipherBytes, GCM_IV_LENGTH, cipherBytes.length - GCM_IV_LENGTH);

        } catch (Exception e) {
            Logger.e(TAG + " cipherDecrypt(): unable to decrypt bytes: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * ----------------------------------------------------------------------------
     * Cipher Encrypt (String)
     * ----------------------------------------------------------------------------
     * @param plainText String data to be encrypted
     * @param key key used for encryption
     * @return Base64 string of the IV + Ciphertext
     */
    public static String cipherEncrypt(String plainText, final String key) {
        try {
            byte[] aesData = cipherEncrypt(plainText.getBytes(StandardCharsets.UTF_8), key);
            if (aesData == null) return null;
            return Base64.encodeToString(aesData, Base64.NO_WRAP | Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
            Logger.e(TAG + " cipherEncrypt(): unable to encrypt string, " + e.getMessage());
            return null;
        }
    }

    /**
     * ----------------------------------------------------------------------------
     * Cipher Decrypt (String)
     * ----------------------------------------------------------------------------
     * @param encryptedText Base64 string containing IV + Ciphertext
     * @param key must be the same key used for encryption
     * @return decrypted String data
     */
    public static String cipherDecrypt(String encryptedText, final String key) {
        try {
            byte[] decoded = Base64.decode(encryptedText, Base64.DEFAULT);
            byte[] aesDecrypted = cipherDecrypt(decoded, key);
            if (aesDecrypted == null) return null;
            return new String(aesDecrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
            Logger.e(TAG + " cipherDecrypt(): unable to decrypt string, " + e.getMessage());
            return null;
        }
    }

    /**
     * --------------------------------------------------------------
     * XOR Encrypt
     * --------------------------------------------------------------
     * ⚠️ WARNING: THIS IS NOT ENCRYPTION. THIS IS OBFUSCATION ONLY. ⚠️
     * XOR is trivially reversible via known-plaintext attacks and
     * provides ZERO cryptographic security. Do NOT use this for sensitive
     * data, passwords, or secure communications.
     * --------------------------------------------------------------
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
     * --------------------------------------------------------------
     * XOR Decrypt
     * --------------------------------------------------------------
     * ⚠️ WARNING: OBFUSCATION ONLY. NOT CRYPTOGRAPHICALLY SECURE. ⚠️
     * --------------------------------------------------------------
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
     * --------------------------------------------------------------
     * XOR Encrypt Decrypt
     * --------------------------------------------------------------
     * ⚠️ WARNING: OBFUSCATION ONLY. NOT CRYPTOGRAPHICALLY SECURE. ⚠️
     * --------------------------------------------------------------
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
     * ****************************************************************
     * Verify RSA Signature
     * ****************************************************************
     * - This replaces HMAC verification with RSA signature verification for better security.
     * - This method verifies the RSA signature of the given data using the provided public key.
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

    /**
     * ****************************************************************
     * Get or Create Master Key Alias
     * ****************************************************************
     * - This method checks if a master key alias exists in the Android Keystore.
     * - If it doesn't exist, it creates a new AES key with the specified parameters.
     * - Returns the alias of the master key.
     * @return The alias of the master key in the Android Keystore.
     * @throws GeneralSecurityException
     * @throws IOException
     */
    public static String getOrCreateMasterKeyAlias() throws GeneralSecurityException, IOException {
        final String alias = "_androidx_security_master_key_";
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        if (!keyStore.containsAlias(alias)) {
            KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(
                    alias, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build();
            KeyGenerator keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            keyGenerator.init(spec);
            keyGenerator.generateKey();
        }
        return alias;
    }
}
