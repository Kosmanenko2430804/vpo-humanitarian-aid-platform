package com.kosmanenko.vpo_humanitarian_aid_platform.repository;

import com.kosmanenko.vpo_humanitarian_aid_platform.enums.AnnouncementStatus;
import com.kosmanenko.vpo_humanitarian_aid_platform.enums.AnnouncementType;
import com.kosmanenko.vpo_humanitarian_aid_platform.model.Announcement;
import com.kosmanenko.vpo_humanitarian_aid_platform.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    List<Announcement> findByAuthor(User author);

    List<Announcement> findByStatusOrderByCreatedAtDesc(AnnouncementStatus status);

    List<Announcement> findByStatusAndPublishedAtBeforeAndArchivedAtIsNull(
        AnnouncementStatus status, LocalDateTime before);

    @Query("SELECT a FROM Announcement a WHERE a.status = 'PUBLISHED'" +
           " AND (:type IS NULL OR a.type = :type)" +
           " AND (:cityLike IS NULL OR LOWER(a.city) LIKE :cityLike)" +
           " AND (:keywordLike IS NULL OR LOWER(a.title) LIKE :keywordLike" +
           "     OR LOWER(a.description) LIKE :keywordLike)" +
           " ORDER BY a.createdAt DESC")
    Page<Announcement> searchAnnouncements(
        @Param("type") AnnouncementType type,
        @Param("cityLike") String cityLike,
        @Param("keywordLike") String keywordLike,
        Pageable pageable);

    @Query("SELECT a FROM Announcement a JOIN a.categories c WHERE a.status = 'PUBLISHED'" +
           " AND (:type IS NULL OR a.type = :type)" +
           " AND (:cityLike IS NULL OR LOWER(a.city) LIKE :cityLike)" +
           " AND c.id = :categoryId" +
           " AND (:keywordLike IS NULL OR LOWER(a.title) LIKE :keywordLike" +
           "     OR LOWER(a.description) LIKE :keywordLike)" +
           " ORDER BY a.createdAt DESC")
    Page<Announcement> searchAnnouncementsWithCategory(
        @Param("type") AnnouncementType type,
        @Param("cityLike") String cityLike,
        @Param("categoryId") Long categoryId,
        @Param("keywordLike") String keywordLike,
        Pageable pageable);

    List<Announcement> findTop6ByStatusAndTypeOrderByCreatedAtDesc(AnnouncementStatus status, AnnouncementType type);

    List<Announcement> findByAuthorAndStatus(User author, AnnouncementStatus status);
}
