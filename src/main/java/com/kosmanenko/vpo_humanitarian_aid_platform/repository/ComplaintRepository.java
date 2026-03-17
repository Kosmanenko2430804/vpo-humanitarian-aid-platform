package com.kosmanenko.vpo_humanitarian_aid_platform.repository;

import com.kosmanenko.vpo_humanitarian_aid_platform.model.Announcement;
import com.kosmanenko.vpo_humanitarian_aid_platform.model.Complaint;
import com.kosmanenko.vpo_humanitarian_aid_platform.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    List<Complaint> findByStatusOrderByCreatedAtDesc(String status);
    List<Complaint> findAllByOrderByCreatedAtDesc();
    boolean existsByAnnouncementAndComplainant(Announcement announcement, User complainant);
}
