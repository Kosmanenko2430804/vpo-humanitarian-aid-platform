package com.kosmanenko.vpo_humanitarian_aid_platform.controller;

import com.kosmanenko.vpo_humanitarian_aid_platform.dto.AnnouncementForm;
import com.kosmanenko.vpo_humanitarian_aid_platform.enums.AnnouncementStatus;
import com.kosmanenko.vpo_humanitarian_aid_platform.enums.AnnouncementType;
import com.kosmanenko.vpo_humanitarian_aid_platform.enums.ProviderType;
import com.kosmanenko.vpo_humanitarian_aid_platform.enums.UserRole;
import com.kosmanenko.vpo_humanitarian_aid_platform.model.Announcement;
import com.kosmanenko.vpo_humanitarian_aid_platform.model.User;
import com.kosmanenko.vpo_humanitarian_aid_platform.service.ApplicationService;
import com.kosmanenko.vpo_humanitarian_aid_platform.service.AnnouncementService;
import com.kosmanenko.vpo_humanitarian_aid_platform.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;
    private final ApplicationService helpApplicationService;
    private final UserService userService;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("latestOffers", announcementService.findTop6Offers());
        model.addAttribute("latestRequests", announcementService.findTop6Requests());
        model.addAttribute("latestProviders", userService.getPublicProviders(null, null, null).stream().limit(6).toList());
        model.addAttribute("categories", announcementService.findAllCategories());
        return "home";
    }

    @GetMapping("/catalog/providers")
    public String providers(@RequestParam(required = false) String city,
                            @RequestParam(required = false) String providerType,
                            @RequestParam(required = false) Long categoryId,
                            Model model) {
        ProviderType pType = null;
        if (providerType != null && !providerType.isBlank()) {
            try { pType = ProviderType.valueOf(providerType); } catch (Exception ignored) {}
        }
        model.addAttribute("providers", userService.getPublicProviders(city, pType, categoryId));
        model.addAttribute("categories", announcementService.findAllCategories());
        model.addAttribute("currentCity", city);
        model.addAttribute("currentProviderType", providerType);
        model.addAttribute("currentCategoryId", categoryId);
        return "announcements/providers";
    }

    @GetMapping("/catalog/providers/{id}")
    public String providerProfile(@PathVariable Long id, Model model) {
        User provider = userService.findById(id).orElseThrow();
        model.addAttribute("provider", provider);
        model.addAttribute("announcements",
            announcementService.findByAuthor(provider).stream()
                .filter(a -> a.getStatus() == AnnouncementStatus.PUBLISHED)
                .toList());
        return "announcements/provider-profile";
    }

    @GetMapping("/announcements")
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
        model.addAttribute("categories", announcementService.findAllCategories());
        model.addAttribute("currentType", type);
        model.addAttribute("currentCity", city);
        model.addAttribute("currentCategoryId", categoryId);
        model.addAttribute("currentKeyword", keyword);
        return "announcements/list";
    }

    @GetMapping("/announcements/{id}")
    public String detail(@PathVariable Long id, Authentication auth, Model model) {
        Announcement announcement = announcementService.findById(id)
            .orElseThrow(() -> new RuntimeException("Не знайдено"));
        model.addAttribute("announcement", announcement);

        if (auth != null && auth.isAuthenticated()) {
            User user = userService.findByAuthentication(auth);
            model.addAttribute("currentUser", user);
            if (user.getRole() == UserRole.VPO) {
                model.addAttribute("alreadyApplied",
                    helpApplicationService.alreadyApplied(announcement, user));
            }
        }
        return "announcements/detail";
    }

    @GetMapping("/announcements/new")
    public String newForm(Authentication auth, Model model) {
        model.addAttribute("announcementForm", new AnnouncementForm());
        model.addAttribute("categories", announcementService.findAllCategories());
        model.addAttribute("currentUser", userService.findByAuthentication(auth));
        return "announcements/form";
    }

    @PostMapping("/announcements/new")
    public String create(@ModelAttribute("announcementForm") @Valid AnnouncementForm form,
                         BindingResult bindingResult,
                         Authentication auth,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", announcementService.findAllCategories());
            model.addAttribute("currentUser", userService.findByAuthentication(auth));
            return "announcements/form";
        }
        User user = userService.findByAuthentication(auth);
        AnnouncementType type = user.getRole() == UserRole.VPO ? AnnouncementType.REQUEST : AnnouncementType.OFFER;
        try {
            announcementService.create(form.getTitle(), form.getDescription(), form.getCity(), type,
                    form.getAcceptsApplications(), form.getCategoryIds(), user, form.getDonationUrl());
            redirectAttributes.addFlashAttribute("success", "Оголошення надіслано на модерацію");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cabinet/announcements";
    }

    @GetMapping("/announcements/{id}/edit")
    public String editForm(@PathVariable Long id, Authentication auth, Model model,
                           RedirectAttributes redirectAttributes) {
        Announcement announcement = announcementService.findById(id).orElseThrow();
        User user = userService.findByAuthentication(auth);
        if (!announcement.getAuthor().getId().equals(user.getId())) {
            redirectAttributes.addFlashAttribute("error", "Немає доступу до редагування цього оголошення");
            return "redirect:/announcements/" + id;
        }
        AnnouncementForm form = new AnnouncementForm();
        form.setTitle(announcement.getTitle());
        form.setDescription(announcement.getDescription());
        form.setCity(announcement.getCity());
        form.setDonationUrl(announcement.getDonationUrl());
        form.setAcceptsApplications(announcement.getAcceptsApplications());
        form.setCategoryIds(announcement.getCategories().stream().map(c -> c.getId()).toList());
        model.addAttribute("announcementForm", form);
        model.addAttribute("announcement", announcement);
        model.addAttribute("categories", announcementService.findAllCategories());
        return "announcements/form";
    }

    @PostMapping("/announcements/{id}/edit")
    public String update(@PathVariable Long id,
                         @ModelAttribute("announcementForm") @Valid AnnouncementForm form,
                         BindingResult bindingResult,
                         Authentication auth,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("announcement", announcementService.findById(id).orElseThrow());
            model.addAttribute("categories", announcementService.findAllCategories());
            return "announcements/form";
        }
        User user = userService.findByAuthentication(auth);
        try {
            announcementService.update(id, form.getTitle(), form.getDescription(), form.getCity(),
                    form.getAcceptsApplications(), form.getCategoryIds(), user, form.getDonationUrl());
            redirectAttributes.addFlashAttribute("success", "Оголошення оновлено та надіслано на модерацію");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cabinet/announcements";
    }

    @PostMapping("/announcements/{id}/complete")
    public String complete(@PathVariable Long id, Authentication auth,
                           RedirectAttributes redirectAttributes) {
        User user = userService.findByAuthentication(auth);
        try {
            announcementService.complete(id, user);
            redirectAttributes.addFlashAttribute("success", "Оголошення закрито");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cabinet/announcements";
    }
}
