package com.kosmanenko.vpo_humanitarian_aid_platform.service;

import com.kosmanenko.vpo_humanitarian_aid_platform.enums.AnnouncementStatus;
import com.kosmanenko.vpo_humanitarian_aid_platform.enums.ApplicationStatus;
import com.kosmanenko.vpo_humanitarian_aid_platform.event.HelpApplicationAcceptedEvent;
import com.kosmanenko.vpo_humanitarian_aid_platform.event.HelpApplicationCompletedEvent;
import com.kosmanenko.vpo_humanitarian_aid_platform.event.HelpApplicationReceivedEvent;
import com.kosmanenko.vpo_humanitarian_aid_platform.event.HelpApplicationRejectedEvent;
import com.kosmanenko.vpo_humanitarian_aid_platform.model.Announcement;
import com.kosmanenko.vpo_humanitarian_aid_platform.model.HelpApplication;
import com.kosmanenko.vpo_humanitarian_aid_platform.model.User;
import com.kosmanenko.vpo_humanitarian_aid_platform.repository.HelpApplicationRepository;
import com.kosmanenko.vpo_humanitarian_aid_platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Сервіс для управління заявками на гуманітарну допомогу (HelpApplication).
 * Реалізує повний lifecycle заявки: подання → прийняття/відхилення → завершення → відгук.
 * Після кожного переходу стану публікує відповідну подію Spring для автоматичного
 * надсилання сповіщень через {@link NotificationService}.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class HelpApplicationService {

    /** Репозиторій для CRUD операцій із заявками. */
    private final HelpApplicationRepository helpApplicationRepository;

    /** Публікатор подій Spring для ланцюжка сповіщень. */
    private final ApplicationEventPublisher eventPublisher;

    /** Репозиторій користувачів — для оновлення рейтингу надавача. */
    private final UserRepository userRepository;

    /**
     * Знаходить заявку за її ідентифікатором.
     * @param id ідентифікатор заявки
     * @return {@link Optional} із заявкою, або порожній якщо не знайдено
     */
    public Optional<HelpApplication> findById(Long id) {
        return helpApplicationRepository.findById(id);
    }

    /**
     * Повертає всі заявки, подані конкретним заявником, відсортовані від найновіших.
     * @param applicant заявник (ВПО)
     * @return список заявок заявника
     */
    public List<HelpApplication> findByApplicant(User applicant) {
        return helpApplicationRepository.findByApplicantOrderByCreatedAtDesc(applicant);
    }

    /**
     * Повертає всі заявки на конкретне оголошення.
     * @param announcement оголошення надавача
     * @return список заявок на це оголошення
     */
    public List<HelpApplication> findByAnnouncement(Announcement announcement) {
        return helpApplicationRepository.findByAnnouncement(announcement);
    }

    /**
     * Перевіряє, чи вже подав даний користувач заявку на це оголошення.
     *
     * @param announcement оголошення
     * @param user         заявник
     * @return {@code true} якщо заявка вже існує
     */
    public boolean alreadyApplied(Announcement announcement, User user) {
        return helpApplicationRepository.existsByAnnouncementAndApplicant(announcement, user);
    }

    /**
     * Подає заявку ВПО на оголошення надавача.
     * Валідаційні умови:
     *   <оголошення повинно мати статус {@code PUBLISHED}
     *   <оголошення повинно приймати заявки ({@code acceptsApplications == true})
     *   <заявник ще не подав заявку на це оголошення<
     *
     * @param announcement оголошення надавача
     * @param applicant    заявник (ВПО)
     * @param message      повідомлення заявника надавачу
     * @return збережена заявка зі статусом {@code PENDING}
     * @throws RuntimeException якщо будь-яка з умов не виконана
     */
    @Transactional
    public HelpApplication apply(Announcement announcement, User applicant, String message) {
        // Перевірка статусу оголошення
        if (announcement.getStatus() != AnnouncementStatus.PUBLISHED) {
            throw new RuntimeException("Оголошення недоступне для подачі заявок");
        }
        // Перевірка прапорця прийому заявок
        if (!Boolean.TRUE.equals(announcement.getAcceptsApplications())) {
            throw new RuntimeException("Це оголошення не приймає заявки");
        }
        // Перевірка дублікату
        if (helpApplicationRepository.existsByAnnouncementAndApplicant(announcement, applicant)) {
            throw new RuntimeException("Ви вже подали заявку на це оголошення");
        }

        HelpApplication application = HelpApplication.builder()
            .announcement(announcement)
            .applicant(applicant)
            .message(message)
            .status(ApplicationStatus.PENDING)
            .build();

        HelpApplication saved = helpApplicationRepository.save(application);
        // Сповіщення надавача про нову заявку
        eventPublisher.publishEvent(new HelpApplicationReceivedEvent(saved));
        return saved;
    }

    /**
     * Приймає заявку надавачем: встановлює статус {@code ACCEPTED} та зберігає
     * деталі зустрічі (дата, місце, телефон).
     *
     * @param applicationId  ідентифікатор заявки
     * @param pickupDate     дата та час видачі допомоги
     * @param pickupLocation адреса/місце видачі допомоги
     * @param providerPhone  телефон надавача для зв'язку
     * @param provider       поточний надавач (перевірка прав)
     * @throws RuntimeException якщо надавач не є автором оголошення або статус не {@code PENDING}
     */
    @Transactional
    public void accept(Long applicationId, LocalDateTime pickupDate, String pickupLocation,
                       String providerPhone, User provider) {
        HelpApplication app = helpApplicationRepository.findById(applicationId).orElseThrow();
        // Перевірка що надавач є автором оголошення
        validateProviderAccess(app, provider);
        // Прийняти можна лише заявку зі статусом PENDING
        if (app.getStatus() != ApplicationStatus.PENDING) {
            throw new RuntimeException("Можна прийняти лише заявку зі статусом 'Очікує'");
        }

        app.setStatus(ApplicationStatus.ACCEPTED);
        app.setPickupDate(pickupDate);
        app.setPickupLocation(pickupLocation);
        app.setProviderPhone(providerPhone);
        helpApplicationRepository.save(app);
        // Сповіщення заявника про прийняття
        eventPublisher.publishEvent(new HelpApplicationAcceptedEvent(app));
    }

    /**
     * Відхиляє заявку надавачем із зазначенням причини.
     *
     * @param applicationId ідентифікатор заявки
     * @param reason        причина відхилення
     * @param provider      поточний надавач (перевірка прав)
     * @throws RuntimeException якщо надавач не є автором оголошення або статус не {@code PENDING}
     */
    @Transactional
    public void reject(Long applicationId, String reason, User provider) {
        HelpApplication app = helpApplicationRepository.findById(applicationId).orElseThrow();
        validateProviderAccess(app, provider);
        // Відхилити можна лише заявку зі статусом PENDING
        if (app.getStatus() != ApplicationStatus.PENDING) {
            throw new RuntimeException("Можна відхилити лише заявку зі статусом 'Очікує'");
        }

        app.setStatus(ApplicationStatus.REJECTED);
        app.setRejectionReason(reason);
        helpApplicationRepository.save(app);
        // Сповіщення заявника про відхилення
        eventPublisher.publishEvent(new HelpApplicationRejectedEvent(app, reason));
    }

    /**
     * Завершує заявку надавачем після фактичної видачі допомоги.
     * Завершити можна лише прийняту заявку.
     *
     * @param applicationId ідентифікатор заявки
     * @param provider      поточний надавач (перевірка прав)
     * @throws RuntimeException якщо надавач не є автором оголошення або статус не {@code ACCEPTED}
     */
    @Transactional
    public void complete(Long applicationId, User provider) {
        HelpApplication app = helpApplicationRepository.findById(applicationId).orElseThrow();
        validateProviderAccess(app, provider);
        // Завершити можна лише прийняту заявку
        if (app.getStatus() != ApplicationStatus.ACCEPTED) {
            throw new RuntimeException("Можна завершити лише прийняту заявку");
        }

        app.setStatus(ApplicationStatus.COMPLETED);
        helpApplicationRepository.save(app);
        // Сповіщення заявника про завершення з пропозицією залишити відгук
        eventPublisher.publishEvent(new HelpApplicationCompletedEvent(app));
    }

    /**
     * Залишає відгук та рейтинг від заявника після завершення заявки.
     * Після збереження відгуку автоматично перераховується середній рейтинг надавача
     * на основі всіх завершених та оцінених заявок.
     *
     * @param applicationId ідентифікатор заявки
     * @param rating        оцінка від 1 до 5
     * @param review        текстовий відгук (необов'язково)
     * @param applicant     заявник, який залишає відгук
     * @throws RuntimeException якщо заявник не є власником заявки, статус не {@code COMPLETED},
     *                          рейтинг поза межами 1–5, або відгук вже залишено
     */
    @Transactional
    public void leaveReview(Long applicationId, int rating, String review, User applicant) {
        HelpApplication app = helpApplicationRepository.findById(applicationId).orElseThrow();
        // Перевірка що відгук залишає саме заявник
        if (!app.getApplicant().getId().equals(applicant.getId())) {
            throw new RuntimeException("Немає доступу");
        }
        // Відгук можна залишити лише для завершеної заявки
        if (app.getStatus() != ApplicationStatus.COMPLETED) {
            throw new RuntimeException("Можна залишити відгук тільки для завершених заявок");
        }
        // Перевірка допустимого діапазону рейтингу
        if (rating < 1 || rating > 5) {
            throw new RuntimeException("Рейтинг має бути від 1 до 5");
        }
        // Перевірка відсутності повторного відгуку
        if (app.getRating() != null) {
            throw new RuntimeException("Відгук вже залишено");
        }

        app.setRating(rating);
        app.setReview(review);
        helpApplicationRepository.save(app);

        // Перерахунок середнього рейтингу надавача
        User provider = app.getAnnouncement().getAuthor();
        updateProviderRating(provider);
    }

    /**
     * Перераховує та зберігає середній рейтинг надавача на основі всіх
     * завершених заявок із оцінками. Округлення — HALF_UP до 2 знаків.
     *
     * @param provider надавач, рейтинг якого потрібно оновити
     */
    private void updateProviderRating(User provider) {
        // Отримуємо всі заявки з виставленою оцінкою для даного надавача
        List<HelpApplication> allApps = helpApplicationRepository.findRatedApplicationsByProvider(provider);

        if (!allApps.isEmpty()) {
            // Обчислюємо середнє арифметичне всіх оцінок
            double avg = allApps.stream().mapToInt(HelpApplication::getRating).average().orElse(0);
            provider.setRating(BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
            provider.setRatingCount(allApps.size());
            userRepository.save(provider);
        }
    }

    /**
     * Перевіряє, що вказаний надавач є автором оголошення, до якого належить заявка.
     *
     * @param app      заявка
     * @param provider надавач, чиї права перевіряються
     * @throws RuntimeException якщо надавач не є автором оголошення
     */
    private void validateProviderAccess(HelpApplication app, User provider) {
        if (!app.getAnnouncement().getAuthor().getId().equals(provider.getId())) {
            throw new RuntimeException("Немає доступу");
        }
    }
}