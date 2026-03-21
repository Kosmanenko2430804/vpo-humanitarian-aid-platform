package com.kosmanenko.vpo_humanitarian_aid_platform.controller;

import com.kosmanenko.vpo_humanitarian_aid_platform.repository.CategoryRepository;
import com.kosmanenko.vpo_humanitarian_aid_platform.service.AnnouncementService;
import com.kosmanenko.vpo_humanitarian_aid_platform.service.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final AnnouncementService announcementService;
    private final CategoryRepository categoryRepository;
    private final CatalogService catalogService;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("latestOffers", announcementService.findTop6Offers());
        model.addAttribute("latestRequests", announcementService.findTop6Requests());
        model.addAttribute("latestProviders", catalogService.getPublicProviders(null, null, null).stream().limit(6).toList());
        model.addAttribute("categories", categoryRepository.findAll());
        return "home";
    }
}
