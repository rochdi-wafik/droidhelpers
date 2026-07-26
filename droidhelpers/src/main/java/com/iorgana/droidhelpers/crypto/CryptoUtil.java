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
 * - Methods for encryption, decryption, and cryptographic operations.
 * ------------------------------------------------------------------------
 * @todo Maybe need to change the class name.
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
     * ************************************************************************
     * deriveKey()
     * ************************************************************************
     * - Derive a robust 256-bit AES key from any given string using SHA-256.
     * ------------------------------------------------------------------------
     * @param key The input string to derive the key from.
     * @return A SecretKeySpec for AES encryption.
     * @throws NoSuchAlgorithmException if SHA-256 is not available.
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
     * ************************************************************************
     * cipherEncrypt() (Bytes)
     * ************************************************************************
     * - Encrypt byte data using AES/GCM/NoPadding.
     * ------------------------------------------------------------------------
     * @param plainBytes Bytes data to be encrypted.
     * @param key        The encryption key.
     * @return Byte array containing [IV (12 bytes)] + [Ciphertext].
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
     * ************************************************************************
     * cipherDecrypt() (Bytes)
     * ************************************************************************
     * - Decrypt byte data that was encrypted with cipherEncrypt().
     * ------------------------------------------------------------------------
     * @param cipherBytes Byte array containing [IV (12 bytes)] + [Ciphertext].
     * @param key         Must be the same key used for encryption.
     * @return Decrypted data.
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
     * ************************************************************************
     * cipherEncrypt() (String)
     * ************************************************************************
     * - Encrypt a string using AES/GCM/NoPadding and return Base64.
     * ------------------------------------------------------------------------
     * @param plainText String data to be encrypted.
     * @param key       The encryption key.
     * @return Base64 string of the IV + Ciphertext.
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
     * ************************************************************************
     * cipherDecrypt() (String)
     * ************************************************************************
     * - Decrypt a Base64 string that was encrypted with cipherEncrypt().
     * ------------------------------------------------------------------------
     * @param encryptedText Base64 string containing IV + Ciphertext.
     * @param key           Must be the same key used for encryption.
     * @return Decrypted string data.
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

    /**
     * ************************************************************************
     * getOrCreateMasterKeyAlias()
     * ************************************************************************
     * - Check if a master key alias exists in the Android Keystore.
     * - If not, create a new AES key with the specified parameters.
     * ------------------------------------------------------------------------
     * @return The alias of the master key in the Android Keystore.
     * @throws GeneralSecurityException If a security error occurs.
     * @throws IOException              If an I/O error occurs.
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