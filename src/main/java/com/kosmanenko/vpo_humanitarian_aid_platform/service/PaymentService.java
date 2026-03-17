package com.kosmanenko.vpo_humanitarian_aid_platform.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * Сервіс для інтеграції з платіжною системою LiqPay (API v3).
 * Реалізує генерацію параметрів платіжної форми відповідно до документації LiqPay:
 *   Формує JSON з параметрами платежу
 *   Кодує JSON у Base64 ({@code data})
 *   Генерує підпис: {@code base64(sha1(privateKey + data + privateKey))}
 * Параметри підключення (public_key та private_key) беруться з конфігурації ({@code .env}).
 */
@Service
public class PaymentService {

    /** Публічний ключ LiqPay (ідентифікатор мерчанта). */
    @Value("${liqpay.public-key}")
    private String publicKey;

    /** Приватний ключ LiqPay (секрет для генерації підпису; не передається клієнту). */
    @Value("${liqpay.private-key}")
    private String privateKey;

    /** Базова URL адреса додатку (для формування callback та result URL). */
    @Value("${app.base-url}")
    private String baseUrl;

    /**
     * Генерує Base64-закодований рядок даних платіжної форми LiqPay.
     * Формує JSON з такими параметрами:
     *   {@code version}: версія API (завжди "3")
     *   {@code public_key}: публічний ключ мерчанта
     *   {@code action}: "pay" — звичайний платіж
     *   {@code amount}: сума в гривнях з 2 знаками після коми
     *   {@code currency}: "UAH"
     *   {@code description}: опис платежу для відображення платнику
     *   {@code order_id}: унікальний ідентифікатор замовлення (order_ID_TIMESTAMP)
     *   {@code sandbox}: "1" — тестовий режим (без реального списання)
     *   {@code result_url}: URL для перенаправлення після оплати
     *   {@code server_url}: URL для callback від LiqPay
     *
     * @param announcementId ідентифікатор оголошення (для формування order_id та URL)
     * @param amount         сума пожертви в гривнях
     * @param description    опис призначення платежу
     * @return рядок даних, закодований Base64
     */
    public String generateData(Long announcementId, double amount, String description) {
        // Формуємо JSON параметри платежу відповідно до специфікації LiqPay API v3
        String json = String.format(
            "{\"version\":\"3\",\"public_key\":\"%s\",\"action\":\"pay\"," +
            "\"amount\":\"%.2f\",\"currency\":\"UAH\",\"description\":\"%s\"," +
            "\"order_id\":\"order_%d_%d\",\"sandbox\":\"1\"," +
            "\"result_url\":\"%s/payment/result?id=%d\"," +
            "\"server_url\":\"%s/payment/callback\"}",
            publicKey, amount, description, announcementId, System.currentTimeMillis(),
            baseUrl, announcementId, baseUrl);
        // Кодуємо JSON у Base64 для передачі у форму
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Генерує підпис (signature) для запиту до LiqPay.
     * Алгоритм підпису LiqPay API v3:
     * signature = base64( sha1( privateKey + data + privateKey ) )
     * використовується SHA-1
     * @param data Base64-рядок даних, згенерований {@link #generateData}
     * @return Base64-закодований SHA-1 підпис
     * @throws RuntimeException якщо алгоритм SHA-1 недоступний у JVM
     */
    public String generateSignature(String data) {
        try {
            // Конкатенація: приватний ключ + дані + приватний ключ (специфікація LiqPay)
            String str = privateKey + data + privateKey;
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            byte[] hash = sha1.digest(str.getBytes(StandardCharsets.UTF_8));
            // Кодуємо бінарний хеш у Base64 для передачі в HTTP-параметрі
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Error generating LiqPay signature", e);
        }
    }
}
