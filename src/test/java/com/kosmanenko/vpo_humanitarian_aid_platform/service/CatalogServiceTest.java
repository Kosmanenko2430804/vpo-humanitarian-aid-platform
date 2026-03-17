package com.kosmanenko.vpo_humanitarian_aid_platform.service;

import com.kosmanenko.vpo_humanitarian_aid_platform.model.Category;
import com.kosmanenko.vpo_humanitarian_aid_platform.model.User;
import com.kosmanenko.vpo_humanitarian_aid_platform.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CatalogService catalogService;

    @Test
    @DisplayName("getPublicProviders без categoryId — повертає всіх провайдерів")
    void getPublicProviders_withoutCategoryFilter_returnsAll() {
        List<User> providers = List.of(User.builder().id(1L).build(), User.builder().id(2L).build());
        when(userRepository.findPublicProvidersByFilters(null, null)).thenReturn(providers);

        List<User> result = catalogService.getPublicProviders(null, null, null);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("getPublicProviders з categoryId — фільтрує по категорії")
    void getPublicProviders_withCategoryFilter_filtersResults() {
        Category cat1 = Category.builder().id(1L).build();
        Category cat2 = Category.builder().id(2L).build();
        User p1 = User.builder().id(1L).providerCategories(Set.of(cat1)).build();
        User p2 = User.builder().id(2L).providerCategories(Set.of(cat2)).build();
        when(userRepository.findPublicProvidersByFilters(null, null))
            .thenReturn(new ArrayList<>(List.of(p1, p2)));

        List<User> result = catalogService.getPublicProviders(null, null, 1L);

        assertThat(result).containsExactly(p1);
    }
}
