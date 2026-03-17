package com.kosmanenko.vpo_humanitarian_aid_platform.service;

import com.kosmanenko.vpo_humanitarian_aid_platform.event.AnnouncementApprovedEvent;
import com.kosmanenko.vpo_humanitarian_aid_platform.event.AnnouncementRejectedEvent;
import com.kosmanenko.vpo_humanitarian_aid_platform.event.AnnouncementSubmittedEvent;
import com.kosmanenko.vpo_humanitarian_aid_platform.event.HelpApplicationAcceptedEvent;
import com.kosmanenko.vpo_humanitarian_aid_platform.event.HelpApplicationCompletedEvent;
import com.kosmanenko.vpo_humanitarian_aid_platform.event.HelpApplicationReceivedEvent;
import com.kosmanenko.vpo_humanitarian_aid_platform.event.HelpApplicationRejectedEvent;
import com.kosmanenko.vpo_humanitarian_aid_platform.model.HelpApplication;
import com.kosmanenko.vpo_humanitarian_aid_platform.model.Notification;
import com.kosmanenko.vpo_humanitarian_aid_platform.model.User;
import com.kosmanenko.vpo_humanitarian_aid_platform.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Сервіс сповіщень (Observer pattern).
 * <p>
 * Підписується на події бізнес-логіки через {@link EventListener} і реагує на них
 * двома каналами:
 * <ol>
 *   <li>Збереження сповіщення в БД (відображається в особистому кабінеті)</li>
 *   <li>Надсилання листа на email користувача (best-effort: помилки ігноруються)</li>
 * </ol>
 * </p>
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    /** Репозиторій для зберігання та отримання сповіщень. */
    private final NotificationRepository notificationRepository;

    /** Клієнт для надсилання електронних листів (Spring Mail). */
    private final JavaMailSender mailSender;

    // Обробники подій (Observer)

    /**
     * Обробляє подію затвердження оголошення адміністратором.
     * Сповіщає автора оголошення про успішну публікацію.
     *
     * @param event подія з затвердженим оголошенням
     */
    @EventListener
    @Transactional
    public void onAnnouncementApproved(AnnouncementApprovedEvent event) {
        notify(event.announcement().getAuthor(),
            "Ваше оголошення \"" + event.announcement().getTitle() + "\" опубліковано.");
    }

    /**
     * Обробляє подію відхилення оголошення адміністратором.
     * Сповіщає автора із зазначенням причини відхилення.
     *
     * @param event подія з відхиленим оголошенням та причиною
     */
    @EventListener
    @Transactional
    public void onAnnouncementRejected(AnnouncementRejectedEvent event) {
        notify(event.announcement().getAuthor(),
            "Ваше оголошення \"" + event.announcement().getTitle() + "\" відхилено. Причина: " + event.reason());
    }

    /**
     * Обробляє подію надсилання оголошення на модерацію (нове або повторне).
     * Сповіщає автора про отримання оголошення системою.
     *
     * @param event подія з оголошенням та текстом повідомлення для автора
     */
    @EventListener
    @Transactional
    public void onAnnouncementSubmitted(AnnouncementSubmittedEvent event) {
        notify(event.announcement().getAuthor(), event.message());
    }

    /**
     * Обробляє подію отримання нової заявки на оголошення.
     * Сповіщає надавача (автора оголошення) про нову заявку від ВПО.
     *
     * @param event подія з новою заявкою
     */
    @EventListener
    @Transactional
    public void onHelpApplicationReceived(HelpApplicationReceivedEvent event) {
        HelpApplication app = event.application();
        notify(app.getAnnouncement().getAuthor(),
            "Нова заявка на ваше оголошення \"" + app.getAnnouncement().getTitle() +
            "\" від " + app.getApplicant().getFullName());
    }

    /**
     * Обробляє подію прийняття заявки надавачем.
     * Сповіщає заявника з деталями зустрічі (дата, місце, телефон).
     *
     * @param event подія з прийнятою заявкою
     */
    @EventListener
    @Transactional
    public void onHelpApplicationAccepted(HelpApplicationAcceptedEvent event) {
        HelpApplication app = event.application();
        // Формуємо повідомлення з усіма доступними деталями зустрічі
        String msg = "Ваша заявка на \"" + app.getAnnouncement().getTitle() + "\" прийнята!\n" +
            "Дата: " + (app.getPickupDate() != null ? app.getPickupDate().toString() : "уточнюється") + "\n" +
            "Місце: " + (app.getPickupLocation() != null ? app.getPickupLocation() : "уточнюється") + "\n" +
            "Телефон: " + (app.getProviderPhone() != null ? app.getProviderPhone() : "уточнюється");
        notify(app.getApplicant(), msg);
    }

    /**
     * Обробляє подію відхилення заявки надавачем.
     * Сповіщає заявника із зазначенням причини відхилення.
     *
     * @param event подія з відхиленою заявкою та причиною
     */
    @EventListener
    @Transactional
    public void onHelpApplicationRejected(HelpApplicationRejectedEvent event) {
        HelpApplication app = event.application();
        notify(app.getApplicant(),
            "Вашу заявку на \"" + app.getAnnouncement().getTitle() + "\" відхилено. Причина: " + event.reason());
    }

    /**
     * Обробляє подію завершення заявки надавачем.
     * Запрошує заявника залишити відгук про отриману допомогу.
     *
     * @param event подія із завершеною заявкою
     */
    @EventListener
    @Transactional
    public void onHelpApplicationCompleted(HelpApplicationCompletedEvent event) {
        HelpApplication app = event.application();
        notify(app.getApplicant(),
            "Заявку на \"" + app.getAnnouncement().getTitle() + "\" закрито. Будь ласка, залиште відгук.");
    }

    // Основні методи

    /**
     * Зберігає сповіщення для користувача в БД та надсилає email.
     * Сповіщення зберігається у будь-якому випадку. Email є best-effort:
     * помилки надсилання логуються, але не кидають виняток.
     *
     * @param user    одержувач сповіщення
     * @param message текст сповіщення
     */
    @Transactional
    public void notify(User user, String message) {
        // Зберігаємо сповіщення в БД для відображення в кабінеті
        Notification notification = Notification.builder()
            .user(user)
            .message(message)
            .isRead(false)
            .build();
        notificationRepository.save(notification);
        // Намагаємось надіслати email (помилки не є критичними)
        sendEmail(user.getEmail(), "Сповіщення від платформи ВПО", message);
    }

    /**
     * Надсилає простий текстовий email.
     * Помилки надсилання (наприклад, SMTP недоступний) ігноруються —
     * email є додатковим каналом, а не критичним.
     *
     * @param to      адреса одержувача
     * @param subject тема листа
     * @param text    тіло листа
     */
    private void sendEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setTo(to);
            mail.setSubject(subject);
            mail.setText(text);
            mailSender.send(mail);
        } catch (Exception e) {
            // Email є необов'язковим: помилка не повинна переривати бізнес-операцію
        }
    }

    /**
     * Повертає всі сповіщення користувача, відсортовані від найновіших.
     *
     * @param user користувач
     * @return список сповіщень
     */
    public List<Notification> getNotificationsForUser(User user) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user);
    }

    /**
     * Підраховує кількість непрочитаних сповіщень користувача.
     * Використовується для відображення лічильника в навігаційній панелі.
     *
     * @param user користувач
     * @return кількість непрочитаних сповіщень
     */
    public long countUnread(User user) {
        return notificationRepository.countByUserAndIsReadFalse(user);
    }

    /**
     * Позначає всі непрочитані сповіщення користувача як прочитані.
     * Викликається при відкритті сторінки сповіщень.
     *
     * @param user користувач
     */
    @Transactional
    public void markAllRead(User user) {
        List<Notification> unread = notificationRepository.findByUserAndIsReadFalseOrderByCreatedAtDesc(user);
        // Оновлюємо прапорець для всіх непрочитаних
        unread.forEach(n -> n.setIsRead(true));
        notificationRepository.saveAll(unread);
    }

    /**
     * Позначає одне конкретне сповіщення як прочитане.
     *
     * @param notificationId ідентифікатор сповіщення
     */
    @Transactional
    public void markRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setIsRead(true);
            notificationRepository.save(n);
        });
    }
}
