package com.kosmanenko.vpo_humanitarian_aid_platform.service;

import com.kosmanenko.vpo_humanitarian_aid_platform.enums.AnnouncementStatus;
import com.kosmanenko.vpo_humanitarian_aid_platform.model.Announcement;
import com.kosmanenko.vpo_humanitarian_aid_platform.model.Complaint;
import com.kosmanenko.vpo_humanitarian_aid_platform.repository.AnnouncementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ModerationService {

    private final AnnouncementRepository announcementRepository;
    private final ComplaintService complaintService;
    private final NotificationService notificationService;

    @Transactional
    public void blockAnnouncementFromComplaint(Long complaintId) {
        Complaint complaint = complaintService.findById(complaintId).orElseThrow();
        Announcement announcement = complaint.getAnnouncement();
        announcement.setStatus(AnnouncementStatus.ARCHIVED);
        announcementRepository.save(announcement);
        complaintService.markReviewed(complaintId);
        notificationService.notify(announcement.getAuthor(),
            "Ваше оголошення \"" + announcement.getTitle() + "\" заблоковано за скаргою.");
    }

    @Transactional
    public void dismissComplaint(Long complaintId) {
        complaintService.markReviewed(complaintId);
    }
}
