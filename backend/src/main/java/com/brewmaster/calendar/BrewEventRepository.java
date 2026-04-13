package com.brewmaster.calendar;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

// TODO: add custom queries for calendar view
public interface BrewEventRepository extends JpaRepository<BrewEvent, UUID> {}
