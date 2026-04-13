package com.brewmaster.calendar;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

// TODO: implement in Calendar feature milestone
@Entity
@Table(name = "brew_events")
public class BrewEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "brew_date", nullable = false)
    private LocalDate brewDate;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(length = 200)
    private String location;

    @Column(name = "recipe_id")
    private UUID recipeId;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected BrewEvent() {}

    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public LocalDate getBrewDate() { return brewDate; }
    public UUID getCreatedBy() { return createdBy; }
}
