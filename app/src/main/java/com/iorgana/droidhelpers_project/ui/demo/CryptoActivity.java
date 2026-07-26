package com.iorgana.droidhelpers_project.ui.demo;

import android.util.Base64;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.iorgana.droidhelpers.crypto.Base64Helper;
import com.iorgana.droidhelpers.crypto.CryptoUtil;
import com.iorgana.droidhelpers.crypto.HmacVerifier;
import com.iorgana.droidhelpers_project.ui.base.BaseDemoActivity;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * CryptoActivity
 * -----------------------------------------------------------------------------
 * Live usage examples for com.iorgana.droidhelpers.crypto package:
 * CryptoUtil (AES-GCM cipher, XOR obfuscation, RSA signature verification),
 * HmacVerifier (HMAC-SHA256 payload integrity check), Base64Helper.
 */
public class CryptoActivity extends BaseDemoActivity {

    @Override
    protected String getScreenTitle() {
        return "Encryption";
    }

    @Override
    protected void buildContent() {
        buildCryptoUtilSection();
        buildHmacVerifierSection();
        buildBase64HelperSection();
    }

    /* ------------------------------------------------------------------ */
    private void buildCryptoUtilSection() {
        LinearLayout s = addSection("CryptoUtil",
                "AES-GCM authenticated encryption, XOR obfuscation (NOT secure), and RSA signature verification.");

        EditText textInput = addInput(s, "Text to encrypt", "Sensitive networking config");
        EditText keyInput = addInput(s, "Secret key (any length, internally SHA-256 derived)", "MySuperSecretAppKey");

        runSafe(addRow(s, "cipherEncrypt(String, key) / cipherDecrypt(String, key)  (AES-GCM)"), () -> {
            String encrypted = CryptoUtil.cipherEncrypt(textInput.getText().toString(), keyInput.getText().toString());
            String decrypted = CryptoUtil.cipherDecrypt(encrypted, keyInput.getText().toString());
            return "encrypted=" + trim(encrypted) + "\ndecrypted back=" + decrypted;
        });

        runSafe(addRow(s, "cipherEncrypt(byte[], key) / cipherDecrypt(byte[], key)"), () -> {
            byte[] plainBytes = textInput.getText().toString().getBytes(StandardCharsets.UTF_8);
            byte[] cipherBytes = CryptoUtil.cipherEncrypt(plainBytes, keyInput.getText().toString());
            byte[] decryptedBytes = CryptoUtil.cipherDecrypt(cipherBytes, keyInput.getText().toString());
            return "cipher bytes length=" + cipherBytes.length
                    + "\ndecrypted back=" + new String(decryptedBytes, StandardCharsets.UTF_8);
        });

        runSafe(addRow(s, "xorEncrypt(data, key) / xorDecrypt(data, key)  \u26A0\uFE0F obfuscation only, not secure"), () -> {
            String encoded = CryptoUtil.xorEncrypt(textInput.getText().toString(), keyInput.getText().toString());
            String decoded = CryptoUtil.xorDecrypt(encoded, keyInput.getText().toString());
            return "xor-encoded=" + trim(encoded) + "\ndecoded back=" + decoded;
        });

        runSafe(addRow(s, "xorEncryptDecrypt(data, key)  (symmetric single call, apply twice to round-trip)"), () -> {
            String once = CryptoUtil.xorEncryptDecrypt(textInput.getText().toString(), keyInput.getText().toString());
            String twice = CryptoUtil.xorEncryptDecrypt(once, keyInput.getText().toString());
            return "after 1 call=" + trim(once) + "\nafter 2nd call (restored)=" + twice;
        });

        runSafe(addRow(s, "verifyRSASignature(data, signature, publicKey)  (keypair generated in-memory for this demo)"), () -> {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair keyPair = generator.generateKeyPair();

            byte[] data = textInput.getText().toString().getBytes(StandardCharsets.UTF_8);
            Signature signer = Signature.getInstance("SHA256withRSA");
            signer.initSign(keyPair.getPrivate());
            signer.update(data);
            String signatureBase64 = Base64.encodeToString(signer.sign(), Base64.NO_WRAP);
            String publicKeyBase64 = Base64.encodeToString(keyPair.getPublic().getEncoded(), Base64.NO_WRAP);

            boolean isValid = CryptoUtil.verifyRSASignature(data, signatureBase64, publicKeyBase64);
            return "signature valid = " + isValid;
        });

        runSafe(addRow(s, "getOrCreateMasterKeyAlias()  (Android Keystore AES key alias)"), CryptoUtil::getOrCreateMasterKeyAlias);
    }

    /* ------------------------------------------------------------------ */
    private void buildHmacVerifierSection() {
        LinearLayout s = addSection("HmacVerifier",
                "Recomputes HMAC-SHA256 over a payload and compares in constant time - detects tampered API responses.");

        EditText payloadInput = addInput(s, "Payload (e.g. JSON from a server)", "{\"status\":\"ACTIVE\"}");
        EditText secretInput = addInput(s, "Shared secret", "shared-hmac-secret");

        runSafe(addRow(s, "verify(payload, signature, secret)  with a CORRECT signature"), () -> {
            String correctSignature = hmacHex(payloadInput.getText().toString(), secretInput.getText().toString());
            boolean valid = HmacVerifier.verify(payloadInput.getText().toString(), correctSignature, secretInput.getText().toString());
            return "computed signature=" + trim(correctSignature) + "\nverify() = " + valid;
        });

        runSafe(addRow(s, "verify(payload, signature, secret)  with a TAMPERED signature"), () -> {
            boolean valid = HmacVerifier.verify(payloadInput.getText().toString(), "0000tampered0000", secretInput.getText().toString());
            return "verify() = " + valid + "  (correctly rejected)";
        });
    }

    /* ------------------------------------------------------------------ */
    private void buildBase64HelperSection() {
        LinearLayout s = addSection("Base64Helper", "Decodes URL-safe Base64 (RFC 4648 §5) back to a normal string.");

        String sampleBase64Url = Base64.encodeToString("hello-from-droidhelpers".getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP)
                .replace("+", "-").replace("/", "_");
        EditText base64Input = addInput(s, "Base64Url-encoded text", sampleBase64Url);

        runSafe(addRow(s, "base64UrlDecode(base64Url)"), () -> Base64Helper.base64UrlDecode(base64Input.getText().toString()));
    }

    /* ------------------------------------------------------------------ */
    private static String hmacHex(String payload, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : raw) hex.append(String.format("%02x", b));
        return hex.toString();
    }

    private static String trim(String s) {
        if (s == null) return "null";
        return s.length() > 200 ? s.substring(0, 200) + "..." : s;
    }
}