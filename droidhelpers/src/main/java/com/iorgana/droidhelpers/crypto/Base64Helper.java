package com.iorgana.droidhelpers.crypto;

import android.util.Base64;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * ************************************************************************
 * Base64Helper
 * ************************************************************************
 * Helper methods for Base64 URL encoding and decoding.
 */
public class Base64Helper {


    /**
     * ************************************************************************
     * base64UrlDecode()
     * ************************************************************************
     * - Decode a Base64URL-encoded string to a plain string.
     * ------------------------------------------------------------------------
     * @param base64Url The Base64URL-encoded string.
     * @return The decoded string.
     */
    public static String base64UrlDecode(String base64Url){
        String base64 = base64Url.replace("-", "+").replace("_", "/");
        byte[] decodedBytes = Base64.decode(base64, Base64.DEFAULT);
        return new String(decodedBytes, StandardCharsets.UTF_8);
    }

}