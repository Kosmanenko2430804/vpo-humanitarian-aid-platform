package com.kosmanenko.vpo_humanitarian_aid_platform.controller;

import com.kosmanenko.vpo_humanitarian_aid_platform.model.Announcement;
import com.kosmanenko.vpo_humanitarian_aid_platform.model.HelpApplication;
import com.kosmanenko.vpo_humanitarian_aid_platform.model.User;
import com.kosmanenko.vpo_humanitarian_aid_platform.service.AnnouncementService;
import com.kosmanenko.vpo_humanitarian_aid_platform.service.HelpApplicationService;
import com.kosmanenko.vpo_humanitarian_aid_platform.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final HelpApplicationService helpApplicationService;
    private final AnnouncementService announcementService;
    private final UserService userService;

    @PostMapping("/apply/{announcementId}")
    public String apply(@PathVariable Long announcementId,
                        @RequestParam String message,
                        @AuthenticationPrincipal UserDetails userDetails,
                        RedirectAttributes redirectAttributes) {
        User user = userService.findByEmail(userDetails.getUsername()).orElseThrow();
        Announcement announcement = announcementService.findById(announcementId).orElseThrow();
        try {
            helpApplicationService.apply(announcement, user, message);
            redirectAttributes.addFlashAttribute("success", "Заявку надіслано");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/announcements/" + announcementId;
    }

    @PostMapping("/{id}/accept")
    public String accept(@PathVariable Long id,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime pickupDate,
                         @RequestParam(required = false) String pickupLocation,
                         @RequestParam(required = false) String providerPhone,
                         @AuthenticationPrincipal UserDetails userDetails,
                         RedirectAttributes redirectAttributes) {
        if (pickupDate == null || pickupLocation == null || pickupLocation.isBlank()
                || providerPhone == null || providerPhone.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Вкажіть дату, місце та телефон для видачі допомоги");
            return "redirect:/cabinet/applications";
        }
        User user = userService.findByEmail(userDetails.getUsername()).orElseThrow();
        try {
            helpApplicationService.accept(id, pickupDate, pickupLocation, providerPhone, user);
            redirectAttributes.addFlashAttribute("success", "Заявку прийнято");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cabinet/applications";
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id,
                         @RequestParam String reason,
                         @AuthenticationPrincipal UserDetails userDetails,
                         RedirectAttributes redirectAttributes) {
        User user = userService.findByEmail(userDetails.getUsername()).orElseThrow();
        try {
            helpApplicationService.reject(id, reason, user);
            redirectAttributes.addFlashAttribute("success", "Заявку відхилено");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cabinet/applications";
    }

    @PostMapping("/{id}/complete")
    public String complete(@PathVariable Long id,
                           @AuthenticationPrincipal UserDetails userDetails,
                           RedirectAttributes redirectAttributes) {
        User user = userService.findByEmail(userDetails.getUsername()).orElseThrow();
        try {
            helpApplicationService.complete(id, user);
            redirectAttributes.addFlashAttribute("success", "Заявку закрито");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cabinet/applications";
    }

    @PostMapping("/{id}/review")
    public String review(@PathVariable Long id,
                         @RequestParam int rating,
                         @RequestParam(required = false) String review,
                         @AuthenticationPrincipal UserDetails userDetails,
                         RedirectAttributes redirectAttributes) {
        User user = userService.findByEmail(userDetails.getUsername()).orElseThrow();
        try {
            helpApplicationService.leaveReview(id, rating, review, user);
            redirectAttributes.addFlashAttribute("success", "Дякуємо за відгук!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cabinet/applications";
    }
}
