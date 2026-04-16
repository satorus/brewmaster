package com.brewmaster.calendar;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<BrewEventParticipant> participants = new ArrayList<>();

    protected BrewEvent() {}

    public BrewEvent(String title, String description, LocalDate brewDate, LocalTime startTime,
                     String location, UUID recipeId, UUID createdBy) {
        this.title = title;
        this.description = description;
        this.brewDate = brewDate;
        this.startTime = startTime;
        this.location = location;
        this.recipeId = recipeId;
        this.createdBy = createdBy;
    }

    @PreUpdate
    void onUpdate() { this.updatedAt = Instant.now(); }

    public void update(String title, String description, LocalDate brewDate, LocalTime startTime,
                       String location, UUID recipeId) {
        this.title = title;
        this.description = description;
        this.brewDate = brewDate;
        this.startTime = startTime;
        this.location = location;
        this.recipeId = recipeId;
    }

    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public LocalDate getBrewDate() { return brewDate; }
    public LocalTime getStartTime() { return startTime; }
    public String getLocation() { return location; }
    public UUID getRecipeId() { return recipeId; }
    public UUID getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<BrewEventParticipant> getParticipants() { return participants; }
}
