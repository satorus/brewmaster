package com.brewmaster.brew;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

// TODO: add custom queries for brew session feature
public interface BrewSessionRepository extends JpaRepository<BrewSession, UUID> {}
