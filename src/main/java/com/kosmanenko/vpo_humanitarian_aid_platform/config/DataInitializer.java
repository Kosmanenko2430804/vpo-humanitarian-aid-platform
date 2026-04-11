package com.kosmanenko.vpo_humanitarian_aid_platform.config;

import com.kosmanenko.vpo_humanitarian_aid_platform.enums.ProviderType;
import com.kosmanenko.vpo_humanitarian_aid_platform.enums.UserRole;
import com.kosmanenko.vpo_humanitarian_aid_platform.model.Category;
import com.kosmanenko.vpo_humanitarian_aid_platform.model.User;
import com.kosmanenko.vpo_humanitarian_aid_platform.repository.CategoryRepository;
import com.kosmanenko.vpo_humanitarian_aid_platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        initCategories();
        initUsers();
    }

    private void initCategories() {
        List<String> names = List.of(
            "Їжа та продукти харчування",
            "Одяг та взуття",
            "Ліки та медичні товари",
            "Транспорт",
            "Житло",
            "Психологічна підтримка",
            "Юридична допомога",
            "Фінансова допомога",
            "Дитячі товари",
            "Інше"
        );
        for (String name : names) {
            if (!categoryRepository.existsByName(name)) {
                categoryRepository.save(Category.builder().name(name).build());
                log.info("Додано категорію: {}", name);
            }
        }
    }

    private void initUsers() {
        if (!userRepository.existsByEmail("admin@vpo.ua")) {
            userRepository.save(User.builder()
                .email("admin@vpo.ua")
                .passwordHash(passwordEncoder.encode("Admin1234!"))
                .fullName("Адміністратор")
                .role(UserRole.ADMIN)
                .isBlocked(false)
                .isProfilePublic(false)
                .build());
            log.info("Створено акаунт адміністратора");
        }

        if (!userRepository.existsByEmail("vpo@test.ua")) {
            userRepository.save(User.builder()
                .email("vpo@test.ua")
                .passwordHash(passwordEncoder.encode("Test1234!"))
                .fullName("Тестовий ВПО")
                .city("Одеса")
                .role(UserRole.VPO)
                .isBlocked(false)
                .isProfilePublic(false)
                .build());
            log.info("Створено тестового користувача ВПО");
        }

        if (!userRepository.existsByEmail("provider@test.ua")) {
            Category foodCategory = categoryRepository.findByName("Їжа та продукти харчування").orElse(null);
            User provider = User.builder()
                .email("provider@test.ua")
                .passwordHash(passwordEncoder.encode("Test1234!"))
                .fullName("Представник Фонду")
                .city("Київ")
                .role(UserRole.PROVIDER)
                .providerType(ProviderType.ORGANIZATION)
                .orgName("Фонд Допомоги")
                .orgDescription("Благодійний фонд, що надає комплексну допомогу ВПО")
                .isBlocked(false)
                .isProfilePublic(true)
                .providerCategories(foodCategory != null ? Set.of(foodCategory) : Set.of())
                .build();
            userRepository.save(provider);
            log.info("Створено тестового провайдера");
        }
    }
}
