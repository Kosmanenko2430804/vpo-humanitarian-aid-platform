package com.kosmanenko.vpo_humanitarian_aid_platform.controller;

import com.kosmanenko.vpo_humanitarian_aid_platform.enums.AnnouncementType;
import com.kosmanenko.vpo_humanitarian_aid_platform.enums.UserRole;
import com.kosmanenko.vpo_humanitarian_aid_platform.model.Announcement;
import com.kosmanenko.vpo_humanitarian_aid_platform.model.User;
import com.kosmanenko.vpo_humanitarian_aid_platform.repository.CategoryRepository;
import com.kosmanenko.vpo_humanitarian_aid_platform.service.AnnouncementService;
import com.kosmanenko.vpo_humanitarian_aid_platform.service.ComplaintService;
import com.kosmanenko.vpo_humanitarian_aid_platform.service.HelpApplicationService;
import com.kosmanenko.vpo_humanitarian_aid_platform.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;
    private final HelpApplicationService helpApplicationService;
    private final ComplaintService complaintService;
    private final CategoryRepository categoryRepository;
    private final UserService userService;

    @GetMapping
    public String list(@RequestParam(required = false) String type,
                       @RequestParam(required = false) String city,
                       @RequestParam(required = false) Long categoryId,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        AnnouncementType announcementType = null;
        if (type != null && !type.isBlank()) {
            try { announcementType = AnnouncementType.valueOf(type); } catch (Exception ignored) {}
        }
        Page<Announcement> announcements = announcementService.search(
            announcementType, city, categoryId, keyword, page);
        model.addAttribute("announcements", announcements);
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("currentType", type);
        model.addAttribute("currentCity", city);
        model.addAttribute("currentCategoryId", categoryId);
        model.addAttribute("currentKeyword", keyword);
        return "announcements/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id,
                         @AuthenticationPrincipal UserDetails userDetails,
                         Model model) {
        Announcement announcement = announcementService.findById(id)
            .orElseThrow(() -> new RuntimeException("Не знайдено"));
        model.addAttribute("announcement", announcement);

        if (userDetails != null) {
            userService.findByEmail(userDetails.getUsername()).ifPresent(user -> {
                model.addAttribute("currentUser", user);
                if (user.getRole() == UserRole.VPO) {
                    model.addAttribute("alreadyApplied",
                        helpApplicationService.alreadyApplied(announcement, user));
                }
            });
        }
        return "announcements/detail";
    }

    @GetMapping("/new")
    public String newForm(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("categories", categoryRepository.findAll());
        userService.findByEmail(userDetails.getUsername())
            .ifPresent(u -> model.addAttribute("currentUser", u));
        return "announcements/form";
    }

    @PostMapping("/new")
    public String create(@RequestParam String title,
                         @RequestParam String description,
                         @RequestParam String city,
                         @RequestParam(required = false) Boolean acceptsApplications,
                         @RequestParam(required = false) List<Long> categoryIds,
                         @AuthenticationPrincipal UserDetails userDetails,
                         RedirectAttributes redirectAttributes) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Оберіть принаймні одну категорію");
            return "redirect:/announcements/new";
        }
        User user = userService.findByEmail(userDetails.getUsername()).orElseThrow();
        AnnouncementType type = user.getRole() == UserRole.VPO ? AnnouncementType.REQUEST : AnnouncementType.OFFER;

        try {
            announcementService.create(title, description, city, type, acceptsApplications, categoryIds, user);
            redirectAttributes.addFlashAttribute("success", "Оголошення надіслано на модерацію");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cabinet";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id,
                           @AuthenticationPrincipal UserDetails userDetails,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        Announcement announcement = announcementService.findById(id).orElseThrow();
        User user = userService.findByEmail(userDetails.getUsername()).orElseThrow();
        if (!announcement.getAuthor().getId().equals(user.getId())) {
            redirectAttributes.addFlashAttribute("error", "Немає доступу до редагування цього оголошення");
            return "redirect:/announcements/" + id;
        }
        model.addAttribute("announcement", announcement);
        model.addAttribute("categories", categoryRepository.findAll());
        return "announcements/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @RequestParam String title,
                         @RequestParam String description,
                         @RequestParam String city,
                         @RequestParam(required = false) Boolean acceptsApplications,
                         @RequestParam(required = false) List<Long> categoryIds,
                         @AuthenticationPrincipal UserDetails userDetails,
                         RedirectAttributes redirectAttributes) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Оберіть принаймні одну категорію");
            return "redirect:/announcements/" + id + "/edit";
        }
        User user = userService.findByEmail(userDetails.getUsername()).orElseThrow();
        try {
            announcementService.update(id, title, description, city, acceptsApplications, categoryIds, user);
            redirectAttributes.addFlashAttribute("success", "Оголошення оновлено та надіслано на модерацію");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cabinet";
    }

    @PostMapping("/{id}/complete")
    public String complete(@PathVariable Long id,
                           @AuthenticationPrincipal UserDetails userDetails,
                           RedirectAttributes redirectAttributes) {
        User user = userService.findByEmail(userDetails.getUsername()).orElseThrow();
        try {
            announcementService.complete(id, user);
            redirectAttributes.addFlashAttribute("success", "Оголошення закрито");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cabinet";
    }

    @PostMapping("/{id}/republish")
    public String republish(@PathVariable Long id,
                            @AuthenticationPrincipal UserDetails userDetails,
                            RedirectAttributes redirectAttributes) {
        User user = userService.findByEmail(userDetails.getUsername()).orElseThrow();
        try {
            announcementService.republish(id, user);
            redirectAttributes.addFlashAttribute("success", "Оголошення надіслано на повторну модерацію");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cabinet";
    }

    @PostMapping("/{id}/complaint")
    public String submitComplaint(@PathVariable Long id,
                                  @RequestParam String reason,
                                  @AuthenticationPrincipal UserDetails userDetails,
                                  RedirectAttributes redirectAttributes) {
        User user = userService.findByEmail(userDetails.getUsername()).orElseThrow();
        Announcement announcement = announcementService.findById(id).orElseThrow();
        try {
            complaintService.submit(announcement, user, reason);
            redirectAttributes.addFlashAttribute("success", "Скаргу надіслано");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/announcements/" + id;
    }
}
