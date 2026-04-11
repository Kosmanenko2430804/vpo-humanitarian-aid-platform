package com.kosmanenko.vpo_humanitarian_aid_platform.service;

import com.kosmanenko.vpo_humanitarian_aid_platform.enums.ProviderType;
import com.kosmanenko.vpo_humanitarian_aid_platform.enums.UserRole;
import com.kosmanenko.vpo_humanitarian_aid_platform.model.Category;
import com.kosmanenko.vpo_humanitarian_aid_platform.model.User;
import com.kosmanenko.vpo_humanitarian_aid_platform.repository.CategoryRepository;
import com.kosmanenko.vpo_humanitarian_aid_platform.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Тести для сервісу користувачів
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    // toggleBlock — блокує активного користувача
    @Test
    void toggleBlock_blocksActiveUser() {
        User user = User.builder().id(1L).isBlocked(false).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        userService.toggleBlock(1L);

        assertThat(user.getIsBlocked()).isTrue();
        verify(userRepository).save(user);
    }

    // toggleBlock — розблоковує заблокованого користувача
    @Test
    void toggleBlock_unblocksBlockedUser() {
        User user = User.builder().id(1L).isBlocked(true).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        userService.toggleBlock(1L);

        assertThat(user.getIsBlocked()).isFalse();
    }

    // toggleBlock — нічого не робить якщо користувача не знайдено
    @Test
    void toggleBlock_doesNothing_whenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        userService.toggleBlock(99L);

        verify(userRepository, never()).save(any());
    }

    // updateProfile — оновлює поля та зберігає
    @Test
    void updateProfile_updatesFieldsAndSaves() {
        User user = User.builder().id(1L).build();
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        userService.updateProfile(user, "Нове Ім'я", "+380991234567", "Одеса",
                null, null, true, null);

        assertThat(user.getFullName()).isEqualTo("Нове Ім'я");
        assertThat(user.getPhone()).isEqualTo("+380991234567");
        assertThat(user.getCity()).isEqualTo("Одеса");
        assertThat(user.getIsProfilePublic()).isTrue();
        verify(userRepository).save(user);
    }

    // updateProfile — резолвить категорії за ID якщо передані
    @Test
    void updateProfile_resolvesCategories_whenProvided() {
        User user = User.builder().id(1L).build();
        Category cat = Category.builder().id(1L).name("Їжа").build();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(cat));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        userService.updateProfile(user, "Ім'я", "тел", "Місто",
                null, null, false, List.of(1L));

        assertThat(user.getProviderCategories()).containsExactly(cat);
    }

    // emailExists — повертає true якщо email вже зареєстровано
    @Test
    void emailExists_returnsTrue_whenEmailRegistered() {
        when(userRepository.existsByEmail("exist@test.com")).thenReturn(true);

        assertThat(userService.emailExists("exist@test.com")).isTrue();
    }

    // emailExists — повертає false якщо email не зареєстровано
    @Test
    void emailExists_returnsFalse_whenEmailNotRegistered() {
        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);

        assertThat(userService.emailExists("new@test.com")).isFalse();
    }

    // register — зберігає користувача з хешованим паролем та роллю VPO
    @Test
    void register_savesUser_withHashedPassword_andVpoRole() {
        when(passwordEncoder.encode("rawPass")).thenReturn("hashedPass");
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        when(userRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        userService.register("vpo@test.com", "rawPass", "Іван Франко", "+380501234567",
                "Київ", UserRole.VPO, null, null);

        User saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("vpo@test.com");
        assertThat(saved.getPasswordHash()).isEqualTo("hashedPass");
        assertThat(saved.getRole()).isEqualTo(UserRole.VPO);
        assertThat(saved.getProviderType()).isNull();
        assertThat(saved.getIsBlocked()).isFalse();
    }

    // register — встановлює providerType тільки для ролі PROVIDER
    @Test
    void register_setsProviderType_onlyForProviderRole() {
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        when(userRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        userService.register("org@test.com", "pass", "Org Name", null,
                "Львів", UserRole.PROVIDER, ProviderType.ORGANIZATION, "БФ Надія");

        User saved = captor.getValue();
        assertThat(saved.getRole()).isEqualTo(UserRole.PROVIDER);
        assertThat(saved.getProviderType()).isEqualTo(ProviderType.ORGANIZATION);
        assertThat(saved.getOrgName()).isEqualTo("БФ Надія");
    }
}
