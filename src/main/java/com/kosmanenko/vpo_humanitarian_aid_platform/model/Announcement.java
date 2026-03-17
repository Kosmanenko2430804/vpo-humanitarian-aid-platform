package com.kosmanenko.vpo_humanitarian_aid_platform.model;

import com.kosmanenko.vpo_humanitarian_aid_platform.enums.AnnouncementStatus;
import com.kosmanenko.vpo_humanitarian_aid_platform.enums.AnnouncementType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "announcements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnnouncementType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnnouncementStatus status;

    @Column(nullable = false)
    private String city;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "accepts_applications")
    private Boolean acceptsApplications = true;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @BatchSize(size = 30)
    @JoinTable(
        name = "announcement_categories",
        joinColumns = @JoinColumn(name = "announcement_id"),
        inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<Category> categories = new HashSet<>();

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
