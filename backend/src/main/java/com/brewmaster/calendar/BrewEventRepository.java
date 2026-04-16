package com.brewmaster.calendar;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BrewEventRepository extends JpaRepository<BrewEvent, UUID> {

    @Query("SELECT DISTINCT e FROM BrewEvent e LEFT JOIN FETCH e.participants p LEFT JOIN FETCH p.user " +
           "WHERE e.brewDate BETWEEN :start AND :end ORDER BY e.brewDate ASC")
    List<BrewEvent> findByMonthWithParticipants(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT e FROM BrewEvent e LEFT JOIN FETCH e.participants p LEFT JOIN FETCH p.user WHERE e.id = :id")
    Optional<BrewEvent> findByIdWithParticipants(@Param("id") UUID id);
}
