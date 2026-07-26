package com.iorgana.droidhelpers.crypto;


import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * ************************************************************************
 * HmacVerifier
 * ************************************************************************
 * - Verifies the integrity of V2 API responses using HMAC-SHA256.
 * - The server signs the raw data JSON with HMAC-SHA256 (hex-encoded)
 *   using a shared secret; the client recomputes and compares.
 * - A mismatch means the response was tampered with.
 * - The secret is delivered dynamically, never hardcoded.
 */
public class HmacVerifier {

    /**
     * ************************************************************************
     * verify()
     * ************************************************************************
     * - Recomputes HMAC-SHA256(payload, secret) as lowercase hex and compares
     *   it with the server's signature using constant-time comparison.
     * - Fail-closed: returns false on null signature/secret or any crypto error.
     * ------------------------------------------------------------------------
     * @param payload   Raw JSON string that was signed.
     * @param signature Hex HMAC signature from the server response.
     * @param secret    Shared HMAC secret.
     * @return true only if the signature matches; false otherwise.
     */
    public static boolean verify(String payload, String signature, String secret) {
        if (signature == null || secret == null) return false;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : raw) hex.append(String.format("%02x", b));
            // Constant-time comparison
            return java.security.MessageDigest.isEqual(
                    hex.toString().getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) { return false; }
    }
}