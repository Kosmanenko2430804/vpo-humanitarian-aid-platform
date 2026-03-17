package com.kosmanenko.vpo_humanitarian_aid_platform.service;

import com.kosmanenko.vpo_humanitarian_aid_platform.enums.UserRole;
import com.kosmanenko.vpo_humanitarian_aid_platform.model.User;
import com.kosmanenko.vpo_humanitarian_aid_platform.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("loadUserByUsername — повертає UserDetails для активного користувача")
    void loadUserByUsername_returnsUserDetails_whenActive() {
        User user = User.builder()
            .id(1L).email("user@test.com")
            .passwordHash("hashedPassword")
            .role(UserRole.VPO)
            .isBlocked(false)
            .build();
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));

        UserDetails details = customUserDetailsService.loadUserByUsername("user@test.com");

        assertThat(details.getUsername()).isEqualTo("user@test.com");
        assertThat(details.getPassword()).isEqualTo("hashedPassword");
        assertThat(details.isEnabled()).isTrue();
        assertThat(details.getAuthorities()).anyMatch(a -> a.getAuthority().equals("ROLE_VPO"));
    }

    @Test
    @DisplayName("loadUserByUsername — вимкнений для заблокованого користувача")
    void loadUserByUsername_returnsDisabledUser_whenBlocked() {
        User user = User.builder()
            .id(1L).email("blocked@test.com")
            .passwordHash("hash")
            .role(UserRole.VPO)
            .isBlocked(true)
            .build();
        when(userRepository.findByEmail("blocked@test.com")).thenReturn(Optional.of(user));

        UserDetails details = customUserDetailsService.loadUserByUsername("blocked@test.com");

        assertThat(details.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("loadUserByUsername — кидає UsernameNotFoundException якщо користувача не знайдено")
    void loadUserByUsername_throwsException_whenUserNotFound() {
        when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
            customUserDetailsService.loadUserByUsername("unknown@test.com"))
            .isInstanceOf(UsernameNotFoundException.class)
            .hasMessageContaining("unknown@test.com");
    }

}
