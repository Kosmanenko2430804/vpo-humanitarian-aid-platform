package com.kosmanenko.vpo_humanitarian_aid_platform.service;

import com.kosmanenko.vpo_humanitarian_aid_platform.model.Announcement;
import com.kosmanenko.vpo_humanitarian_aid_platform.model.Complaint;
import com.kosmanenko.vpo_humanitarian_aid_platform.model.User;
import com.kosmanenko.vpo_humanitarian_aid_platform.repository.ComplaintRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Сервіс для управління скаргами користувачів на оголошення.
 * <p>
 * Дозволяє зареєстрованим користувачам повідомляти про порушення в оголошеннях.
 * Адміністратор переглядає скарги та позначає їх як опрацьовані.
 * Запобігає дублюванню скарг від одного й того самого користувача на одне оголошення.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class ComplaintService {

    /** Репозиторій для збереження та пошуку скарг. */
    private final ComplaintRepository complaintRepository;

    /**
     * Подає скаргу від користувача на конкретне оголошення.
     * Перевіряє відсутність попередньої скарги від того самого користувача
     * на те саме оголошення. Нова скарга зберігається зі статусом {@code "PENDING"}.
     *
     * @param announcement оголошення, на яке подається скарга
     * @param complainant  користувач, що подає скаргу
     * @param reason       текст причини скарги
     * @return збережена скарга
     * @throws RuntimeException якщо цей користувач вже подавав скаргу на дане оголошення
     */
    @Transactional
    public Complaint submit(Announcement announcement, User complainant, String reason) {
        // Запобігаємо повторним скаргам від одного користувача
        if (complaintRepository.existsByAnnouncementAndComplainant(announcement, complainant)) {
            throw new RuntimeException("Ви вже подавали скаргу на це оголошення");
        }
        Complaint complaint = Complaint.builder()
            .announcement(announcement)
            .complainant(complainant)
            .reason(reason)
            .status("PENDING") // нова скарга очікує на розгляд адміністратора
            .build();
        return complaintRepository.save(complaint);
    }

    /**
     * Повертає всі скарги в системі, відсортовані від найновіших до найстаріших.
     * Призначено для адмін-панелі.
     *
     * @return список усіх скарг
     */
    public List<Complaint> findAll() {
        return complaintRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * Повертає лише необроблені скарги зі статусом {@code "PENDING"}.
     * Використовується для відображення черги модерації.
     *
     * @return список скарг, що очікують розгляду
     */
    public List<Complaint> findPending() {
        return complaintRepository.findByStatusOrderByCreatedAtDesc("PENDING");
    }

    /**
     * Знаходить скаргу за її ідентифікатором.
     *
     * @param id ідентифікатор скарги
     * @return {@link Optional} зі скаргою, або порожній якщо не знайдено
     */
    public Optional<Complaint> findById(Long id) {
        return complaintRepository.findById(id);
    }

    /**
     * Позначає скаргу як опрацьовану адміністратором (змінює статус на {@code "REVIEWED"}).
     * Якщо скарга з таким ID не існує — операція ігнорується без помилки.
     *
     * @param id ідентифікатор скарги для позначення
     */
    @Transactional
    public void markReviewed(Long id) {
        // ifPresent — безпечна операція, ігнорує відсутній ID
        complaintRepository.findById(id).ifPresent(c -> {
            c.setStatus("REVIEWED");
            complaintRepository.save(c);
        });
    }
}
