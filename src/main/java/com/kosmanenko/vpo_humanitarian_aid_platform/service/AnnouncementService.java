package com.kosmanenko.vpo_humanitarian_aid_platform.service;

import com.kosmanenko.vpo_humanitarian_aid_platform.enums.AnnouncementStatus;
import com.kosmanenko.vpo_humanitarian_aid_platform.enums.AnnouncementType;
import com.kosmanenko.vpo_humanitarian_aid_platform.enums.ApplicationStatus;
import com.kosmanenko.vpo_humanitarian_aid_platform.event.AnnouncementApprovedEvent;
import com.kosmanenko.vpo_humanitarian_aid_platform.event.AnnouncementRejectedEvent;
import com.kosmanenko.vpo_humanitarian_aid_platform.event.AnnouncementSubmittedEvent;
import com.kosmanenko.vpo_humanitarian_aid_platform.model.Announcement;
import com.kosmanenko.vpo_humanitarian_aid_platform.model.Category;
import com.kosmanenko.vpo_humanitarian_aid_platform.model.User;
import com.kosmanenko.vpo_humanitarian_aid_platform.repository.AnnouncementRepository;
import com.kosmanenko.vpo_humanitarian_aid_platform.repository.CategoryRepository;
import com.kosmanenko.vpo_humanitarian_aid_platform.repository.HelpApplicationRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final CategoryRepository categoryRepository;
    private final HelpApplicationRepository helpApplicationRepository;
    private final ApplicationEventPublisher eventPublisher;

    public Optional<Announcement> findById(Long id) {
        return announcementRepository.findById(id);
    }

    public List<Announcement> findByAuthor(User author) {
        return announcementRepository.findByAuthor(author);
    }

    public List<Announcement> findPending() {
        return announcementRepository.findByStatusOrderByCreatedAtDesc(AnnouncementStatus.PENDING);
    }

    public List<Announcement> findTop6Offers() {
        return announcementRepository.findTop6ByStatusAndTypeOrderByCreatedAtDesc(
            AnnouncementStatus.PUBLISHED, AnnouncementType.OFFER);
    }

    public List<Announcement> findTop6Requests() {
        return announcementRepository.findTop6ByStatusAndTypeOrderByCreatedAtDesc(
            AnnouncementStatus.PUBLISHED, AnnouncementType.REQUEST);
    }

    public Page<Announcement> search(AnnouncementType type, String city, Long categoryId, String keyword, int page) {
        PageRequest pageable = PageRequest.of(page, 12);
        String cityLike = (city != null && !city.isBlank()) ? "%" + city.toLowerCase() + "%" : null;
        String keywordLike = (keyword != null && !keyword.isBlank()) ? "%" + keyword.toLowerCase() + "%" : null;

        Specification<Announcement> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), AnnouncementStatus.PUBLISHED));
            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            if (cityLike != null) {
                predicates.add(cb.like(cb.lower(root.get("city")), cityLike));
            }
            if (keywordLike != null) {
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("title")), keywordLike),
                    cb.like(cb.lower(root.get("description")), keywordLike)
                ));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.join("categories").get("id"), categoryId));
                query.distinct(true);
            }
            query.orderBy(cb.desc(root.get("createdAt")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return announcementRepository.findAll(spec, pageable);
    }

    @Transactional
    public Announcement create(String title, String description, String city,
                               AnnouncementType type, Boolean acceptsApplications,
                               List<Long> categoryIds, User author, String donationUrl) {
        Set<Category> categories = categoryIds.stream()
            .map(id -> categoryRepository.findById(id).orElseThrow())
            .collect(Collectors.toSet());

        Announcement announcement = Announcement.builder()
            .title(title)
            .description(description)
            .city(city)
            .type(type)
            .status(AnnouncementStatus.PENDING)
            .acceptsApplications(acceptsApplications != null ? acceptsApplications : true)
            .author(author)
            .categories(categories)
            .donationUrl(donationUrl != null && !donationUrl.isBlank() ? donationUrl : null)
            .build();

        Announcement saved = announcementRepository.save(announcement);
        eventPublisher.publishEvent(new AnnouncementSubmittedEvent(saved,
            "Ваше оголошення \"" + title + "\" надіслано на модерацію."));
        return saved;
    }

    @Transactional
    public Announcement update(Long id, String title, String description, String city,
                               Boolean acceptsApplications, List<Long> categoryIds, User currentUser, String donationUrl) {
        Announcement announcement = announcementRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Оголошення не знайдено"));

        if (!announcement.getAuthor().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Немає доступу");
        }
        if (announcement.getStatus() != AnnouncementStatus.PENDING
                && announcement.getStatus() != AnnouncementStatus.REJECTED) {
            throw new RuntimeException("Редагувати можна лише оголошення в статусі 'На модерації' або 'Відхилено'");
        }

        Set<Category> categories = categoryIds.stream()
            .map(catId -> categoryRepository.findById(catId).orElseThrow())
            .collect(Collectors.toSet());

        announcement.setTitle(title);
        announcement.setDescription(description);
        announcement.setCity(city);
        announcement.setAcceptsApplications(acceptsApplications != null ? acceptsApplications : true);
        announcement.setCategories(categories);
        announcement.setDonationUrl(donationUrl != null && !donationUrl.isBlank() ? donationUrl : null);
        announcement.setStatus(AnnouncementStatus.PENDING);
        announcement.setRejectionReason(null);

        Announcement saved = announcementRepository.save(announcement);
        eventPublisher.publishEvent(new AnnouncementSubmittedEvent(saved,
            "Ваше оголошення \"" + title + "\" оновлено та надіслано на повторну модерацію."));
        return saved;
    }

    @Transactional
    public void approve(Long id) {
        Announcement a = announcementRepository.findById(id).orElseThrow();
        a.setStatus(AnnouncementStatus.PUBLISHED);
        a.setPublishedAt(LocalDateTime.now());
        announcementRepository.save(a);
        eventPublisher.publishEvent(new AnnouncementApprovedEvent(a));
    }

    @Transactional
    public void reject(Long id, String reason) {
        Announcement a = announcementRepository.findById(id).orElseThrow();
        a.setStatus(AnnouncementStatus.REJECTED);
        a.setRejectionReason(reason);
        announcementRepository.save(a);
        eventPublisher.publishEvent(new AnnouncementRejectedEvent(a, reason));
    }

    @Transactional
    public void archive(Announcement announcement) {
        announcement.setStatus(AnnouncementStatus.ARCHIVED);
        announcement.setArchivedAt(LocalDateTime.now());
        announcementRepository.save(announcement);
    }

    @Transactional
    public void complete(Long id, User currentUser) {
        Announcement a = announcementRepository.findById(id).orElseThrow();
        if (!a.getAuthor().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Немає доступу");
        }
        if (a.getStatus() != AnnouncementStatus.PUBLISHED) {
            throw new RuntimeException("Закрити можна лише опубліковане оголошення");
        }
        boolean hasActive = helpApplicationRepository.existsByAnnouncementAndStatusIn(
            a, List.of(ApplicationStatus.PENDING, ApplicationStatus.ACCEPTED));
        if (hasActive) {
            throw new RuntimeException("Неможливо закрити оголошення з активними заявками");
        }
        a.setStatus(AnnouncementStatus.COMPLETED);
        announcementRepository.save(a);
    }
}
