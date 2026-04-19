package com.brewmaster.brew;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface BrewSessionRepository extends JpaRepository<BrewSession, UUID> {

    @Query("SELECT s FROM BrewSession s LEFT JOIN FETCH s.stepLogs WHERE s.id = :id")
    Optional<BrewSession> findByIdWithStepLogs(@Param("id") UUID id);
}
