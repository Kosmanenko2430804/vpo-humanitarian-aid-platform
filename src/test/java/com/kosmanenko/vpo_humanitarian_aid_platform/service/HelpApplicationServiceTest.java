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

    // apply — успішно зберігає заявку та публікує подію
    @Test
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

    // apply — кидає виняток якщо оголошення не приймає заявки
    @Test
    void apply_throwsException_whenDoesNotAcceptApplications() {
        announcement.setAcceptsApplications(false);

        assertThatThrownBy(() -> helpApplicationService.apply(announcement, applicant, "Текст"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("не приймає заявки");
    }

    // apply — кидає виняток при повторній заявці
    @Test
    void apply_throwsException_whenDuplicateApplication() {
        when(helpApplicationRepository.existsByAnnouncementAndApplicant(announcement, applicant))
            .thenReturn(true);

        assertThatThrownBy(() -> helpApplicationService.apply(announcement, applicant, "Текст"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("вже подали заявку");
    }

    // accept — успішно приймає PENDING заявку та публікує подію
    @Test
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

    // complete — успішно завершує ACCEPTED заявку та публікує подію
    @Test
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
}
