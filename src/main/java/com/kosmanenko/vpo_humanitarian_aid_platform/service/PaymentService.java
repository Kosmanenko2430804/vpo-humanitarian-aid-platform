package com.kosmanenko.vpo_humanitarian_aid_platform.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@Service
public class PaymentService {

    @Value("${liqpay.public-key}")
    private String publicKey;

    @Value("${liqpay.private-key}")
    private String privateKey;

    @Value("${app.base-url}")
    private String baseUrl;

    public String generateData(double amount, String description) {
        String json = String.format(
            "{\"version\":\"3\",\"public_key\":\"%s\",\"action\":\"pay\"," +
            "\"amount\":\"%.2f\",\"currency\":\"UAH\",\"description\":\"%s\"," +
            "\"order_id\":\"order_%d\",\"sandbox\":\"1\"," +
            "\"result_url\":\"%s/payment/result\"," +
            "\"server_url\":\"%s/payment/callback\"}",
            publicKey, amount, description, System.currentTimeMillis(),
            baseUrl, baseUrl);
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    // signature = base64(sha1(privateKey + data + privateKey))
    public String generateSignature(String data) {
        try {
            String str = privateKey + data + privateKey;
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            byte[] hash = sha1.digest(str.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Error generating LiqPay signature", e);
        }
    }
}
