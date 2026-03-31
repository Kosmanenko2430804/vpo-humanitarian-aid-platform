package com.kosmanenko.vpo_humanitarian_aid_platform.service;

import com.kosmanenko.vpo_humanitarian_aid_platform.enums.ProviderType;
import com.kosmanenko.vpo_humanitarian_aid_platform.enums.UserRole;
import com.kosmanenko.vpo_humanitarian_aid_platform.model.User;
import com.kosmanenko.vpo_humanitarian_aid_platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
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
            .ratingCount(0)
            .build();
        return userRepository.save(user);
    }

    @Transactional
    public User registerOAuth(String email, String fullName,
                              String oauthProvider, String oauthId,
                              UserRole role, ProviderType providerType,
                              String phone, String city) {
        User user = User.builder()
            .email(email)
            .fullName(fullName)
            .oauthProvider(oauthProvider)
            .oauthId(oauthId)
            .role(role)
            .providerType(role == UserRole.PROVIDER ? providerType : null)
            .phone(phone)
            .city(city)
            .isBlocked(false)
            .isProfilePublic(false)
            .ratingCount(0)
            .build();
        return userRepository.save(user);
    }
}
