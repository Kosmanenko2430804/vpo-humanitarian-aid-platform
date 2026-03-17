package com.kosmanenko.vpo_humanitarian_aid_platform.service;

import com.kosmanenko.vpo_humanitarian_aid_platform.enums.AnnouncementStatus;
import com.kosmanenko.vpo_humanitarian_aid_platform.model.Announcement;
import com.kosmanenko.vpo_humanitarian_aid_platform.repository.AnnouncementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AnnouncementArchivingScheduler {

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementService announcementService;

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void archiveOldAnnouncements() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<Announcement> toArchive = announcementRepository
            .findByStatusAndPublishedAtBeforeAndArchivedAtIsNull(
                AnnouncementStatus.PUBLISHED, thirtyDaysAgo);

        for (Announcement announcement : toArchive) {
            announcementService.archive(announcement);
            log.info("Archived announcement id={} title={}", announcement.getId(), announcement.getTitle());
        }

        if (!toArchive.isEmpty()) {
            log.info("Archived {} announcements", toArchive.size());
        }
    }
}
