package com.kosmanenko.vpo_humanitarian_aid_platform.service;

import com.kosmanenko.vpo_humanitarian_aid_platform.model.User;
import com.kosmanenko.vpo_humanitarian_aid_platform.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("findByEmail — повертає користувача якщо існує")
    void findByEmail_returnsUser_whenExists() {
        User user = User.builder().id(1L).email("user@test.com").build();
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));

        Optional<User> result = userService.findByEmail("user@test.com");

        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("user@test.com");
    }

    @Test
    @DisplayName("findByEmail — повертає порожній Optional якщо не існує")
    void findByEmail_returnsEmpty_whenNotExists() {
        when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        assertThat(userService.findByEmail("unknown@test.com")).isEmpty();
    }

    @Test
    @DisplayName("findById — делегує до репозиторію")
    void findById_delegatesToRepository() {
        User user = User.builder().id(5L).build();
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));

        Optional<User> result = userService.findById(5L);

        assertThat(result).isPresent();
    }

    @Test
    @DisplayName("toggleBlock — блокує активного користувача")
    void toggleBlock_blocksActiveUser() {
        User user = User.builder().id(1L).isBlocked(false).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        userService.toggleBlock(1L);

        assertThat(user.getIsBlocked()).isTrue();
        verify(userRepository).save(user);
    }
}
