package com.kosmanenko.vpo_humanitarian_aid_platform.service;

import com.kosmanenko.vpo_humanitarian_aid_platform.enums.ProviderType;
import com.kosmanenko.vpo_humanitarian_aid_platform.enums.UserRole;
import com.kosmanenko.vpo_humanitarian_aid_platform.model.User;
import com.kosmanenko.vpo_humanitarian_aid_platform.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("emailExists — повертає true якщо email вже зареєстровано")
    void emailExists_returnsTrue_whenEmailRegistered() {
        when(userRepository.existsByEmail("exist@test.com")).thenReturn(true);

        assertThat(authService.emailExists("exist@test.com")).isTrue();
    }

    @Test
    @DisplayName("emailExists — повертає false якщо email не зареєстровано")
    void emailExists_returnsFalse_whenEmailNotRegistered() {
        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);

        assertThat(authService.emailExists("new@test.com")).isFalse();
    }

    @Test
    @DisplayName("register — зберігає користувача з хешованим паролем та роллю VPO")
    void register_savesUser_withHashedPassword_andVpoRole() {
        when(passwordEncoder.encode("rawPass")).thenReturn("hashedPass");
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        when(userRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        authService.register("vpo@test.com", "rawPass", "Іван Франко", "+380501234567",
                "Київ", UserRole.VPO, null, null);

        User saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("vpo@test.com");
        assertThat(saved.getPasswordHash()).isEqualTo("hashedPass");
        assertThat(saved.getRole()).isEqualTo(UserRole.VPO);
        assertThat(saved.getProviderType()).isNull(); // VPO не має типу надавача
        assertThat(saved.getIsBlocked()).isFalse();
        assertThat(saved.getIsProfilePublic()).isFalse();
        assertThat(saved.getRatingCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("register — встановлює providerType тільки для ролі PROVIDER")
    void register_setsProviderType_onlyForProviderRole() {
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        when(userRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        authService.register("org@test.com", "pass", "Org Name", null,
                "Львів", UserRole.PROVIDER, ProviderType.ORGANIZATION, "БФ Надія");

        User saved = captor.getValue();
        assertThat(saved.getRole()).isEqualTo(UserRole.PROVIDER);
        assertThat(saved.getProviderType()).isEqualTo(ProviderType.ORGANIZATION);
        assertThat(saved.getOrgName()).isEqualTo("БФ Надія");
    }

    @Test
    @DisplayName("registerOAuth — зберігає користувача без пароля з oauthProvider та oauthId")
    void registerOAuth_savesUser_withOauthDetails() {
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        when(userRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        authService.registerOAuth("google@test.com", "Тарас Шевченко",
                "google", "google-id-123", UserRole.VPO, null);

        User saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("google@test.com");
        assertThat(saved.getOauthProvider()).isEqualTo("google");
        assertThat(saved.getOauthId()).isEqualTo("google-id-123");
        assertThat(saved.getPasswordHash()).isNull();
        assertThat(saved.getProviderType()).isNull();
        assertThat(saved.getIsBlocked()).isFalse();
    }

}