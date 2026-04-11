package com.kosmanenko.vpo_humanitarian_aid_platform.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@Controller
@RequestMapping("/payment")
public class PaymentController {

    @Value("${liqpay.public-key}")
    private String publicKey;

    @Value("${liqpay.private-key}")
    private String privateKey;

    @Value("${app.base-url}")
    private String baseUrl;

    @PostMapping("/platform")
    public String processPlatformDonate(@RequestParam double amount,
                                        Model model,
                                        RedirectAttributes redirectAttributes) {
        if (amount < 1 || amount > 100_000) {
            redirectAttributes.addFlashAttribute("error", "Сума має бути від 1 до 100 000 грн");
            return "redirect:/";
        }
        String data = generateData(amount, "Підтримка платформи ВПО Допомога");
        String signature = generateSignature(data);
        model.addAttribute("data", data);
        model.addAttribute("signature", signature);
        return "payment/checkout";
    }

    @GetMapping("/result")
    public String result() {
        return "payment/result";
    }

    @PostMapping("/callback")
    @ResponseBody
    public String callback() {
        return "OK";
    }

    private String generateData(double amount, String description) {
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
    private String generateSignature(String data) {
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
