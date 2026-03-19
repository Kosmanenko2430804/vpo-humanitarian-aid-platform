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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
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
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    /** Репозиторій для зберігання та отримання сповіщень. */
    private final NotificationRepository notificationRepository;

    /** Клієнт для надсилання електронних листів (Spring Mail). */
    private final JavaMailSender mailSender;

    @Value("${app.base-url}")
    private String baseUrl;

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
        var a = event.announcement();
        String body = "Вітаємо, " + a.getAuthor().getFullName() + "!\n\n" +
            "Ваше оголошення «" + a.getTitle() + "» успішно пройшло модерацію та опубліковано на платформі.\n\n" +
            "Переглянути оголошення: " + baseUrl + "/announcements/" + a.getId() + "\n\n" +
            "З повагою,\nКоманда ВПО Допомога";
        notify(a.getAuthor(), body, "Ваше оголошення опубліковано");
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
        var a = event.announcement();
        String body = "Вітаємо, " + a.getAuthor().getFullName() + "!\n\n" +
            "На жаль, ваше оголошення «" + a.getTitle() + "» не пройшло модерацію.\n\n" +
            "Причина: " + event.reason() + "\n\n" +
            "Ви можете відредагувати оголошення та надіслати його повторно у вашому кабінеті: " +
            baseUrl + "/cabinet/announcements\n\n" +
            "З повагою,\nКоманда ВПО Допомога";
        notify(a.getAuthor(), body, "Оголошення відхилено модератором");
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
        var a = event.announcement();
        String body = "Вітаємо, " + a.getAuthor().getFullName() + "!\n\n" +
            event.message() + "\n\n" +
            "Очікуйте на результат модерації — ми надішлемо сповіщення після перевірки.\n\n" +
            "Ваш кабінет: " + baseUrl + "/cabinet/announcements\n\n" +
            "З повагою,\nКоманда ВПО Допомога";
        notify(a.getAuthor(), body, "Оголошення надіслано на модерацію");
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
        var author = app.getAnnouncement().getAuthor();
        String body = "Вітаємо, " + author.getFullName() + "!\n\n" +
            "На ваше оголошення «" + app.getAnnouncement().getTitle() + "» надійшла нова заявка.\n\n" +
            "Заявник: " + app.getApplicant().getFullName() + "\n" +
            "Повідомлення: " + (app.getMessage() != null ? app.getMessage() : "—") + "\n\n" +
            "Переглянути заявки: " + baseUrl + "/cabinet/applications\n\n" +
            "З повагою,\nКоманда ВПО Допомога";
        notify(author, body, "Нова заявка на ваше оголошення");
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
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        String date = app.getPickupDate() != null ? app.getPickupDate().format(fmt) : "уточнюється";
        String body = "Вітаємо, " + app.getApplicant().getFullName() + "!\n\n" +
            "Ваша заявка на оголошення «" + app.getAnnouncement().getTitle() + "» прийнята!\n\n" +
            "Деталі отримання допомоги:\n" +
            "  Дата та час: " + date + "\n" +
            "  Місце: " + (app.getPickupLocation() != null ? app.getPickupLocation() : "уточнюється") + "\n" +
            "  Телефон для зв'язку: " + (app.getProviderPhone() != null ? app.getProviderPhone() : "уточнюється") + "\n\n" +
            "Переглянути в кабінеті: " + baseUrl + "/cabinet/applications\n\n" +
            "З повагою,\nКоманда ВПО Допомога";
        notify(app.getApplicant(), body, "Вашу заявку прийнято");
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
        String body = "Вітаємо, " + app.getApplicant().getFullName() + "!\n\n" +
            "На жаль, вашу заявку на оголошення «" + app.getAnnouncement().getTitle() + "» відхилено.\n\n" +
            "Причина: " + event.reason() + "\n\n" +
            "Ви можете переглянути інші оголошення на платформі: " + baseUrl + "/announcements\n\n" +
            "З повагою,\nКоманда ВПО Допомога";
        notify(app.getApplicant(), body, "Заявку відхилено");
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
        String body = "Вітаємо, " + app.getApplicant().getFullName() + "!\n\n" +
            "Заявку на оголошення «" + app.getAnnouncement().getTitle() + "» завершено.\n\n" +
            "Будь ласка, залиште відгук про отриману допомогу — це допоможе іншим користувачам платформи.\n\n" +
            "Залишити відгук: " + baseUrl + "/cabinet/applications\n\n" +
            "Дякуємо, що користуєтесь платформою!\n\nКоманда ВПО Допомога";
        notify(app.getApplicant(), body, "Заявку завершено — залиште відгук");
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
        notify(user, message, "Сповіщення від платформи ВПО");
    }

    @Transactional
    public void notify(User user, String message, String subject) {
        Notification notification = Notification.builder()
            .user(user)
            .message(message)
            .isRead(false)
            .build();
        notificationRepository.save(notification);
        sendEmail(user.getEmail(), subject, message);
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
            log.info("Email sent to={} subject={}", to, subject);
        } catch (Exception e) {
            log.error("Failed to send email to={} subject={}: {}", to, subject, e.getMessage(), e);
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
