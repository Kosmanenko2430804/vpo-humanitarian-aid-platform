package com.kosmanenko.vpo_humanitarian_aid_platform.controller;

import com.kosmanenko.vpo_humanitarian_aid_platform.model.Announcement;
import com.kosmanenko.vpo_humanitarian_aid_platform.service.AnnouncementService;
import com.kosmanenko.vpo_humanitarian_aid_platform.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final AnnouncementService announcementService;

    @GetMapping("/donate/{announcementId}")
    public String donatePage(@PathVariable Long announcementId, Model model) {
        Announcement announcement = announcementService.findById(announcementId).orElseThrow();
        model.addAttribute("announcement", announcement);
        return "payment/donate";
    }

    @PostMapping("/donate/{announcementId}")
    public String processDonate(@PathVariable Long announcementId,
                                @RequestParam double amount,
                                Model model) {
        if (amount < 1 || amount > 100_000) {
            model.addAttribute("error", "Сума має бути від 1 до 100 000 грн");
            Announcement ann = announcementService.findById(announcementId).orElseThrow();
            model.addAttribute("announcement", ann);
            return "payment/donate";
        }
        Announcement announcement = announcementService.findById(announcementId).orElseThrow();
        String data = paymentService.generateData(announcementId, amount,
            "Підтримка для: " + announcement.getTitle());
        String signature = paymentService.generateSignature(data);
        model.addAttribute("data", data);
        model.addAttribute("signature", signature);
        model.addAttribute("announcement", announcement);
        return "payment/checkout";
    }

    @GetMapping("/result")
    public String result(@RequestParam Long id,
                         @RequestParam(required = false) String status,
                         Model model) {
        announcementService.findById(id).ifPresent(a -> model.addAttribute("announcement", a));
        model.addAttribute("paymentStatus", status);
        return "payment/result";
    }

    @PostMapping("/callback")
    @ResponseBody
    public String callback(@RequestParam String data, @RequestParam String signature) {
        // LiqPay callback - verify and process
        return "OK";
    }
}
