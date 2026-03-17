package com.kosmanenko.vpo_humanitarian_aid_platform.service;

import com.kosmanenko.vpo_humanitarian_aid_platform.enums.AnnouncementStatus;
import com.kosmanenko.vpo_humanitarian_aid_platform.model.Announcement;
import com.kosmanenko.vpo_humanitarian_aid_platform.model.Complaint;
import com.kosmanenko.vpo_humanitarian_aid_platform.model.User;
import com.kosmanenko.vpo_humanitarian_aid_platform.repository.AnnouncementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModerationServiceTest {

    @Mock
    private AnnouncementRepository announcementRepository;

    @Mock
    private ComplaintService complaintService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ModerationService moderationService;

    private User author;
    private Announcement announcement;
    private Complaint complaint;

    @BeforeEach
    void setUp() {
        author = User.builder().id(1L).email("author@test.com").build();
        announcement = Announcement.builder()
            .id(10L).title("Тест оголошення")
            .status(AnnouncementStatus.PUBLISHED).author(author).build();
        complaint = Complaint.builder().id(1L).announcement(announcement).build();
    }

    @Test
    @DisplayName("blockAnnouncementFromComplaint — архівує оголошення та позначає скаргу переглянутою")
    void blockAnnouncement_archivesAnnouncement_andMarksComplaintReviewed() {
        when(complaintService.findById(1L)).thenReturn(Optional.of(complaint));
        when(announcementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        moderationService.blockAnnouncementFromComplaint(1L);

        assertThat(announcement.getStatus()).isEqualTo(AnnouncementStatus.ARCHIVED);
        verify(announcementRepository).save(announcement);
        verify(complaintService).markReviewed(1L);
        verify(notificationService).notify(eq(author), any(String.class));
    }

    @Test
    @DisplayName("blockAnnouncementFromComplaint — кидає виняток якщо скарга не знайдена")
    void blockAnnouncement_throwsException_whenComplaintNotFound() {
        when(complaintService.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> moderationService.blockAnnouncementFromComplaint(99L))
            .isInstanceOf(java.util.NoSuchElementException.class);
    }

    @Test
    @DisplayName("dismissComplaint — позначає скаргу як переглянуту")
    void dismissComplaint_marksComplaintReviewed() {
        moderationService.dismissComplaint(5L);

        verify(complaintService).markReviewed(5L);
        verifyNoInteractions(announcementRepository, notificationService);
    }
}
