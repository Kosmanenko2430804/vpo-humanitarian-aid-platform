package com.kosmanenko.vpo_humanitarian_aid_platform.repository;

import com.kosmanenko.vpo_humanitarian_aid_platform.enums.AnnouncementStatus;
import com.kosmanenko.vpo_humanitarian_aid_platform.enums.AnnouncementType;
import com.kosmanenko.vpo_humanitarian_aid_platform.model.Announcement;
import com.kosmanenko.vpo_humanitarian_aid_platform.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.List;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long>,
        JpaSpecificationExecutor<Announcement> {

    List<Announcement> findByAuthor(User author);

    List<Announcement> findByStatusOrderByCreatedAtDesc(AnnouncementStatus status);

    List<Announcement> findByStatusAndPublishedAtBeforeAndArchivedAtIsNull(
        AnnouncementStatus status, LocalDateTime before);

    List<Announcement> findTop6ByStatusAndTypeOrderByCreatedAtDesc(AnnouncementStatus status, AnnouncementType type);

    List<Announcement> findByAuthorAndStatus(User author, AnnouncementStatus status);
}
