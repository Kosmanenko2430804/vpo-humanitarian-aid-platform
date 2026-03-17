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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

    @Test
    @DisplayName("findById — повертає оголошення якщо існує")
    void findById_returnsAnnouncement_whenExists() {
        Announcement announcement = Announcement.builder().id(1L).title("Тест").build();
        when(announcementRepository.findById(1L)).thenReturn(Optional.of(announcement));

        Optional<Announcement> result = announcementService.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("findById — повертає порожній Optional якщо не існує")
    void findById_returnsEmpty_whenNotExists() {
        when(announcementRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Announcement> result = announcementService.findById(99L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findPending — повертає оголошення зі статусом PENDING")
    void findPending_returnsPendingAnnouncements() {
        Announcement a = Announcement.builder().id(1L).status(AnnouncementStatus.PENDING).build();
        when(announcementRepository.findByStatusOrderByCreatedAtDesc(AnnouncementStatus.PENDING))
            .thenReturn(List.of(a));

        List<Announcement> result = announcementService.findPending();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(AnnouncementStatus.PENDING);
    }

    @Test
    @DisplayName("search без categoryId — використовує searchAnnouncements")
    void search_withoutCategory_usesSearchAnnouncements() {
        Page<Announcement> page = new PageImpl<>(List.of());
        when(announcementRepository.searchAnnouncements(any(), any(), any(), any()))
            .thenReturn(page);

        announcementService.search(AnnouncementType.OFFER, "Київ", null, "допомога", 0);

        verify(announcementRepository).searchAnnouncements(eq(AnnouncementType.OFFER),
            eq("%київ%"), eq("%допомога%"), any(PageRequest.class));
        verify(announcementRepository, never()).searchAnnouncementsWithCategory(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("search з categoryId — використовує searchAnnouncementsWithCategory")
    void search_withCategory_usesSearchAnnouncementsWithCategory() {
        Page<Announcement> page = new PageImpl<>(List.of());
        when(announcementRepository.searchAnnouncementsWithCategory(any(), any(), any(), any(), any()))
            .thenReturn(page);

        announcementService.search(null, null, 5L, null, 0);

        verify(announcementRepository).searchAnnouncementsWithCategory(
            isNull(), isNull(), eq(5L), isNull(), any(PageRequest.class));
        verify(announcementRepository, never()).searchAnnouncements(any(), any(), any(), any());
    }

    @Test
    @DisplayName("create — створює оголошення зі статусом PENDING та публікує подію")
    void create_savedWithPendingStatus_andPublishesEvent() {
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        Announcement saved = Announcement.builder()
            .id(1L).title("Роздача їжі").status(AnnouncementStatus.PENDING).author(author).build();
        when(announcementRepository.save(any())).thenReturn(saved);

        Announcement result = announcementService.create(
            "Роздача їжі", "Опис", "Київ", AnnouncementType.OFFER, true, List.of(10L), author);

        assertThat(result.getStatus()).isEqualTo(AnnouncementStatus.PENDING);
        verify(eventPublisher).publishEvent(any(AnnouncementSubmittedEvent.class));
    }

    @Test
    @DisplayName("create — acceptsApplications null → встановлюється true")
    void create_nullAcceptsApplications_defaultsToTrue() {
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        ArgumentCaptor<Announcement> captor = ArgumentCaptor.forClass(Announcement.class);
        when(announcementRepository.save(captor.capture())).thenAnswer(inv -> {
            Announcement a = inv.getArgument(0);
            a.setId(1L);
            return a;
        });

        announcementService.create("Тест", "Опис", "Київ", AnnouncementType.OFFER, null, List.of(10L), author);

        assertThat(captor.getValue().getAcceptsApplications()).isTrue();
    }

    @Test
    @DisplayName("update — успішне редагування REJECTED оголошення скидає статус до PENDING")
    void update_rejectedAnnouncement_resetsToPending() {
        Announcement existing = Announcement.builder()
            .id(1L).status(AnnouncementStatus.REJECTED).author(author).build();
        when(announcementRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        when(announcementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Announcement result = announcementService.update(
            1L, "Новий заголовок", "Новий опис", "Львів", true, List.of(10L), author);

        assertThat(result.getStatus()).isEqualTo(AnnouncementStatus.PENDING);
        assertThat(result.getRejectionReason()).isNull();
        verify(eventPublisher).publishEvent(any(AnnouncementSubmittedEvent.class));
    }

    @Test
    @DisplayName("update — кидає виняток якщо поточний користувач не є автором")
    void update_throwsException_whenNotAuthor() {
        User anotherUser = User.builder().id(2L).email("other@test.com").build();
        Announcement existing = Announcement.builder()
            .id(1L).status(AnnouncementStatus.PENDING).author(author).build();
        when(announcementRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() ->
            announcementService.update(1L, "Тест", "Опис", "Київ", true, List.of(), anotherUser))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Немає доступу");
    }

    @Test
    @DisplayName("update — кидає виняток для PUBLISHED оголошення")
    void update_throwsException_whenPublished() {
        Announcement existing = Announcement.builder()
            .id(1L).status(AnnouncementStatus.PUBLISHED).author(author).build();
        when(announcementRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() ->
            announcementService.update(1L, "Тест", "Опис", "Київ", true, List.of(), author))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Редагувати можна лише");
    }

    @Test
    @DisplayName("approve — встановлює статус PUBLISHED та публікує подію")
    void approve_setsPublishedStatus_andPublishesEvent() {
        Announcement a = Announcement.builder().id(1L).status(AnnouncementStatus.PENDING).build();
        when(announcementRepository.findById(1L)).thenReturn(Optional.of(a));
        when(announcementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        announcementService.approve(1L);

        assertThat(a.getStatus()).isEqualTo(AnnouncementStatus.PUBLISHED);
        assertThat(a.getPublishedAt()).isNotNull();
        verify(eventPublisher).publishEvent(any(AnnouncementApprovedEvent.class));
    }

    @Test
    @DisplayName("reject — встановлює статус REJECTED з причиною та публікує подію")
    void reject_setsRejectedStatus_withReason_andPublishesEvent() {
        Announcement a = Announcement.builder().id(1L).status(AnnouncementStatus.PENDING).build();
        when(announcementRepository.findById(1L)).thenReturn(Optional.of(a));
        when(announcementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        announcementService.reject(1L, "Порушення правил");

        assertThat(a.getStatus()).isEqualTo(AnnouncementStatus.REJECTED);
        assertThat(a.getRejectionReason()).isEqualTo("Порушення правил");
        verify(eventPublisher).publishEvent(any(AnnouncementRejectedEvent.class));
    }

    @Test
    @DisplayName("archive — встановлює статус ARCHIVED та фіксує час архівування")
    void archive_setsArchivedStatus_andTimestamp() {
        Announcement a = Announcement.builder().id(1L).status(AnnouncementStatus.PUBLISHED).build();
        when(announcementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        announcementService.archive(a);

        assertThat(a.getStatus()).isEqualTo(AnnouncementStatus.ARCHIVED);
        assertThat(a.getArchivedAt()).isNotNull();
    }

    @Test
    @DisplayName("republish — успішно переводить ARCHIVED оголошення до PENDING")
    void republish_archivedAnnouncement_setsToPending() {
        Announcement a = Announcement.builder()
            .id(1L).title("Тест").status(AnnouncementStatus.ARCHIVED).author(author).build();
        when(announcementRepository.findById(1L)).thenReturn(Optional.of(a));
        when(announcementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        announcementService.republish(1L, author);

        assertThat(a.getStatus()).isEqualTo(AnnouncementStatus.PENDING);
        assertThat(a.getArchivedAt()).isNull();
        verify(eventPublisher).publishEvent(any(AnnouncementSubmittedEvent.class));
    }

    @Test
    @DisplayName("republish — кидає виняток якщо оголошення не ARCHIVED")
    void republish_throwsException_whenNotArchived() {
        Announcement a = Announcement.builder()
            .id(1L).status(AnnouncementStatus.PUBLISHED).author(author).build();
        when(announcementRepository.findById(1L)).thenReturn(Optional.of(a));

        assertThatThrownBy(() -> announcementService.republish(1L, author))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("архівоване оголошення");
    }

    @Test
    @DisplayName("republish — кидає виняток якщо поточний користувач не є автором")
    void republish_throwsException_whenNotAuthor() {
        User anotherUser = User.builder().id(99L).build();
        Announcement a = Announcement.builder()
            .id(1L).status(AnnouncementStatus.ARCHIVED).author(author).build();
        when(announcementRepository.findById(1L)).thenReturn(Optional.of(a));

        assertThatThrownBy(() -> announcementService.republish(1L, anotherUser))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Немає доступу");
    }

    @Test
    @DisplayName("complete — кидає виняток якщо є активні заявки")
    void complete_throwsException_whenActiveApplicationsExist() {
        Announcement a = Announcement.builder()
            .id(1L).status(AnnouncementStatus.PUBLISHED).author(author).build();
        when(announcementRepository.findById(1L)).thenReturn(Optional.of(a));
        when(helpApplicationRepository.existsByAnnouncementAndStatusIn(any(), any())).thenReturn(true);

        assertThatThrownBy(() -> announcementService.complete(1L, author))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("активними заявками");
    }

    @Test
    @DisplayName("complete — кидає виняток якщо оголошення не PUBLISHED")
    void complete_throwsException_whenNotPublished() {
        Announcement a = Announcement.builder()
            .id(1L).status(AnnouncementStatus.PENDING).author(author).build();
        when(announcementRepository.findById(1L)).thenReturn(Optional.of(a));

        assertThatThrownBy(() -> announcementService.complete(1L, author))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("опубліковане оголошення");
    }

    @Test
    @DisplayName("complete — кидає виняток якщо поточний користувач не є автором")
    void complete_throwsException_whenNotAuthor() {
        User anotherUser = User.builder().id(99L).build();
        Announcement a = Announcement.builder()
            .id(1L).status(AnnouncementStatus.PUBLISHED).author(author).build();
        when(announcementRepository.findById(1L)).thenReturn(Optional.of(a));

        assertThatThrownBy(() -> announcementService.complete(1L, anotherUser))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Немає доступу");
    }
}
