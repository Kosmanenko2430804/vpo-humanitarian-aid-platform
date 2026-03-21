package com.kosmanenko.vpo_humanitarian_aid_platform.service;

import com.kosmanenko.vpo_humanitarian_aid_platform.model.User;
import com.kosmanenko.vpo_humanitarian_aid_platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Сервіс для управління користувачами платформи.
 * <p>
 * Надає базові CRUD операції та функцію блокування/розблокування користувачів.
 * Оновлення рейтингу делеговано до {@link HelpApplicationService}.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class UserService {

    /** Репозиторій для роботи з таблицею користувачів у БД. */
    private final UserRepository userRepository;

    /**
     * Знаходить користувача за адресою електронної пошти.
     * Використовується Spring Security та контролерами для ідентифікації
     * поточного автентифікованого користувача.
     *
     * @param email електронна пошта
     * @return {@link Optional} із користувачем, або порожній якщо не знайдено
     */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Знаходить користувача за його числовим ідентифікатором.
     *
     * @param id ідентифікатор користувача
     * @return {@link Optional} із користувачем, або порожній якщо не знайдено
     */
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * Повертає список усіх користувачів платформи.
     * Призначено для адмін-панелі управління користувачами.
     *
     * @return список усіх користувачів
     */
    public List<User> findAll() {
        return userRepository.findAll();
    }

    /**
     * Зберігає (або оновлює) користувача в базі даних.
     * Використовується для оновлення профілю та налаштувань.
     *
     * @param user користувач для збереження
     * @return збережений користувач з актуальними даними
     */
    @Transactional
    public User save(User user) {
        return userRepository.save(user);
    }

    /**
     * Перемикає стан блокування користувача.
     * Якщо користувач активний — блокує (встановлює {@code isBlocked = true}).
     * Якщо вже заблокований — розблоковує ({@code isBlocked = false}).
     * Заблокований користувач не може увійти в систему (перевірка в Spring Security).
     *
     * @param userId ідентифікатор користувача; ігнорується якщо не існує
     */
    @Transactional
    public void toggleBlock(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            // Інвертуємо поточний стан блокування
            user.setIsBlocked(!Boolean.TRUE.equals(user.getIsBlocked()));
            userRepository.save(user);
        });
    }

}
