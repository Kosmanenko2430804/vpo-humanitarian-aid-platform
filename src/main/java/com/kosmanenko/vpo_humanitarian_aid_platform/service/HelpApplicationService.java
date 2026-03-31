package com.kosmanenko.vpo_humanitarian_aid_platform.service;

import com.kosmanenko.vpo_humanitarian_aid_platform.enums.AnnouncementStatus;
import com.kosmanenko.vpo_humanitarian_aid_platform.enums.ApplicationStatus;
import com.kosmanenko.vpo_humanitarian_aid_platform.event.HelpApplicationAcceptedEvent;
import com.kosmanenko.vpo_humanitarian_aid_platform.event.HelpApplicationCompletedEvent;
import com.kosmanenko.vpo_humanitarian_aid_platform.event.HelpApplicationReceivedEvent;
import com.kosmanenko.vpo_humanitarian_aid_platform.event.HelpApplicationRejectedEvent;
import com.kosmanenko.vpo_humanitarian_aid_platform.model.Announcement;
import com.kosmanenko.vpo_humanitarian_aid_platform.model.HelpApplication;
import com.kosmanenko.vpo_humanitarian_aid_platform.model.User;
import com.kosmanenko.vpo_humanitarian_aid_platform.repository.HelpApplicationRepository;
import com.kosmanenko.vpo_humanitarian_aid_platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class HelpApplicationService {

    private final HelpApplicationRepository helpApplicationRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final UserRepository userRepository;

    public Optional<HelpApplication> findById(Long id) {
        return helpApplicationRepository.findById(id);
    }

    public List<HelpApplication> findByApplicant(User applicant) {
        return helpApplicationRepository.findByApplicantOrderByCreatedAtDesc(applicant);
    }

    public List<HelpApplication> findByAnnouncement(Announcement announcement) {
        return helpApplicationRepository.findByAnnouncement(announcement);
    }

    public boolean alreadyApplied(Announcement announcement, User user) {
        return helpApplicationRepository.existsByAnnouncementAndApplicant(announcement, user);
    }

    @Transactional
    public HelpApplication apply(Announcement announcement, User applicant, String message) {
        if (announcement.getStatus() != AnnouncementStatus.PUBLISHED) {
            throw new RuntimeException("Оголошення недоступне для подачі заявок");
        }
        if (!Boolean.TRUE.equals(announcement.getAcceptsApplications())) {
            throw new RuntimeException("Це оголошення не приймає заявки");
        }
        if (helpApplicationRepository.existsByAnnouncementAndApplicant(announcement, applicant)) {
            throw new RuntimeException("Ви вже подали заявку на це оголошення");
        }

        HelpApplication application = HelpApplication.builder()
            .announcement(announcement)
            .applicant(applicant)
            .message(message)
            .status(ApplicationStatus.PENDING)
            .build();

        HelpApplication saved = helpApplicationRepository.save(application);
        eventPublisher.publishEvent(new HelpApplicationReceivedEvent(saved));
        return saved;
    }

    @Transactional
    public void accept(Long applicationId, LocalDateTime pickupDate, String pickupLocation,
                       String providerPhone, User provider) {
        HelpApplication app = helpApplicationRepository.findById(applicationId).orElseThrow();
        validateProviderAccess(app, provider);
        if (app.getStatus() != ApplicationStatus.PENDING) {
            throw new RuntimeException("Можна прийняти лише заявку зі статусом 'Очікує'");
        }

        app.setStatus(ApplicationStatus.ACCEPTED);
        app.setPickupDate(pickupDate);
        app.setPickupLocation(pickupLocation);
        app.setProviderPhone(providerPhone);
        helpApplicationRepository.save(app);
        eventPublisher.publishEvent(new HelpApplicationAcceptedEvent(app));
    }

    @Transactional
    public void reject(Long applicationId, String reason, User provider) {
        HelpApplication app = helpApplicationRepository.findById(applicationId).orElseThrow();
        validateProviderAccess(app, provider);
        if (app.getStatus() != ApplicationStatus.PENDING) {
            throw new RuntimeException("Можна відхилити лише заявку зі статусом 'Очікує'");
        }

        app.setStatus(ApplicationStatus.REJECTED);
        app.setRejectionReason(reason);
        helpApplicationRepository.save(app);
        eventPublisher.publishEvent(new HelpApplicationRejectedEvent(app, reason));
    }

    @Transactional
    public void complete(Long applicationId, User provider) {
        HelpApplication app = helpApplicationRepository.findById(applicationId).orElseThrow();
        validateProviderAccess(app, provider);
        if (app.getStatus() != ApplicationStatus.ACCEPTED) {
            throw new RuntimeException("Можна завершити лише прийняту заявку");
        }

        app.setStatus(ApplicationStatus.COMPLETED);
        helpApplicationRepository.save(app);
        eventPublisher.publishEvent(new HelpApplicationCompletedEvent(app));
    }

    @Transactional
    public void leaveReview(Long applicationId, int rating, String review, User applicant) {
        HelpApplication app = helpApplicationRepository.findById(applicationId).orElseThrow();
        if (!app.getApplicant().getId().equals(applicant.getId())) {
            throw new RuntimeException("Немає доступу");
        }
        if (app.getStatus() != ApplicationStatus.COMPLETED) {
            throw new RuntimeException("Можна залишити відгук тільки для завершених заявок");
        }
        if (rating < 1 || rating > 5) {
            throw new RuntimeException("Рейтинг має бути від 1 до 5");
        }
        if (app.getRating() != null) {
            throw new RuntimeException("Відгук вже залишено");
        }

        app.setRating(rating);
        app.setReview(review);
        helpApplicationRepository.save(app);

        User provider = app.getAnnouncement().getAuthor();
        updateProviderRating(provider);
    }

    private void updateProviderRating(User provider) {
        List<HelpApplication> allApps = helpApplicationRepository.findRatedApplicationsByProvider(provider);
        if (!allApps.isEmpty()) {
            double avg = allApps.stream().mapToInt(HelpApplication::getRating).average().orElse(0);
            provider.setRating(BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
            provider.setRatingCount(allApps.size());
            userRepository.save(provider);
        }
    }

    private void validateProviderAccess(HelpApplication app, User provider) {
        if (!app.getAnnouncement().getAuthor().getId().equals(provider.getId())) {
            throw new RuntimeException("Немає доступу");
        }
    }
}
