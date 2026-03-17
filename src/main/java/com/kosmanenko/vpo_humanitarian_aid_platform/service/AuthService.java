package com.kosmanenko.vpo_humanitarian_aid_platform.service;

import com.kosmanenko.vpo_humanitarian_aid_platform.enums.ProviderType;
import com.kosmanenko.vpo_humanitarian_aid_platform.enums.UserRole;
import com.kosmanenko.vpo_humanitarian_aid_platform.model.User;
import com.kosmanenko.vpo_humanitarian_aid_platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Сервіс автентифікації та реєстрації користувачів.
 * Підтримує два способи реєстрації:
 * 1 Форма реєстрації з email та паролем
 * 2 OAuth2 (Google)
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    /** Репозиторій для збереження та пошуку користувачів. */
    private final UserRepository userRepository;

    /** Кодувальник паролів BCrypt для безпечного зберігання. */
    private final PasswordEncoder passwordEncoder;

    /**
     * Перевіряє, чи існує вже користувач із зазначеною електронною поштою.
     * Використовується для валідації форми реєстрації перед збереженням.
     *
     * @param email електронна пошта для перевірки
     * @return {@code true} якщо email вже зареєстровано
     */
    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * Реєструє нового користувача через форму з email та паролем.
     * <p>
     * Пароль хешується алгоритмом BCrypt перед збереженням.
     * Тип надавача ({@code providerType}) встановлюється лише для ролі {@code PROVIDER}.
     * Початковий стан: не заблокований, профіль непублічний, рейтинг 0.
     * </p>
     *
     * @param email        електронна пошта
     * @param password     пароль у відкритому вигляді
     * @param fullName     повне ім'я користувача
     * @param phone        номер телефону
     * @param city         місто проживання
     * @param role         роль у системі (VPO, PROVIDER, ADMIN)
     * @param providerType тип надавача (VOLUNTEER або ORGANIZATION)
     * @param orgName      назва організації; актуально для {@code providerType == ORGANIZATION}
     * @return збережений новий користувач
     */
    @Transactional
    public User register(String email, String password, String fullName,
                         String phone, String city, UserRole role,
                         ProviderType providerType, String orgName) {
        User user = User.builder()
            .email(email)
            .passwordHash(passwordEncoder.encode(password)) // BCrypt хешування пароля
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

    /**
     * Реєструє нового користувача через Google OAuth2.
     * пароль не встановлюється - зберігається {@code oauthProvider} ("google") та {@code oauthId}
     * (унікальний ідентифікатор Google-акаунту).
     *
     * @param email         електронна пошта з Google-акаунту
     * @param fullName      ім'я з Google-профілю
     * @param oauthProvider назва постачальника OAuth2 ( "google")
     * @param oauthId       унікальний ідентифікатор в системі постачальника
     * @param role          роль у системі
     * @param providerType  тип надавача; {@code null} для ролі VPO
     * @return збережений новий користувач
     */
    @Transactional
    public User registerOAuth(String email, String fullName,
                              String oauthProvider, String oauthId,
                              UserRole role, ProviderType providerType) {
        User user = User.builder()
            .email(email)
            .fullName(fullName)
            .oauthProvider(oauthProvider) // "google"
            .oauthId(oauthId)
            .role(role)
            .providerType(role == UserRole.PROVIDER ? providerType : null)
            .isBlocked(false)
            .isProfilePublic(false)
            .ratingCount(0)
            .build();
        return userRepository.save(user);
    }
}
