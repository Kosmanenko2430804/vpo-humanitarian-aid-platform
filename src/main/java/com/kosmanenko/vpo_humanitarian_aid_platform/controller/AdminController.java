package com.kosmanenko.vpo_humanitarian_aid_platform.controller;

import com.kosmanenko.vpo_humanitarian_aid_platform.service.AnnouncementService;
import com.kosmanenko.vpo_humanitarian_aid_platform.service.ComplaintService;
import com.kosmanenko.vpo_humanitarian_aid_platform.service.ModerationService;
import com.kosmanenko.vpo_humanitarian_aid_platform.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final AnnouncementService announcementService;
    private final ModerationService moderationService;
    private final ComplaintService complaintService;
    private final UserService userService;

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("pendingCount", announcementService.findPending().size());
        model.addAttribute("complaintsCount", complaintService.findPending().size());
        return "admin/dashboard";
    }

    @GetMapping("/moderation")
    public String moderation(Model model) {
        model.addAttribute("announcements", announcementService.findPending());
        return "admin/moderation";
    }

    @PostMapping("/moderation/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable Long id,
                                     @RequestHeader(value = "HX-Request", required = false) String htmxRequest,
                                     RedirectAttributes redirectAttributes) {
        announcementService.approve(id);
        if (htmxRequest != null) {
            return ResponseEntity.ok("");
        }
        redirectAttributes.addFlashAttribute("success", "Оголошення опубліковано");
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, "/admin/moderation").build();
    }

    @PostMapping("/moderation/{id}/reject")
    public ResponseEntity<?> reject(@PathVariable Long id,
                                    @RequestParam String reason,
                                    @RequestHeader(value = "HX-Request", required = false) String htmxRequest,
                                    RedirectAttributes redirectAttributes) {
        announcementService.reject(id, reason);
        if (htmxRequest != null) {
            return ResponseEntity.ok("");
        }
        redirectAttributes.addFlashAttribute("success", "Оголошення відхилено");
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, "/admin/moderation").build();
    }

    @GetMapping("/complaints")
    public String complaints(Model model) {
        model.addAttribute("complaints", complaintService.findAll());
        return "admin/complaints";
    }

    @PostMapping("/complaints/{id}/block")
    public String blockAnnouncement(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        moderationService.blockAnnouncementFromComplaint(id);
        redirectAttributes.addFlashAttribute("success", "Оголошення заблоковано");
        return "redirect:/admin/complaints";
    }

    @PostMapping("/complaints/{id}/dismiss")
    public String dismissComplaint(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        moderationService.dismissComplaint(id);
        redirectAttributes.addFlashAttribute("success", "Скаргу відхилено");
        return "redirect:/admin/complaints";
    }

    @GetMapping("/history")
    public String history(Model model) {
        model.addAttribute("announcements", announcementService.findReviewed());
        return "admin/history";
    }

    @GetMapping("/users")
    public String users(Model model) {
        model.addAttribute("users", userService.findAll());
        return "admin/users";
    }

    @PostMapping("/users/{id}/toggle-block")
    public String toggleBlock(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        userService.toggleBlock(id);
        redirectAttributes.addFlashAttribute("success", "Статус користувача змінено");
        return "redirect:/admin/users";
    }
}
