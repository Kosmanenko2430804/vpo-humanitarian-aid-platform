package com.kosmanenko.vpo_humanitarian_aid_platform.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentServiceTest {

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService();
        ReflectionTestUtils.setField(paymentService, "publicKey", "test_public_key");
        ReflectionTestUtils.setField(paymentService, "privateKey", "test_private_key");
        ReflectionTestUtils.setField(paymentService, "baseUrl", "http://localhost:8080");
    }

    // ==================== generateData ====================

    @Test
    @DisplayName("generateData — повертає валідний Base64 рядок")
    void generateData_returnsValidBase64() {
        String data = paymentService.generateData(1L, 100.0, "Пожертва");

        // Має декодуватись без виключень
        byte[] decoded = Base64.getDecoder().decode(data);
        assertThat(decoded).isNotEmpty();
    }

    @Test
    @DisplayName("generateData — JSON містить правильні параметри платежу")
    void generateData_jsonContainsCorrectParams() {
        String data = paymentService.generateData(42L, 250.50, "Допомога ВПО");

        String json = new String(Base64.getDecoder().decode(data), StandardCharsets.UTF_8);
        assertThat(json).contains("\"version\":\"3\"");
        assertThat(json).contains("\"public_key\":\"test_public_key\"");
        assertThat(json).contains("\"action\":\"pay\"");
        // Сума може бути 250.50 або 250,50 залежно від локалі JVM
        assertThat(json).containsAnyOf("\"amount\":\"250.50\"", "\"amount\":\"250,50\"");
        assertThat(json).contains("\"currency\":\"UAH\"");
        assertThat(json).contains("\"description\":\"Допомога ВПО\"");
        assertThat(json).contains("\"sandbox\":\"1\"");
        assertThat(json).contains("order_42_");
    }

    @Test
    @DisplayName("generateData — містить посилання на сторінку результату платежу")
    void generateData_containsResultUrl() {
        String data = paymentService.generateData(10L, 50.0, "Тест");

        String json = new String(Base64.getDecoder().decode(data), StandardCharsets.UTF_8);
        assertThat(json).contains("result_url");
        assertThat(json).contains("id=10");
    }

    // ==================== generateSignature ====================

    @Test
    @DisplayName("generateSignature — повертає не порожній Base64 рядок")
    void generateSignature_returnsNonEmptyBase64() {
        String data = paymentService.generateData(1L, 100.0, "Тест");

        String signature = paymentService.generateSignature(data);

        assertThat(signature).isNotBlank();
        // Має бути валідним Base64
        assertThat(Base64.getDecoder().decode(signature)).hasSize(20); // SHA-1 = 20 bytes
    }

}
