package com.kosmanenko.vpo_humanitarian_aid_platform.repository;

import com.kosmanenko.vpo_humanitarian_aid_platform.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByName(String name);
    boolean existsByName(String name);
}
