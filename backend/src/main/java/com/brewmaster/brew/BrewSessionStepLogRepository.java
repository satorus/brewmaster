package com.brewmaster.brew;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BrewSessionStepLogRepository extends JpaRepository<BrewSessionStepLog, UUID> {

    boolean existsBySessionIdAndStepNumber(UUID sessionId, int stepNumber);

    List<BrewSessionStepLog> findBySessionIdOrderByStepNumberAsc(UUID sessionId);
}
