package com.kosmanenko.vpo_humanitarian_aid_platform.service;

import com.kosmanenko.vpo_humanitarian_aid_platform.event.*;
import com.kosmanenko.vpo_humanitarian_aid_platform.model.Announcement;
import com.kosmanenko.vpo_humanitarian_aid_platform.model.HelpApplication;
import com.kosmanenko.vpo_humanitarian_aid_platform.model.Notification;
import com.kosmanenko.vpo_humanitarian_aid_platform.model.User;
import com.kosmanenko.vpo_humanitarian_aid_platform.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private NotificationService notificationService;

    private User user;
    private Announcement announcement;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).email("user@test.com").fullName("Тест Юзер").build();
        announcement = Announcement.builder().id(1L).title("Оголошення тест").author(user).build();
    }

    @Test
    @DisplayName("notify — зберігає сповіщення в БД та надсилає email")
    void notify_savesNotification_andSendsEmail() {
        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        notificationService.notify(user, "Тестове повідомлення");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getMessage()).isEqualTo("Тестове повідомлення");
        assertThat(captor.getValue().getIsRead()).isFalse();
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("notify — ігнорує помилку надсилання email")
    void notify_ignoresEmailError() {
        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("SMTP error")).when(mailSender).send(any(SimpleMailMessage.class));

        notificationService.notify(user, "Повідомлення");

        verify(notificationRepository).save(any());
    }

    @Test
    @DisplayName("onAnnouncementApproved — сповіщає автора про публікацію")
    void onAnnouncementApproved_notifiesAuthor() {
        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        notificationService.onAnnouncementApproved(new AnnouncementApprovedEvent(announcement));

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getMessage()).contains("опубліковано");
    }

    @Test
    @DisplayName("onHelpApplicationReceived — сповіщає надавача про нову заявку")
    void onHelpApplicationReceived_notifiesProvider() {
        User applicant = User.builder().id(2L).fullName("ВПО Тест").build();
        HelpApplication app = HelpApplication.builder()
            .announcement(announcement).applicant(applicant).build();
        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        notificationService.onHelpApplicationReceived(new HelpApplicationReceivedEvent(app));

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getMessage()).contains("Нова заявка");
    }

    @Test
    @DisplayName("countUnread — повертає кількість непрочитаних")
    void countUnread_returnsCount() {
        when(notificationRepository.countByUserAndIsReadFalse(user)).thenReturn(3L);

        assertThat(notificationService.countUnread(user)).isEqualTo(3L);
    }

    @Test
    @DisplayName("markAllRead — позначає всі непрочитані як прочитані")
    void markAllRead_setsIsReadTrue() {
        Notification n1 = Notification.builder().isRead(false).build();
        Notification n2 = Notification.builder().isRead(false).build();
        when(notificationRepository.findByUserAndIsReadFalseOrderByCreatedAtDesc(user))
            .thenReturn(List.of(n1, n2));

        notificationService.markAllRead(user);

        assertThat(n1.getIsRead()).isTrue();
        assertThat(n2.getIsRead()).isTrue();
    }
}
