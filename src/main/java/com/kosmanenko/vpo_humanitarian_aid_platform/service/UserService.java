package com.kosmanenko.vpo_humanitarian_aid_platform.service;

import com.kosmanenko.vpo_humanitarian_aid_platform.enums.ProviderType;
import com.kosmanenko.vpo_humanitarian_aid_platform.enums.UserRole;
import com.kosmanenko.vpo_humanitarian_aid_platform.model.User;
import com.kosmanenko.vpo_humanitarian_aid_platform.repository.CategoryRepository;
import com.kosmanenko.vpo_humanitarian_aid_platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    // Отримує поточного користувача незалежно від типу входу (форма або Google OAuth2)
    public User findByAuthentication(Authentication auth) {
        String email;
        if (auth.getPrincipal() instanceof OAuth2User oAuth2User) {
            email = oAuth2User.getAttribute("email");
        } else {
            email = auth.getName();
        }
        return userRepository.findByEmail(email).orElseThrow();
    }

    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    @Transactional
    public User save(User user) {
        return userRepository.save(user);
    }

    @Transactional
    public void toggleBlock(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setIsBlocked(!Boolean.TRUE.equals(user.getIsBlocked()));
            userRepository.save(user);
        });
    }

    @Transactional
    public User register(String email, String password, String fullName,
                         String phone, String city, UserRole role,
                         ProviderType providerType, String orgName) {
        User user = User.builder()
            .email(email)
            .passwordHash(passwordEncoder.encode(password))
            .fullName(fullName)
            .phone(phone)
            .city(city)
            .role(role)
            .providerType(role == UserRole.PROVIDER ? providerType : null)
            .orgName(orgName)
            .isBlocked(false)
            .isProfilePublic(false)
            .build();
        return userRepository.save(user);
    }

    @Transactional
    public User registerOAuth(String email, String fullName,
                              UserRole role, ProviderType providerType,
                              String phone, String city) {
        User user = User.builder()
            .email(email)
            .fullName(fullName)
            .role(role)
            .providerType(role == UserRole.PROVIDER ? providerType : null)
            .phone(phone)
            .city(city)
            .isBlocked(false)
            .isProfilePublic(false)
            .build();
        return userRepository.save(user);
    }

    @Transactional
    public void updateProfile(User user, String fullName, String phone, String city,
                              String orgName, String orgDescription, Boolean isProfilePublic,
                              List<Long> providerCategoryIds) {
        user.setFullName(fullName);
        user.setPhone(phone);
        user.setCity(city);
        user.setOrgName(orgName);
        user.setOrgDescription(orgDescription);
        user.setIsProfilePublic(Boolean.TRUE.equals(isProfilePublic));
        if (providerCategoryIds != null) {
            user.setProviderCategories(
                providerCategoryIds.stream()
                    .map(id -> categoryRepository.findById(id).orElseThrow())
                    .collect(Collectors.toSet())
            );
        }
        userRepository.save(user);
    }

    public List<User> getPublicProviders(String city, ProviderType providerType, Long categoryId) {
        List<User> providers = userRepository.findByRoleAndIsProfilePublicTrueAndIsBlockedFalse(UserRole.PROVIDER);

        if (city != null && !city.isBlank()) {
            providers = providers.stream()
                    .filter(u -> city.equalsIgnoreCase(u.getCity()))
                    .collect(Collectors.toList());
        }
        if (providerType != null) {
            providers = providers.stream()
                    .filter(u -> providerType.equals(u.getProviderType()))
                    .collect(Collectors.toList());
        }
        if (categoryId != null) {
            providers = providers.stream()
                    .filter(u -> u.getProviderCategories().stream().anyMatch(c -> c.getId().equals(categoryId)))
                    .collect(Collectors.toList());
        }
        return providers;
    }
}
