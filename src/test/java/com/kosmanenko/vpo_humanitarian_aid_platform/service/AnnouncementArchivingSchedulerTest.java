package com.kosmanenko.vpo_humanitarian_aid_platform.service;

import com.kosmanenko.vpo_humanitarian_aid_platform.enums.AnnouncementStatus;
import com.kosmanenko.vpo_humanitarian_aid_platform.model.Announcement;
import com.kosmanenko.vpo_humanitarian_aid_platform.repository.AnnouncementRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnnouncementArchivingSchedulerTest {

    @Mock
    private AnnouncementRepository announcementRepository;

    @Mock
    private AnnouncementService announcementService;

    @InjectMocks
    private AnnouncementArchivingScheduler scheduler;

    @Test
    @DisplayName("archiveOldAnnouncements — архівує оголошення старше 30 днів")
    void archiveOldAnnouncements_archivesEligibleAnnouncements() {
        Announcement old1 = Announcement.builder().id(1L)
            .status(AnnouncementStatus.PUBLISHED)
            .publishedAt(LocalDateTime.now().minusDays(31))
            .build();
        Announcement old2 = Announcement.builder().id(2L)
            .status(AnnouncementStatus.PUBLISHED)
            .publishedAt(LocalDateTime.now().minusDays(35))
            .build();

        when(announcementRepository.findByStatusAndPublishedAtBeforeAndArchivedAtIsNull(
            eq(AnnouncementStatus.PUBLISHED), any(LocalDateTime.class)))
            .thenReturn(List.of(old1, old2));

        scheduler.archiveOldAnnouncements();

        verify(announcementService).archive(old1);
        verify(announcementService).archive(old2);
    }

    @Test
    @DisplayName("archiveOldAnnouncements — не викликає archive якщо немає кандидатів")
    void archiveOldAnnouncements_doesNothing_whenNoEligibleAnnouncements() {
        when(announcementRepository.findByStatusAndPublishedAtBeforeAndArchivedAtIsNull(
            any(), any())).thenReturn(List.of());

        scheduler.archiveOldAnnouncements();

        verify(announcementService, never()).archive(any());
    }

}
