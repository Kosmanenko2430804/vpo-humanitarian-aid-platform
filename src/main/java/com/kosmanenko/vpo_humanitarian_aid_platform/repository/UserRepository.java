package com.kosmanenko.vpo_humanitarian_aid_platform.repository;

import com.kosmanenko.vpo_humanitarian_aid_platform.enums.ProviderType;
import com.kosmanenko.vpo_humanitarian_aid_platform.enums.UserRole;
import com.kosmanenko.vpo_humanitarian_aid_platform.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<User> findByOauthProviderAndOauthId(String oauthProvider, String oauthId);

    @Query("SELECT u FROM User u WHERE u.role = :role AND u.isProfilePublic = true AND u.isBlocked = false")
    List<User> findPublicProviders(@Param("role") UserRole role);

    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.providerCategories" +
           " WHERE u.role = 'PROVIDER' AND u.isProfilePublic = true AND u.isBlocked = false" +
           " AND (:city IS NULL OR u.city = :city)" +
           " AND (:providerType IS NULL OR u.providerType = :providerType)")
    List<User> findPublicProvidersByFilters(@Param("city") String city,
                                            @Param("providerType") ProviderType providerType);

    List<User> findByRoleAndIsBlockedFalse(UserRole role);
}
