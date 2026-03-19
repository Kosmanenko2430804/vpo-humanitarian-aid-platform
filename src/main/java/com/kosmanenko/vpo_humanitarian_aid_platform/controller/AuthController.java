package com.kosmanenko.vpo_humanitarian_aid_platform.controller;

import com.kosmanenko.vpo_humanitarian_aid_platform.dto.RegisterForm;
import com.kosmanenko.vpo_humanitarian_aid_platform.enums.ProviderType;
import com.kosmanenko.vpo_humanitarian_aid_platform.enums.UserRole;
import com.kosmanenko.vpo_humanitarian_aid_platform.service.AuthService;
import com.kosmanenko.vpo_humanitarian_aid_platform.service.CustomUserDetailsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CustomUserDetailsService userDetailsService;

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String error,
                            @RequestParam(required = false) String blocked,
                            Model model) {
        if (error != null) model.addAttribute("error", "Невірний email або пароль");
        if (blocked != null) model.addAttribute("blocked", "Ваш акаунт заблоковано");
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerForm", new RegisterForm());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(
            @ModelAttribute("registerForm") @Valid RegisterForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        if (authService.emailExists(form.getEmail())) {
            model.addAttribute("error", "Користувач з таким email вже існує");
            return "auth/register";
        }

        try {
            UserRole userRole = UserRole.valueOf(form.getRole());
            ProviderType pType = (form.getProviderType() != null && !form.getProviderType().isBlank())
                ? ProviderType.valueOf(form.getProviderType()) : null;
            authService.register(form.getEmail(), form.getPassword(), form.getFullName(),
                form.getPhone(), form.getCity(), userRole, pType, form.getOrgName());
            redirectAttributes.addFlashAttribute("success", "Реєстрацію успішно завершено. Увійдіть у систему.");
            return "redirect:/auth/login";
        } catch (Exception e) {
            model.addAttribute("error", "Помилка реєстрації: " + e.getMessage());
            return "auth/register";
        }
    }

    @GetMapping("/oauth2/complete")
    public String completeOAuth2(HttpSession session, Model model) {
        if (session.getAttribute("oauth_email") == null) {
            return "redirect:/auth/login";
        }
        model.addAttribute("email", session.getAttribute("oauth_email"));
        model.addAttribute("name", session.getAttribute("oauth_name"));
        return "auth/oauth2-complete";
    }

    @PostMapping("/oauth2/complete")
    public String saveOAuth2User(
            @RequestParam String role,
            @RequestParam(required = false) String providerType,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String city,
            HttpSession session,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        String email = (String) session.getAttribute("oauth_email");
        String name = (String) session.getAttribute("oauth_name");
        String oauthId = (String) session.getAttribute("oauth_id");
        String oauthProvider = (String) session.getAttribute("oauth_provider");

        if (email == null) {
            return "redirect:/auth/login";
        }

        try {
            UserRole userRole = UserRole.valueOf(role);
            ProviderType pType = (providerType != null && !providerType.isBlank())
                ? ProviderType.valueOf(providerType) : null;
            authService.registerOAuth(email, name, oauthProvider, oauthId, userRole, pType, phone, city);
            session.removeAttribute("oauth_email");
            session.removeAttribute("oauth_name");
            session.removeAttribute("oauth_id");
            session.removeAttribute("oauth_provider");

            // Re-authenticate as UserDetails so @AuthenticationPrincipal works in controllers
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);
            request.getSession().setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                SecurityContextHolder.getContext());

            redirectAttributes.addFlashAttribute("success", "Реєстрацію завершено!");
            return "redirect:/";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Помилка: " + e.getMessage());
            return "redirect:/auth/oauth2/complete";
        }
    }
}
