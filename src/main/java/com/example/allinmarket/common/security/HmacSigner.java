package com.example.allinmarket.common.security;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
public class HmacSigner {

    private static final String ALGORITHM = "HmacSHA256";

    public static String sign(String secret, String message) {

        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("secret must not be null or blank");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message must not be null or blank");
        }
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8),
                    ALGORITHM
            );

            mac.init(keySpec);

            byte[] hmac = mac.doFinal(
                    message.getBytes(StandardCharsets.UTF_8)
            );

            return Base64.getEncoder().encodeToString(hmac);

        } catch (Exception e) {
            log.error("HMAC signing failed", e);
            throw new IllegalStateException("HMAC signing failed", e);
        }
    }
}
