package com.kosmanenko.vpo_humanitarian_aid_platform.service;

import com.kosmanenko.vpo_humanitarian_aid_platform.enums.AnnouncementStatus;
import com.kosmanenko.vpo_humanitarian_aid_platform.enums.AnnouncementType;
import com.kosmanenko.vpo_humanitarian_aid_platform.enums.ApplicationStatus;
import com.kosmanenko.vpo_humanitarian_aid_platform.event.AnnouncementApprovedEvent;
import com.kosmanenko.vpo_humanitarian_aid_platform.event.AnnouncementRejectedEvent;
import com.kosmanenko.vpo_humanitarian_aid_platform.event.AnnouncementSubmittedEvent;
import com.kosmanenko.vpo_humanitarian_aid_platform.model.Announcement;
import com.kosmanenko.vpo_humanitarian_aid_platform.model.Category;
import com.kosmanenko.vpo_humanitarian_aid_platform.model.User;
import com.kosmanenko.vpo_humanitarian_aid_platform.repository.AnnouncementRepository;
import com.kosmanenko.vpo_humanitarian_aid_platform.repository.CategoryRepository;
import com.kosmanenko.vpo_humanitarian_aid_platform.repository.HelpApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Сервіс для управління оголошеннями про гуманітарну допомогу.
 * <p>
 * Відповідає за створення, редагування, модерацію (затвердження/відхилення),
 * архівування та завершення оголошень. Публікує події Spring для надсилання
 * сповіщень після кожної зміни статусу.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class AnnouncementService {

    /** Репозиторій для роботи з оголошеннями в БД. */
    private final AnnouncementRepository announcementRepository;

    /** Репозиторій для отримання категорій допомоги. */
    private final CategoryRepository categoryRepository;

    /** Репозиторій заявок використовується для перевірки активних заявок при завершенні. */
    private final HelpApplicationRepository helpApplicationRepository;

    /** Публікатор подій Spring для ланцюжка сповіщень (Observer pattern). */
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Знаходить оголошення за його ідентифікатором.
     *
     * @param id ідентифікатор оголошення
     * @return {@link Optional} з оголошенням, або порожній якщо не знайдено
     */
    public Optional<Announcement> findById(Long id) {
        return announcementRepository.findById(id);
    }

    /**
     * Повертає всі оголошення, створені конкретним користувачем.
     *
     * @param author автор оголошень
     * @return список оголошень автора
     */
    public List<Announcement> findByAuthor(User author) {
        return announcementRepository.findByAuthor(author);
    }

    /**
     * Повертає список оголошень зі статусом {@code PENDING} (очікують модерації),
     * відсортованих від найновіших до найстаріших.
     *
     * @return список оголошень на модерації
     */
    public List<Announcement> findPending() {
        return announcementRepository.findByStatusOrderByCreatedAtDesc(AnnouncementStatus.PENDING);
    }

    /**
     * Повертає 6 найновіших опублікованих оголошень типу OFFER (пропозиція допомоги).
     *
     * @return список останніх пропозицій допомоги
     */
    public List<Announcement> findTop6Offers() {
        return announcementRepository.findTop6ByStatusAndTypeOrderByCreatedAtDesc(
            AnnouncementStatus.PUBLISHED, AnnouncementType.OFFER);
    }

    /**
     * Повертає 6 найновіших опублікованих оголошень типу REQUEST (запит допомоги).
     *
     * @return список останніх запитів допомоги
     */
    public List<Announcement> findTop6Requests() {
        return announcementRepository.findTop6ByStatusAndTypeOrderByCreatedAtDesc(
            AnnouncementStatus.PUBLISHED, AnnouncementType.REQUEST);
    }

    /**
     * Виконує пошук опублікованих оголошень за фільтрами.
     * Якщо вказано {@code categoryId}, використовує запит з фільтрацією по категорії.
     * Рядки {@code city} та {@code keyword} перетворюються на шаблони SQL LIKE (нижній регістр).
     *
     * @param type       тип оголошення (OFFER або REQUEST); {@code null} — усі типи
     * @param city       місто для фільтрації; {@code null} або порожній — без фільтра
     * @param categoryId ідентифікатор категорії; {@code null} — без фільтра
     * @param keyword    ключове слово для пошуку в назві/описі; {@code null} — без фільтра
     * @param page       номер сторінки (0-based), по 12 результатів на сторінку
     * @return сторінка результатів пошуку
     */
    public Page<Announcement> search(AnnouncementType type, String city, Long categoryId, String keyword, int page) {
        PageRequest pageable = PageRequest.of(page, 12);
        // Перетворюємо фільтри на шаблони LIKE або null для ігнорування
        String cityLike = (city != null && !city.isBlank()) ? "%" + city.toLowerCase() + "%" : null;
        String keywordLike = (keyword != null && !keyword.isBlank()) ? "%" + keyword.toLowerCase() + "%" : null;
        if (categoryId != null) {
            // Запит з JOIN по таблиці категорій
            return announcementRepository.searchAnnouncementsWithCategory(type, cityLike, categoryId, keywordLike, pageable);
        }
        return announcementRepository.searchAnnouncements(type, cityLike, keywordLike, pageable);
    }

    /**
     * Створює нове оголошення зі статусом {@code PENDING} та публікує подію
     * {@link AnnouncementSubmittedEvent} для сповіщення автора.
     * @param title               заголовок оголошення
     * @param description         детальний опис
     * @param city                місто розташування/допомоги
     * @param type                тип оголошення (OFFER або REQUEST)
     * @param acceptsApplications чи приймає оголошення заявки від ВПО
     * @param categoryIds         список ідентифікаторів категорій допомоги
     * @param author              автор оголошення
     * @return збережене оголошення
     */
    @Transactional
    public Announcement create(String title, String description, String city,
                               AnnouncementType type, Boolean acceptsApplications,
                               List<Long> categoryIds, User author) {
        // Завантажуємо категорії за ідентифікаторами
        Set<Category> categories = categoryIds.stream()
            .map(id -> categoryRepository.findById(id).orElseThrow())
            .collect(Collectors.toSet());

        Announcement announcement = Announcement.builder()
            .title(title)
            .description(description)
            .city(city)
            .type(type)
            .status(AnnouncementStatus.PENDING) // нові оголошення завжди на модерації
            .acceptsApplications(acceptsApplications != null ? acceptsApplications : true)
            .author(author)
            .categories(categories)
            .build();

        Announcement saved = announcementRepository.save(announcement);
        // Повідомляємо автора про успішне надсилання на модерацію
        eventPublisher.publishEvent(new AnnouncementSubmittedEvent(saved,
            "Ваше оголошення \"" + title + "\" надіслано на модерацію."));
        return saved;
    }

    /**
     * Оновлює існуюче оголошення. Редагувати можна лише оголошення зі статусом
     * {@code PENDING} або {@code REJECTED}. Після редагування статус скидається
     * до {@code PENDING} для повторної модерації.
     *
     * @param id                  ідентифікатор оголошення
     * @param title               новий заголовок
     * @param description         новий опис
     * @param city                нове місто
     * @param acceptsApplications нове значення прийому заявок
     * @param categoryIds         нові категорії
     * @param currentUser         поточний автентифікований користувач
     * @return оновлене оголошення
     * @throws RuntimeException якщо оголошення не знайдено, користувач не є автором,
     *                          або статус не дозволяє редагування
     */
    @Transactional
    public Announcement update(Long id, String title, String description, String city,
                               Boolean acceptsApplications, List<Long> categoryIds, User currentUser) {
        Announcement announcement = announcementRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Оголошення не знайдено"));

        // Перевірка прав: редагувати може лише автор
        if (!announcement.getAuthor().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Немає доступу");
        }
        // Перевірка статусу: дозволено лише PENDING або REJECTED
        if (announcement.getStatus() != AnnouncementStatus.PENDING
                && announcement.getStatus() != AnnouncementStatus.REJECTED) {
            throw new RuntimeException("Редагувати можна лише оголошення в статусі 'На модерації' або 'Відхилено'");
        }

        Set<Category> categories = categoryIds.stream()
            .map(catId -> categoryRepository.findById(catId).orElseThrow())
            .collect(Collectors.toSet());

        announcement.setTitle(title);
        announcement.setDescription(description);
        announcement.setCity(city);
        announcement.setAcceptsApplications(acceptsApplications != null ? acceptsApplications : true);
        announcement.setCategories(categories);
        // Скидаємо статус до PENDING для повторної модерації
        announcement.setStatus(AnnouncementStatus.PENDING);
        announcement.setRejectionReason(null);

        Announcement saved = announcementRepository.save(announcement);
        eventPublisher.publishEvent(new AnnouncementSubmittedEvent(saved,
            "Ваше оголошення \"" + title + "\" оновлено та надіслано на повторну модерацію."));
        return saved;
    }

    /**
     * Затверджує оголошення адміністратором: встановлює статус {@code PUBLISHED},
     * фіксує час публікації та публікує подію {@link AnnouncementApprovedEvent}.
     *
     * @param id ідентифікатор оголошення для затвердження
     */
    @Transactional
    public void approve(Long id) {
        Announcement a = announcementRepository.findById(id).orElseThrow();
        a.setStatus(AnnouncementStatus.PUBLISHED);
        a.setPublishedAt(LocalDateTime.now());
        announcementRepository.save(a);
        // Сповіщення автора про публікацію
        eventPublisher.publishEvent(new AnnouncementApprovedEvent(a));
    }

    /**
     * Відхиляє оголошення адміністратором: встановлює статус {@code REJECTED},
     * зберігає причину відхилення та публікує подію {@link AnnouncementRejectedEvent}.
     *
     * @param id     ідентифікатор оголошення
     * @param reason причина відхилення, яку побачить автор
     */
    @Transactional
    public void reject(Long id, String reason) {
        Announcement a = announcementRepository.findById(id).orElseThrow();
        a.setStatus(AnnouncementStatus.REJECTED);
        a.setRejectionReason(reason);
        announcementRepository.save(a);
        // Сповіщення автора про відхилення із зазначенням причини
        eventPublisher.publishEvent(new AnnouncementRejectedEvent(a, reason));
    }

    /**
     * Архівує оголошення: встановлює статус {@code ARCHIVED} та фіксує час архівування.
     * Викликається планувальником {@code AnnouncementArchivingScheduler} для оголошень,
     * старших за 30 днів.
     *
     * @param announcement оголошення для архівування
     */
    @Transactional
    public void archive(Announcement announcement) {
        announcement.setStatus(AnnouncementStatus.ARCHIVED);
        announcement.setArchivedAt(LocalDateTime.now());
        announcementRepository.save(announcement);
    }

    /**
     * Повторно публікує архівоване оголошення: переводить статус до {@code PENDING}
     * для проходження повторної модерації.
     *
     * @param id          ідентифікатор оголошення
     * @param currentUser поточний автентифікований користувач
     * @throws RuntimeException якщо поточний користувач не є автором
     *                          або оголошення не в статусі {@code ARCHIVED}
     */
    @Transactional
    public void republish(Long id, User currentUser) {
        Announcement a = announcementRepository.findById(id).orElseThrow();
        // Перевірка прав доступу
        if (!a.getAuthor().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Немає доступу");
        }
        // Дозволяємо повторну публікацію лише для архівованих оголошень
        if (a.getStatus() != AnnouncementStatus.ARCHIVED) {
            throw new RuntimeException("Повторно опублікувати можна лише архівоване оголошення");
        }
        a.setStatus(AnnouncementStatus.PENDING);
        a.setArchivedAt(null);
        announcementRepository.save(a);
        eventPublisher.publishEvent(new AnnouncementSubmittedEvent(a,
            "Оголошення \"" + a.getTitle() + "\" надіслано на повторну модерацію."));
    }

    /**
     * Завершує оголошення автором: переводить статус до {@code COMPLETED}.
     * Завершення неможливе, якщо є активні заявки (у статусі {@code PENDING} або
     * {@code ACCEPTED}) — це запобігає залишенню заявників без відповіді.
     *
     * @param id          ідентифікатор оголошення
     * @param currentUser поточний автентифікований користувач
     * @throws RuntimeException якщо поточний користувач не є автором, статус не
     *                          {@code PUBLISHED}, або є активні заявки
     */
    @Transactional
    public void complete(Long id, User currentUser) {
        Announcement a = announcementRepository.findById(id).orElseThrow();
        // Перевірка прав доступу
        if (!a.getAuthor().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Немає доступу");
        }
        // Завершити можна лише опубліковане оголошення
        if (a.getStatus() != AnnouncementStatus.PUBLISHED) {
            throw new RuntimeException("Закрити можна лише опубліковане оголошення");
        }
        // Перевірка наявності активних заявок
        boolean hasActive = helpApplicationRepository.existsByAnnouncementAndStatusIn(
            a, List.of(ApplicationStatus.PENDING, ApplicationStatus.ACCEPTED));
        if (hasActive) {
            throw new RuntimeException("Неможливо закрити оголошення з активними заявками");
        }
        a.setStatus(AnnouncementStatus.COMPLETED);
        announcementRepository.save(a);
    }
}
