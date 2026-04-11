package com.kosmanenko.vpo_humanitarian_aid_platform.controller;

import com.kosmanenko.vpo_humanitarian_aid_platform.service.AnnouncementService;
import com.kosmanenko.vpo_humanitarian_aid_platform.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
    private final UserService userService;

    @ModelAttribute
    public void populateSidebar(Authentication auth, Model model) {
        if (auth != null) {
            model.addAttribute("user", userService.findByAuthentication(auth));
        }
        model.addAttribute("pendingCount", announcementService.countPending());
    }

    @GetMapping
    public String dashboard() {
        return "redirect:/admin/moderation";
    }

    @GetMapping("/moderation")
    public String moderation(Model model) {
        model.addAttribute("announcements", announcementService.findPending());
        return "admin/moderation";
    }

    @PostMapping("/moderation/{id}/approve")
    public String approve(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        announcementService.approve(id);
        redirectAttributes.addFlashAttribute("success", "Оголошення опубліковано");
        return "redirect:/admin/moderation";
    }

    @PostMapping("/moderation/{id}/reject")
    public String reject(@PathVariable Long id,
                         @RequestParam String reason,
                         RedirectAttributes redirectAttributes) {
        announcementService.reject(id, reason);
        redirectAttributes.addFlashAttribute("success", "Оголошення відхилено");
        return "redirect:/admin/moderation";
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
