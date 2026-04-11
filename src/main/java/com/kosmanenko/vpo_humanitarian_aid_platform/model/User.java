package com.kosmanenko.vpo_humanitarian_aid_platform.model;

import com.kosmanenko.vpo_humanitarian_aid_platform.enums.ProviderType;
import com.kosmanenko.vpo_humanitarian_aid_platform.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    private String phone;
    private String city;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_type")
    private ProviderType providerType;

    @Column(name = "org_name")
    private String orgName;

    @Column(name = "org_description", columnDefinition = "TEXT")
    private String orgDescription;

    @Column(name = "is_profile_public")
    private Boolean isProfilePublic = false;

    @Column(name = "is_blocked")
    private Boolean isBlocked = false;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "provider_categories",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<Category> providerCategories = new HashSet<>();

}
