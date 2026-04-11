package com.kosmanenko.vpo_humanitarian_aid_platform.controller;

import com.kosmanenko.vpo_humanitarian_aid_platform.dto.ProfileForm;
import com.kosmanenko.vpo_humanitarian_aid_platform.enums.ProviderType;
import com.kosmanenko.vpo_humanitarian_aid_platform.enums.UserRole;
import com.kosmanenko.vpo_humanitarian_aid_platform.model.Announcement;
import com.kosmanenko.vpo_humanitarian_aid_platform.model.User;
import com.kosmanenko.vpo_humanitarian_aid_platform.service.AnnouncementService;
import com.kosmanenko.vpo_humanitarian_aid_platform.service.ApplicationService;
import com.kosmanenko.vpo_humanitarian_aid_platform.service.NotificationService;
import com.kosmanenko.vpo_humanitarian_aid_platform.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;
    private final AnnouncementService announcementService;
    private final ApplicationService helpApplicationService;
    private final NotificationService notificationService;

    @GetMapping("/cabinet")
    public String cabinet() {
        return "redirect:/cabinet/announcements";
    }

    @GetMapping("/cabinet/profile")
    public String editProfile(Authentication auth, Model model) {
        User user = userService.findByAuthentication(auth);
        ProfileForm form = new ProfileForm();
        form.setFullName(user.getFullName());
        form.setPhone(user.getPhone());
        form.setCity(user.getCity());
        form.setOrgName(user.getOrgName());
        form.setOrgDescription(user.getOrgDescription());
        form.setIsProfilePublic(user.getIsProfilePublic());
        model.addAttribute("profileForm", form);
        model.addAttribute("user", user);
        model.addAttribute("allCategories", announcementService.findAllCategories());
        model.addAttribute("unreadCount", notificationService.countUnread(user));
        return "cabinet/profile";
    }

    @PostMapping("/cabinet/profile")
    public String saveProfile(@ModelAttribute("profileForm") @Valid ProfileForm form,
                              BindingResult bindingResult,
                              Authentication auth,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        User user = userService.findByAuthentication(auth);

        if (user.getProviderType() == ProviderType.ORGANIZATION) {
            if (form.getOrgName() == null || form.getOrgName().isBlank()) {
                bindingResult.rejectValue("orgName", "orgName.required", "Назва організації є обов'язковою");
            }
            if (form.getOrgDescription() == null || form.getOrgDescription().isBlank()) {
                bindingResult.rejectValue("orgDescription", "orgDescription.required", "Опис організації є обов'язковим");
            }
        }
        if (user.getRole() == UserRole.PROVIDER) {
            if (form.getProviderCategoryIds() == null || form.getProviderCategoryIds().isEmpty()) {
                bindingResult.rejectValue("providerCategoryIds", "providerCategoryIds.required", "Оберіть принаймні одну категорію");
            }
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("user", user);
            model.addAttribute("allCategories", announcementService.findAllCategories());
            model.addAttribute("unreadCount", notificationService.countUnread(user));
            return "cabinet/profile";
        }
        userService.updateProfile(user, form.getFullName(), form.getPhone(), form.getCity(),
                form.getOrgName(), form.getOrgDescription(), form.getIsProfilePublic(), form.getProviderCategoryIds());
        redirectAttributes.addFlashAttribute("success", "Профіль оновлено");
        return "redirect:/cabinet/profile";
    }

    @GetMapping("/cabinet/announcements")
    public String myAnnouncements(Authentication auth, Model model) {
        User user = userService.findByAuthentication(auth);
        model.addAttribute("user", user);
        model.addAttribute("announcements", announcementService.findByAuthor(user));
        model.addAttribute("unreadCount", notificationService.countUnread(user));
        return "cabinet/my-announcements";
    }

    @GetMapping("/cabinet/applications")
    public String myApplications(Authentication auth, Model model) {
        User user = userService.findByAuthentication(auth);
        model.addAttribute("user", user);
        model.addAttribute("unreadCount", notificationService.countUnread(user));
        model.addAttribute("myApplications", helpApplicationService.findByApplicant(user));

        List<com.kosmanenko.vpo_humanitarian_aid_platform.model.HelpApplication> receivedApps =
            announcementService.findByAuthor(user).stream()
                .flatMap(a -> helpApplicationService.findByAnnouncement(a).stream())
                .toList();
        model.addAttribute("receivedApplications", receivedApps);

        return "cabinet/my-applications";
    }

    @GetMapping("/cabinet/notifications")
    public String notifications(Authentication auth, Model model) {
        User user = userService.findByAuthentication(auth);
        notificationService.markAllRead(user);
        model.addAttribute("user", user);
        model.addAttribute("notifications", notificationService.getNotificationsForUser(user));
        model.addAttribute("unreadCount", 0L);
        return "cabinet/notifications";
    }

    @PostMapping("/applications/apply/{announcementId}")
    public String apply(@PathVariable Long announcementId,
                        @RequestParam String message,
                        Authentication auth,
                        RedirectAttributes redirectAttributes) {
        User user = userService.findByAuthentication(auth);
        Announcement announcement = announcementService.findById(announcementId).orElseThrow();
        try {
            helpApplicationService.apply(announcement, user, message);
            redirectAttributes.addFlashAttribute("success", "Заявку надіслано");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/announcements/" + announcementId;
    }

    @PostMapping("/applications/{id}/accept")
    public String accept(@PathVariable Long id,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime pickupDate,
                         @RequestParam(required = false) String pickupLocation,
                         @RequestParam(required = false) String providerPhone,
                         Authentication auth,
                         RedirectAttributes redirectAttributes) {
        if (pickupDate == null || pickupLocation == null || pickupLocation.isBlank()
                || providerPhone == null || providerPhone.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Вкажіть дату, місце та телефон для видачі допомоги");
            return "redirect:/cabinet/applications";
        }
        User user = userService.findByAuthentication(auth);
        try {
            helpApplicationService.accept(id, pickupDate, pickupLocation, providerPhone, user);
            redirectAttributes.addFlashAttribute("success", "Заявку прийнято");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cabinet/applications";
    }

    @PostMapping("/applications/{id}/reject")
    public String reject(@PathVariable Long id,
                         @RequestParam String reason,
                         Authentication auth,
                         RedirectAttributes redirectAttributes) {
        User user = userService.findByAuthentication(auth);
        try {
            helpApplicationService.reject(id, reason, user);
            redirectAttributes.addFlashAttribute("success", "Заявку відхилено");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cabinet/applications";
    }

    @PostMapping("/applications/{id}/complete")
    public String complete(@PathVariable Long id, Authentication auth,
                           RedirectAttributes redirectAttributes) {
        User user = userService.findByAuthentication(auth);
        try {
            helpApplicationService.complete(id, user);
            redirectAttributes.addFlashAttribute("success", "Заявку закрито");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cabinet/applications";
    }
}
