package com.iorgana.droidhelpers.crypto;

import android.util.Base64;

import java.nio.charset.Charset;

public class Base64Helper {


    /**
     * Decode encoded Base64Url
     * --------------------------------------------------------------------------
     */
    public static String base64UrlDecode(String base64Url){
        String base64 = base64Url.replace("-", "+").replace("_", "/");
        byte[] decodedBytes = Base64.decode(base64, Base64.DEFAULT);
        return new String(decodedBytes, Charset.defaultCharset());
    }

}
