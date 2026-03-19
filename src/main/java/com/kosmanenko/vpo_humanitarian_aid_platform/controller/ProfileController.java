package com.kosmanenko.vpo_humanitarian_aid_platform.controller;

import com.kosmanenko.vpo_humanitarian_aid_platform.model.User;
import com.kosmanenko.vpo_humanitarian_aid_platform.repository.CategoryRepository;
import com.kosmanenko.vpo_humanitarian_aid_platform.service.AnnouncementService;
import com.kosmanenko.vpo_humanitarian_aid_platform.service.HelpApplicationService;
import com.kosmanenko.vpo_humanitarian_aid_platform.service.NotificationService;
import com.kosmanenko.vpo_humanitarian_aid_platform.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.springframework.http.MediaType;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/cabinet")
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;
    private final AnnouncementService announcementService;
    private final HelpApplicationService helpApplicationService;
    private final NotificationService notificationService;
    private final CategoryRepository categoryRepository;

    @GetMapping
    public String cabinet() {
        return "redirect:/cabinet/announcements";
    }

    @GetMapping("/profile")
    public String editProfile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.findByEmail(userDetails.getUsername()).orElseThrow();
        model.addAttribute("user", user);
        model.addAttribute("allCategories", categoryRepository.findAll());
        model.addAttribute("unreadCount", notificationService.countUnread(user));
        return "cabinet/profile";
    }

    @PostMapping("/profile")
    public String saveProfile(@RequestParam String fullName,
                              @RequestParam(required = false) String phone,
                              @RequestParam(required = false) String city,
                              @RequestParam(required = false) String orgName,
                              @RequestParam(required = false) String orgDescription,
                              @RequestParam(required = false) String orgLogoUrl,
                              @RequestParam(required = false) Boolean isProfilePublic,
                              @RequestParam(required = false) List<Long> providerCategoryIds,
                              @AuthenticationPrincipal UserDetails userDetails,
                              RedirectAttributes redirectAttributes) {
        User user = userService.findByEmail(userDetails.getUsername()).orElseThrow();
        user.setFullName(fullName);
        user.setPhone(phone);
        user.setCity(city);
        user.setOrgName(orgName);
        user.setOrgDescription(orgDescription);
        user.setOrgLogoUrl(orgLogoUrl);
        user.setIsProfilePublic(Boolean.TRUE.equals(isProfilePublic));

        if (providerCategoryIds != null) {
            user.setProviderCategories(
                providerCategoryIds.stream()
                    .map(id -> categoryRepository.findById(id).orElseThrow())
                    .collect(Collectors.toSet())
            );
        }

        userService.save(user);
        redirectAttributes.addFlashAttribute("success", "Профіль оновлено");
        return "redirect:/cabinet/profile";
    }

    @GetMapping("/announcements")
    public String myAnnouncements(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.findByEmail(userDetails.getUsername()).orElseThrow();
        model.addAttribute("user", user);
        model.addAttribute("announcements", announcementService.findByAuthor(user));
        model.addAttribute("unreadCount", notificationService.countUnread(user));
        return "cabinet/my-announcements";
    }

    @GetMapping("/applications")
    public String myApplications(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.findByEmail(userDetails.getUsername()).orElseThrow();
        model.addAttribute("user", user);
        model.addAttribute("unreadCount", notificationService.countUnread(user));

        // For VPO: show their submitted applications
        model.addAttribute("myApplications", helpApplicationService.findByApplicant(user));

        // For PROVIDER: show applications on their announcements
        List<com.kosmanenko.vpo_humanitarian_aid_platform.model.HelpApplication> receivedApps =
            announcementService.findByAuthor(user).stream()
                .flatMap(a -> helpApplicationService.findByAnnouncement(a).stream())
                .toList();
        model.addAttribute("receivedApplications", receivedApps);

        return "cabinet/my-applications";
    }

    @GetMapping("/notifications")
    public String notifications(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.findByEmail(userDetails.getUsername()).orElseThrow();
        notificationService.markAllRead(user);
        model.addAttribute("user", user);
        model.addAttribute("notifications", notificationService.getNotificationsForUser(user));
        model.addAttribute("unreadCount", 0L);
        return "cabinet/notifications";
    }

    @GetMapping(value = "/notifications/count", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String notificationsCount(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return "<span id=\"notif-badge\"></span>";
        }
        return userService.findByEmail(userDetails.getUsername()).map(user -> {
            long count = notificationService.countUnread(user);
            if (count > 0) {
                return "<span id=\"notif-badge\" class=\"badge bg-warning text-dark ms-1\" " +
                       "hx-get=\"/cabinet/notifications/count\" hx-trigger=\"every 60s\" " +
                       "hx-target=\"#notif-badge\" hx-swap=\"outerHTML\">" + count + "</span>";
            }
            return "<span id=\"notif-badge\" " +
                   "hx-get=\"/cabinet/notifications/count\" hx-trigger=\"every 60s\" " +
                   "hx-target=\"#notif-badge\" hx-swap=\"outerHTML\"></span>";
        }).orElse("<span id=\"notif-badge\"></span>");
    }
}
