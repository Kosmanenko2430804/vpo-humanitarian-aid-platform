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

import java.util.List;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    List<Announcement> findByAuthor(User author);

    List<Announcement> findByStatusOrderByCreatedAtDesc(AnnouncementStatus status);

    List<Announcement> findTop6ByStatusAndTypeOrderByCreatedAtDesc(AnnouncementStatus status, AnnouncementType type);

    long countByStatus(AnnouncementStatus status);

    @Query(value = "SELECT DISTINCT a FROM Announcement a LEFT JOIN a.categories c " +
                   "WHERE a.status = 'PUBLISHED' " +
                   "AND (:type IS NULL OR a.type = :type) " +
                   "AND (:city IS NULL OR LOWER(a.city) LIKE :city) " +
                   "AND (:categoryId IS NULL OR c.id = :categoryId) " +
                   "AND (:keyword IS NULL OR LOWER(a.title) LIKE :keyword OR LOWER(a.description) LIKE :keyword) " +
                   "ORDER BY a.createdAt DESC",
           countQuery = "SELECT COUNT(DISTINCT a) FROM Announcement a LEFT JOIN a.categories c " +
                        "WHERE a.status = 'PUBLISHED' " +
                        "AND (:type IS NULL OR a.type = :type) " +
                        "AND (:city IS NULL OR LOWER(a.city) LIKE :city) " +
                        "AND (:categoryId IS NULL OR c.id = :categoryId) " +
                        "AND (:keyword IS NULL OR LOWER(a.title) LIKE :keyword OR LOWER(a.description) LIKE :keyword)")
    Page<Announcement> searchPublished(@Param("type") AnnouncementType type,
                                       @Param("city") String city,
                                       @Param("categoryId") Long categoryId,
                                       @Param("keyword") String keyword,
                                       Pageable pageable);
}
