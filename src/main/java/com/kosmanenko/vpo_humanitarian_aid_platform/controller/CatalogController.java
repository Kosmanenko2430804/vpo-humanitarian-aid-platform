package com.kosmanenko.vpo_humanitarian_aid_platform.controller;

import com.kosmanenko.vpo_humanitarian_aid_platform.enums.AnnouncementStatus;
import com.kosmanenko.vpo_humanitarian_aid_platform.enums.AnnouncementType;
import com.kosmanenko.vpo_humanitarian_aid_platform.enums.ProviderType;
import com.kosmanenko.vpo_humanitarian_aid_platform.model.User;
import com.kosmanenko.vpo_humanitarian_aid_platform.repository.CategoryRepository;
import com.kosmanenko.vpo_humanitarian_aid_platform.service.AnnouncementService;
import com.kosmanenko.vpo_humanitarian_aid_platform.service.CatalogService;
import com.kosmanenko.vpo_humanitarian_aid_platform.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/catalog")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;
    private final UserService userService;
    private final AnnouncementService announcementService;
    private final CategoryRepository categoryRepository;

    @GetMapping("/providers")
    public String providers(@RequestParam(required = false) String city,
                            @RequestParam(required = false) String providerType,
                            @RequestParam(required = false) Long categoryId,
                            Model model) {
        ProviderType pType = null;
        if (providerType != null && !providerType.isBlank()) {
            try { pType = ProviderType.valueOf(providerType); } catch (Exception ignored) {}
        }
        model.addAttribute("providers", catalogService.getPublicProviders(city, pType, categoryId));
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("currentCity", city);
        model.addAttribute("currentProviderType", providerType);
        model.addAttribute("currentCategoryId", categoryId);
        return "catalog/providers";
    }

    @GetMapping("/providers/{id}")
    public String providerProfile(@PathVariable Long id, Model model) {
        User provider = userService.findById(id).orElseThrow();
        model.addAttribute("provider", provider);
        model.addAttribute("announcements",
            announcementService.findByAuthor(provider).stream()
                .filter(a -> a.getStatus() == AnnouncementStatus.PUBLISHED)
                .toList());
        return "catalog/provider-profile";
    }
}
