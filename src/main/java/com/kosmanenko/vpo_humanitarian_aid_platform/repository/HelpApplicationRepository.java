package com.kosmanenko.vpo_humanitarian_aid_platform.repository;

import com.kosmanenko.vpo_humanitarian_aid_platform.enums.ApplicationStatus;
import com.kosmanenko.vpo_humanitarian_aid_platform.model.Announcement;
import com.kosmanenko.vpo_humanitarian_aid_platform.model.HelpApplication;
import com.kosmanenko.vpo_humanitarian_aid_platform.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface HelpApplicationRepository extends JpaRepository<HelpApplication, Long> {
    List<HelpApplication> findByApplicant(User applicant);
    List<HelpApplication> findByAnnouncement(Announcement announcement);
    Optional<HelpApplication> findByAnnouncementAndApplicant(Announcement announcement, User applicant);
    boolean existsByAnnouncementAndApplicant(Announcement announcement, User applicant);
    List<HelpApplication> findByAnnouncementAndStatus(Announcement announcement, ApplicationStatus status);
    boolean existsByAnnouncementAndStatusIn(Announcement announcement, List<ApplicationStatus> statuses);
    List<HelpApplication> findByApplicantOrderByCreatedAtDesc(User applicant);

    @Query("SELECT a FROM HelpApplication a WHERE a.announcement.author = :provider AND a.rating IS NOT NULL")
    List<HelpApplication> findRatedApplicationsByProvider(@Param("provider") User provider);
}
