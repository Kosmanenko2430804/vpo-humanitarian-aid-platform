package com.kosmanenko.vpo_humanitarian_aid_platform.service;

import com.kosmanenko.vpo_humanitarian_aid_platform.enums.ProviderType;
import com.kosmanenko.vpo_humanitarian_aid_platform.enums.UserRole;
import com.kosmanenko.vpo_humanitarian_aid_platform.model.User;
import com.kosmanenko.vpo_humanitarian_aid_platform.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CatalogService {

    private final UserRepository userRepository;

    public List<User> getPublicProviders(String city, ProviderType providerType, Long categoryId) {
        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("role"), UserRole.PROVIDER));
            predicates.add(cb.isTrue(root.get("isProfilePublic")));
            predicates.add(cb.isFalse(root.get("isBlocked")));
            if (city != null && !city.isBlank()) {
                predicates.add(cb.equal(root.get("city"), city));
            }
            if (providerType != null) {
                predicates.add(cb.equal(root.get("providerType"), providerType));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        List<User> providers = userRepository.findAll(spec);
        if (categoryId != null) {
            providers = providers.stream()
                .filter(u -> u.getProviderCategories().stream().anyMatch(c -> c.getId().equals(categoryId)))
                .collect(Collectors.toList());
        }
        return providers;
    }
}
