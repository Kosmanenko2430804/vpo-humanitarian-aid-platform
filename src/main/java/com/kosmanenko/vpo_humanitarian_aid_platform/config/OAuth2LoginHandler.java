package com.kosmanenko.vpo_humanitarian_aid_platform.config;

import com.kosmanenko.vpo_humanitarian_aid_platform.model.User;
import com.kosmanenko.vpo_humanitarian_aid_platform.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OAuth2LoginHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        HttpSession session = request.getSession();
        Optional<User> existingUser = userRepository.findByEmail(email);

        if (existingUser.isPresent()) {
            // Перевіряємо чи не заблокований
            if (Boolean.TRUE.equals(existingUser.get().getIsBlocked())) {
                session.invalidate();
                response.sendRedirect("/auth/login?blocked=true");
                return;
            }
            response.sendRedirect("/");
        } else {
            // Новий користувач — потрібно обрати роль
            session.setAttribute("oauth_email", email);
            session.setAttribute("oauth_name", name);
            response.sendRedirect("/auth/oauth2/complete");
        }
    }
}
