package com.kosmanenko.vpo_humanitarian_aid_platform.service;

import com.kosmanenko.vpo_humanitarian_aid_platform.enums.AnnouncementStatus;
import com.kosmanenko.vpo_humanitarian_aid_platform.enums.AnnouncementType;
import com.kosmanenko.vpo_humanitarian_aid_platform.event.AnnouncementApprovedEvent;
import com.kosmanenko.vpo_humanitarian_aid_platform.event.AnnouncementRejectedEvent;
import com.kosmanenko.vpo_humanitarian_aid_platform.event.AnnouncementSubmittedEvent;
import com.kosmanenko.vpo_humanitarian_aid_platform.model.Announcement;
import com.kosmanenko.vpo_humanitarian_aid_platform.model.Category;
import com.kosmanenko.vpo_humanitarian_aid_platform.model.User;
import com.kosmanenko.vpo_humanitarian_aid_platform.repository.AnnouncementRepository;
import com.kosmanenko.vpo_humanitarian_aid_platform.repository.CategoryRepository;
import com.kosmanenko.vpo_humanitarian_aid_platform.repository.HelpApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Модульні тести для {@link AnnouncementService}.
 * Усі залежності замінені мок-об'єктами (Mockito), БД не використовується.
 */
@ExtendWith(MockitoExtension.class)
class AnnouncementServiceTest {

    @Mock
    private AnnouncementRepository announcementRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private HelpApplicationRepository helpApplicationRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AnnouncementService announcementService;

    private User author;
    private Category category;

    @BeforeEach
    void setUp() {
        // Ініціалізація тестових даних перед кожним тестом
        author = User.builder().id(1L).email("user@test.com").fullName("Тест Юзер").build();
        category = Category.builder().id(10L).name("Їжа").build();
    }

    //findById — повертає оголошення якщо існує
    @Test
    void findById_returnsAnnouncement_whenExists() {
        Announcement announcement = Announcement.builder().id(1L).title("Тест").build();
        when(announcementRepository.findById(1L)).thenReturn(Optional.of(announcement));

        Optional<Announcement> result = announcementService.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
    }

    //findById — повертає порожній Optional якщо не існує
    @Test
    void findById_returnsEmpty_whenNotExists() {
        when(announcementRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Announcement> result = announcementService.findById(99L);

        assertThat(result).isEmpty();
    }

    //findPending — повертає оголошення зі статусом PENDING
    @Test
    void findPending_returnsPendingAnnouncements() {
        Announcement a = Announcement.builder().id(1L).status(AnnouncementStatus.PENDING).build();
        when(announcementRepository.findByStatusOrderByCreatedAtDesc(AnnouncementStatus.PENDING))
            .thenReturn(List.of(a));

        List<Announcement> result = announcementService.findPending();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(AnnouncementStatus.PENDING);
    }

    //search без categoryId — викликає findAll зі специфікацією
    @Test
    void search_withoutCategory_usesSpecification() {
        Page<Announcement> page = new PageImpl<>(List.of());
        when(announcementRepository.findAll(any(Specification.class), any(PageRequest.class)))
            .thenReturn(page);

        announcementService.search(AnnouncementType.OFFER, "Київ", null, "допомога", 0);

        verify(announcementRepository).findAll(any(Specification.class), any(PageRequest.class));
    }

    //create — створює оголошення зі статусом PENDING та публікує подію
    @Test
    void create_savedWithPendingStatus_andPublishesEvent() {
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        Announcement saved = Announcement.builder()
            .id(1L).title("Роздача їжі").status(AnnouncementStatus.PENDING).author(author).build();
        when(announcementRepository.save(any())).thenReturn(saved);

        Announcement result = announcementService.create(
            "Роздача їжі", "Опис", "Київ", AnnouncementType.OFFER, true, List.of(10L), author, null);

        assertThat(result.getStatus()).isEqualTo(AnnouncementStatus.PENDING);
        verify(eventPublisher).publishEvent(any(AnnouncementSubmittedEvent.class));
    }

    //create — acceptsApplications null → встановлюється true
    @Test
    void create_nullAcceptsApplications_defaultsToTrue() {
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        ArgumentCaptor<Announcement> captor = ArgumentCaptor.forClass(Announcement.class);
        when(announcementRepository.save(captor.capture())).thenAnswer(inv -> {
            Announcement a = inv.getArgument(0);
            a.setId(1L);
            return a;
        });

        announcementService.create("Тест", "Опис", "Київ", AnnouncementType.OFFER, null, List.of(10L), author, null);

        assertThat(captor.getValue().getAcceptsApplications()).isTrue();
    }

    //update — успішне редагування REJECTED оголошення скидає статус до PENDING
    @Test
    void update_rejectedAnnouncement_resetsToPending() {
        Announcement existing = Announcement.builder()
            .id(1L).status(AnnouncementStatus.REJECTED).author(author).build();
        when(announcementRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        when(announcementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Announcement result = announcementService.update(
            1L, "Новий заголовок", "Новий опис", "Львів", true, List.of(10L), author, null);

        assertThat(result.getStatus()).isEqualTo(AnnouncementStatus.PENDING);
        assertThat(result.getRejectionReason()).isNull();
        verify(eventPublisher).publishEvent(any(AnnouncementSubmittedEvent.class));
    }

    //update — кидає виняток якщо поточний користувач не є автором
    @Test
    void update_throwsException_whenNotAuthor() {
        User anotherUser = User.builder().id(2L).email("other@test.com").build();
        Announcement existing = Announcement.builder()
            .id(1L).status(AnnouncementStatus.PENDING).author(author).build();
        when(announcementRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() ->
            announcementService.update(1L, "Тест", "Опис", "Київ", true, List.of(), anotherUser, null))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Немає доступу");
    }

    //update — кидає виняток для PUBLISHED оголошення
    @Test
    void update_throwsException_whenPublished() {
        Announcement existing = Announcement.builder()
            .id(1L).status(AnnouncementStatus.PUBLISHED).author(author).build();
        when(announcementRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() ->
            announcementService.update(1L, "Тест", "Опис", "Київ", true, List.of(), author, null))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Редагувати можна лише");
    }

    //approve — встановлює статус PUBLISHED та публікує подію
    @Test
    void approve_setsPublishedStatus_andPublishesEvent() {
        Announcement a = Announcement.builder().id(1L).status(AnnouncementStatus.PENDING).build();
        when(announcementRepository.findById(1L)).thenReturn(Optional.of(a));
        when(announcementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        announcementService.approve(1L);

        assertThat(a.getStatus()).isEqualTo(AnnouncementStatus.PUBLISHED);
        assertThat(a.getPublishedAt()).isNotNull();
        verify(eventPublisher).publishEvent(any(AnnouncementApprovedEvent.class));
    }

    //reject — встановлює статус REJECTED з причиною та публікує подію
    @Test
    void reject_setsRejectedStatus_withReason_andPublishesEvent() {
        Announcement a = Announcement.builder().id(1L).status(AnnouncementStatus.PENDING).build();
        when(announcementRepository.findById(1L)).thenReturn(Optional.of(a));
        when(announcementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        announcementService.reject(1L, "Порушення правил");

        assertThat(a.getStatus()).isEqualTo(AnnouncementStatus.REJECTED);
        assertThat(a.getRejectionReason()).isEqualTo("Порушення правил");
        verify(eventPublisher).publishEvent(any(AnnouncementRejectedEvent.class));
    }

    //archive — встановлює статус ARCHIVED та фіксує час архівування
    @Test
    void archive_setsArchivedStatus_andTimestamp() {
        Announcement a = Announcement.builder().id(1L).status(AnnouncementStatus.PUBLISHED).build();
        when(announcementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        announcementService.archive(a);

        assertThat(a.getStatus()).isEqualTo(AnnouncementStatus.ARCHIVED);
        assertThat(a.getArchivedAt()).isNotNull();
    }

    //complete — кидає виняток якщо є активні заявки
    @Test
    void complete_throwsException_whenActiveApplicationsExist() {
        Announcement a = Announcement.builder()
            .id(1L).status(AnnouncementStatus.PUBLISHED).author(author).build();
        when(announcementRepository.findById(1L)).thenReturn(Optional.of(a));
        when(helpApplicationRepository.existsByAnnouncementAndStatusIn(any(), any())).thenReturn(true);

        assertThatThrownBy(() -> announcementService.complete(1L, author))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("активними заявками");
    }

    //complete — кидає виняток якщо оголошення не PUBLISHED
    @Test
    void complete_throwsException_whenNotPublished() {
        Announcement a = Announcement.builder()
            .id(1L).status(AnnouncementStatus.PENDING).author(author).build();
        when(announcementRepository.findById(1L)).thenReturn(Optional.of(a));

        assertThatThrownBy(() -> announcementService.complete(1L, author))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("опубліковане оголошення");
    }
}
