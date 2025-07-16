// Last Update: 24-08-2024

package com.iorgana.droidhelpers.crypto;

import android.util.Base64;
import android.util.Log;

import com.orhanobut.logger.Logger;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;


public class StringCrypto {
    private static final String TAG = "__StringCrypto";
    public static final String CIPHER_TRANS = "AES/CBC/PKCS5Padding";
    public static final String CIPHER_ALGO = "AES";

    /**
     * ----------------------------------------------------------------------------
     * Cipher Encrypt (Bytes)
     * ----------------------------------------------------------------------------
     * @param plainBytes bytes data to be encrypted
     * @param key key used for encryption
     * @return encrypted data
     */
    public static byte[] cipherEncrypt(byte[] plainBytes, final String key){
        try{
            // convert key to bytes
            byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
            // Use the first 16 bytes (or even less if key is shorter)
            byte[] keyBytes16 = new byte[16];
            System.arraycopy(keyBytes, 0, keyBytes16, 0, Math.min(keyBytes.length, 16));

            // setup cipher
            SecretKeySpec skeySpec = new SecretKeySpec(keyBytes16, CIPHER_ALGO);
            Cipher cipher = Cipher.getInstance(CIPHER_TRANS);
            byte[] iv = new byte[16]; // initialization vector with all 0
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, new IvParameterSpec(iv));

            // encrypt bytes
            return  cipher.doFinal(plainBytes);
        }catch (Exception e){
            Logger.e(TAG + " cipherEncrypt(): unable to encrypt bytes: "+e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * ----------------------------------------------------------------------------
     *  Cipher Decrypt (Bytes)
     *  ---------------------------------------------------------------------------
     * @param plainBytes bytes data to be decrypted
     * @param key must be the same key used for encryption
     * @return decrypted data
     */
    public static byte[] cipherDecrypt(byte[] plainBytes, final String key) {
       try{
           // convert key to bytes
           byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
           // Use the first 16 bytes (or even less if key is shorter)
           byte[] keyBytes16 = new byte[16];
           System.arraycopy(keyBytes, 0, keyBytes16, 0, Math.min(keyBytes.length, 16));

           // setup cipher
           SecretKeySpec skeySpec = new SecretKeySpec(keyBytes16, CIPHER_ALGO);
           Cipher cipher = Cipher.getInstance(CIPHER_TRANS);
           byte[] iv = new byte[16]; // initialization vector with all 0
           cipher.init(Cipher.DECRYPT_MODE, skeySpec, new IvParameterSpec(iv));

           // decrypt
           return cipher.doFinal(plainBytes);
       }catch (Exception e){
           Logger.e(TAG + " cipherDecrypt(): unable to decrypt bytes: "+e.getMessage());
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
     * @return encrypted data
     * ----------------------------------------------------------------------------
     * String will be converted to bytes, then bytes will be encrypted
     * Then encrypted bytes will be returned as Base64
     */
    public static String cipherEncrypt(String plainText, final String key){
        try{
            byte[] aesData = cipherEncrypt(plainText.getBytes(StandardCharsets.UTF_8), key);
            String encrypted =  Base64.encodeToString(aesData, Base64.NO_WRAP|Base64.DEFAULT);
            // Logger.d(TAG + " cipherEncrypt(): data = "+encrypted);
            return encrypted;
        }catch (Exception e){
            e.printStackTrace();
            Logger.e(TAG + " cipherEncrypt(): unable to encrypt string, "+e.getMessage());
            return null;
        }
    }

    /**
     * ----------------------------------------------------------------------------
     * Cipher Decrypt (String)
     * ----------------------------------------------------------------------------
     * @param encryptedText String data to be decrypted
     * @param key must be the same key used for encryption
     * @return decrypted String data
     * ----------------------------------------------------------------------------
     * - Given string will be decoded from Base64,
     * - Then bytes will be decrypted
     * - Then decrypted bytes will be converted to String UTF-8
     */
    public static String cipherDecrypt(String encryptedText, final String key){
        try{
            byte[] decoded = Base64.decode(encryptedText, Base64.DEFAULT);
            byte[] aesDecrypted = cipherDecrypt(decoded, key);
            String decrypted =  new String(aesDecrypted, StandardCharsets.UTF_8);
            // Logger.d(TAG + " cipherDecrypt(): data = "+decrypted);
            return decrypted;
        }catch (Exception e){
            e.printStackTrace();
            Logger.e(TAG + " cipherDecrypt(): unable to decrypt string, "+e.getMessage());
            return null;
        }
    }

    /**
     * XOR Encrypt
     * --------------------------------------------------------------
     */
    public static String xorEncrypt(String data, String secretKey) {
        if(data==null){
            Log.e(TAG, "xorEncrypt(): null data");
            return null;
        }
        char[] key = secretKey.toCharArray();
        StringBuilder output = new StringBuilder();

        for(int i = 0; i < data.length(); i++) {
            output.append((char) (data.charAt(i) ^ key[i % key.length]));
        }

        byte[] result = output.toString().getBytes();

        return Base64.encodeToString(result, Base64.NO_WRAP|Base64.DEFAULT);
    }

    /**
     * XOR Decrypt
     * --------------------------------------------------------------
     */
    public static String xorDecrypt(String data, String secretKey){
        if(data==null){
            Log.e(TAG, "xorDecrypt(): null data");
            return null;
        }

        byte[] encrypted_bytes = Base64.decode(data, Base64.DEFAULT);

        String input = new String(encrypted_bytes);

        char[] key = secretKey.toCharArray();
        StringBuilder output = new StringBuilder();

        for(int i = 0; i < input.length(); i++) {
            output.append((char) (input.charAt(i) ^ key[i % key.length]));
        }

        return output.toString();

    }

    /**
     * XOR Encrypt Decrypt
     * --------------------------------------------------------------
     */
    public static String xorEncryptDecrypt(String data, String secretKey) {
        if(data==null){
            Log.e(TAG, "xorEncryptDecrypt(): null data");
            return null;
        }

        char[] key = secretKey.toCharArray();
        StringBuilder output = new StringBuilder();

        for(int i = 0; i < data.length(); i++) {
            output.append((char) (data.charAt(i) ^ key[i % key.length]));
        }

        return output.toString();
    }
}
