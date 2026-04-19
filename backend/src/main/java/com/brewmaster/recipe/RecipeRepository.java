package com.brewmaster.recipe;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface RecipeRepository extends JpaRepository<Recipe, UUID> {

    Page<Recipe> findByCreatedByOrderByCreatedAtDesc(UUID createdBy, Pageable pageable);

    @Query("SELECT DISTINCT r FROM Recipe r LEFT JOIN FETCH r.ingredients WHERE r.id = :id")
    Optional<Recipe> findByIdWithDetails(@Param("id") UUID id);
}
