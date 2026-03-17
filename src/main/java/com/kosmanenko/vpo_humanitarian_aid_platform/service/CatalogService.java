package com.kosmanenko.vpo_humanitarian_aid_platform.service;

import com.kosmanenko.vpo_humanitarian_aid_platform.enums.ProviderType;
import com.kosmanenko.vpo_humanitarian_aid_platform.enums.UserRole;
import com.kosmanenko.vpo_humanitarian_aid_platform.model.User;
import com.kosmanenko.vpo_humanitarian_aid_platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CatalogService {

    private final UserRepository userRepository;

    public List<User> getPublicProviders(String city, ProviderType providerType, Long categoryId) {
        List<User> providers = userRepository.findPublicProvidersByFilters(city, providerType);
        if (categoryId != null) {
            providers = providers.stream()
                .filter(u -> u.getProviderCategories().stream().anyMatch(c -> c.getId().equals(categoryId)))
                .collect(Collectors.toList());
        }
        return providers;
    }
}
