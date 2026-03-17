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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Модульні тести для {@link HelpApplicationService}.
 */
@ExtendWith(MockitoExtension.class)
class HelpApplicationServiceTest {

    @Mock
    private HelpApplicationRepository helpApplicationRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private HelpApplicationService helpApplicationService;

    /** Провайдер — автор оголошення */
    private User provider;

    /** Заявник (ВПО) */
    private User applicant;

    /** Тестове опубліковане оголошення */
    private Announcement announcement;

    @BeforeEach
    void setUp() {
        provider = User.builder().id(1L).email("provider@test.com").fullName("Надавач").ratingCount(0).build();
        applicant = User.builder().id(2L).email("vpo@test.com").fullName("ВПО Тест").build();
        announcement = Announcement.builder()
            .id(10L)
            .title("Роздача продуктів")
            .status(AnnouncementStatus.PUBLISHED)
            .acceptsApplications(true)
            .author(provider)
            .build();
    }

    @Test
    @DisplayName("apply — успішно зберігає заявку та публікує подію")
    void apply_success_savesAndPublishesEvent() {
        when(helpApplicationRepository.existsByAnnouncementAndApplicant(announcement, applicant))
            .thenReturn(false);
        HelpApplication saved = HelpApplication.builder()
            .id(1L).announcement(announcement).applicant(applicant)
            .status(ApplicationStatus.PENDING).build();
        when(helpApplicationRepository.save(any())).thenReturn(saved);

        HelpApplication result = helpApplicationService.apply(announcement, applicant, "Потребую допомоги");

        assertThat(result.getStatus()).isEqualTo(ApplicationStatus.PENDING);
        verify(eventPublisher).publishEvent(any(HelpApplicationReceivedEvent.class));
    }

    @Test
    @DisplayName("apply — кидає виняток якщо оголошення не PUBLISHED")
    void apply_throwsException_whenAnnouncementNotPublished() {
        announcement.setStatus(AnnouncementStatus.PENDING);

        assertThatThrownBy(() -> helpApplicationService.apply(announcement, applicant, "Текст"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("недоступне для подачі заявок");
    }

    @Test
    @DisplayName("apply — кидає виняток якщо оголошення не приймає заявки")
    void apply_throwsException_whenDoesNotAcceptApplications() {
        announcement.setAcceptsApplications(false);

        assertThatThrownBy(() -> helpApplicationService.apply(announcement, applicant, "Текст"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("не приймає заявки");
    }

    @Test
    @DisplayName("apply — кидає виняток при повторній заявці")
    void apply_throwsException_whenDuplicateApplication() {
        when(helpApplicationRepository.existsByAnnouncementAndApplicant(announcement, applicant))
            .thenReturn(true);

        assertThatThrownBy(() -> helpApplicationService.apply(announcement, applicant, "Текст"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("вже подали заявку");
    }

    @Test
    @DisplayName("accept — успішно приймає PENDING заявку та публікує подію")
    void accept_pendingApplication_setsAccepted_andPublishesEvent() {
        HelpApplication app = HelpApplication.builder()
            .id(1L).announcement(announcement).applicant(applicant)
            .status(ApplicationStatus.PENDING).build();
        when(helpApplicationRepository.findById(1L)).thenReturn(Optional.of(app));
        when(helpApplicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LocalDateTime pickupDate = LocalDateTime.now().plusDays(1);
        helpApplicationService.accept(1L, pickupDate, "вул. Хрещатик 1", "+380501234567", provider);

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.ACCEPTED);
        assertThat(app.getPickupDate()).isEqualTo(pickupDate);
        assertThat(app.getPickupLocation()).isEqualTo("вул. Хрещатик 1");
        assertThat(app.getProviderPhone()).isEqualTo("+380501234567");
        verify(eventPublisher).publishEvent(any(HelpApplicationAcceptedEvent.class));
    }

    @Test
    @DisplayName("accept — кидає виняток якщо заявка не в статусі PENDING")
    void accept_throwsException_whenNotPending() {
        HelpApplication app = HelpApplication.builder()
            .id(1L).announcement(announcement)
            .status(ApplicationStatus.ACCEPTED).build();
        when(helpApplicationRepository.findById(1L)).thenReturn(Optional.of(app));

        assertThatThrownBy(() -> helpApplicationService.accept(1L, null, null, null, provider))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Очікує");
    }

    @Test
    @DisplayName("accept — кидає виняток якщо надавач не є автором оголошення")
    void accept_throwsException_whenNotProvider() {
        User wrongProvider = User.builder().id(99L).build();
        HelpApplication app = HelpApplication.builder()
            .id(1L).announcement(announcement)
            .status(ApplicationStatus.PENDING).build();
        when(helpApplicationRepository.findById(1L)).thenReturn(Optional.of(app));

        assertThatThrownBy(() -> helpApplicationService.accept(1L, null, null, null, wrongProvider))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Немає доступу");
    }

    @Test
    @DisplayName("reject — кидає виняток якщо заявка не в статусі PENDING")
    void reject_throwsException_whenNotPending() {
        HelpApplication app = HelpApplication.builder()
            .id(1L).announcement(announcement)
            .status(ApplicationStatus.ACCEPTED).build();
        when(helpApplicationRepository.findById(1L)).thenReturn(Optional.of(app));

        assertThatThrownBy(() -> helpApplicationService.reject(1L, "Причина", provider))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Очікує");
    }

    @Test
    @DisplayName("complete — успішно завершує ACCEPTED заявку та публікує подію")
    void complete_acceptedApplication_setsCompleted_andPublishesEvent() {
        HelpApplication app = HelpApplication.builder()
            .id(1L).announcement(announcement).applicant(applicant)
            .status(ApplicationStatus.ACCEPTED).build();
        when(helpApplicationRepository.findById(1L)).thenReturn(Optional.of(app));
        when(helpApplicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        helpApplicationService.complete(1L, provider);

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.COMPLETED);
        verify(eventPublisher).publishEvent(any(HelpApplicationCompletedEvent.class));
    }

    @Test
    @DisplayName("complete — кидає виняток якщо заявка не в статусі ACCEPTED")
    void complete_throwsException_whenNotAccepted() {
        HelpApplication app = HelpApplication.builder()
            .id(1L).announcement(announcement)
            .status(ApplicationStatus.PENDING).build();
        when(helpApplicationRepository.findById(1L)).thenReturn(Optional.of(app));

        assertThatThrownBy(() -> helpApplicationService.complete(1L, provider))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("прийняту заявку");
    }

    @Test
    @DisplayName("leaveReview — зберігає відгук та оновлює рейтинг надавача")
    void leaveReview_savesReview_andUpdatesProviderRating() {
        HelpApplication app = HelpApplication.builder()
            .id(1L).announcement(announcement).applicant(applicant)
            .status(ApplicationStatus.COMPLETED).rating(null).build();
        when(helpApplicationRepository.findById(1L)).thenReturn(Optional.of(app));
        when(helpApplicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Імітуємо список оцінених заявок для обчислення середнього рейтингу
        HelpApplication ratedApp = HelpApplication.builder().rating(4).build();
        when(helpApplicationRepository.findRatedApplicationsByProvider(provider))
            .thenReturn(List.of(ratedApp));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        helpApplicationService.leaveReview(1L, 4, "Чудова допомога!", applicant);

        assertThat(app.getRating()).isEqualTo(4);
        assertThat(app.getReview()).isEqualTo("Чудова допомога!");
        verify(userRepository).save(provider);
    }

    @Test
    @DisplayName("leaveReview — оновлює рейтинг провайдера до середнього значення")
    void leaveReview_updatesProviderRatingToAverage() {
        HelpApplication app = HelpApplication.builder()
            .id(1L).announcement(announcement).applicant(applicant)
            .status(ApplicationStatus.COMPLETED).rating(null).build();
        when(helpApplicationRepository.findById(1L)).thenReturn(Optional.of(app));
        when(helpApplicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Два відгуки: 3 та 5 → середнє 4.00
        HelpApplication app1 = HelpApplication.builder().rating(3).build();
        HelpApplication app2 = HelpApplication.builder().rating(5).build();
        when(helpApplicationRepository.findRatedApplicationsByProvider(provider))
            .thenReturn(List.of(app1, app2));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        helpApplicationService.leaveReview(1L, 5, null, applicant);

        assertThat(provider.getRating()).isEqualByComparingTo(BigDecimal.valueOf(4.00).setScale(2));
        assertThat(provider.getRatingCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("leaveReview — кидає виняток якщо рейтинг поза межами 1–5")
    void leaveReview_throwsException_whenRatingOutOfRange() {
        HelpApplication app = HelpApplication.builder()
            .id(1L).announcement(announcement).applicant(applicant)
            .status(ApplicationStatus.COMPLETED).rating(null).build();
        when(helpApplicationRepository.findById(1L)).thenReturn(Optional.of(app));

        assertThatThrownBy(() -> helpApplicationService.leaveReview(1L, 6, "Текст", applicant))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("від 1 до 5");

        assertThatThrownBy(() -> helpApplicationService.leaveReview(1L, 0, "Текст", applicant))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("від 1 до 5");
    }

    @Test
    @DisplayName("leaveReview — кидає виняток якщо відгук вже залишено")
    void leaveReview_throwsException_whenAlreadyReviewed() {
        HelpApplication app = HelpApplication.builder()
            .id(1L).announcement(announcement).applicant(applicant)
            .status(ApplicationStatus.COMPLETED).rating(5).build();
        when(helpApplicationRepository.findById(1L)).thenReturn(Optional.of(app));

        assertThatThrownBy(() -> helpApplicationService.leaveReview(1L, 4, "Ще один", applicant))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("вже залишено");
    }

    @Test
    @DisplayName("leaveReview — кидає виняток якщо статус не COMPLETED")
    void leaveReview_throwsException_whenNotCompleted() {
        HelpApplication app = HelpApplication.builder()
            .id(1L).announcement(announcement).applicant(applicant)
            .status(ApplicationStatus.ACCEPTED).rating(null).build();
        when(helpApplicationRepository.findById(1L)).thenReturn(Optional.of(app));

        assertThatThrownBy(() -> helpApplicationService.leaveReview(1L, 5, "Текст", applicant))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("завершених заявок");
    }

    @Test
    @DisplayName("leaveReview — кидає виняток якщо заявник не є власником заявки")
    void leaveReview_throwsException_whenNotApplicant() {
        User wrongUser = User.builder().id(99L).build();
        HelpApplication app = HelpApplication.builder()
            .id(1L).announcement(announcement).applicant(applicant)
            .status(ApplicationStatus.COMPLETED).rating(null).build();
        when(helpApplicationRepository.findById(1L)).thenReturn(Optional.of(app));

        assertThatThrownBy(() -> helpApplicationService.leaveReview(1L, 5, "Текст", wrongUser))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Немає доступу");
    }

}
