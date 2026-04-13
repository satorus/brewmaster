package com.brewmaster.brew;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

// TODO: implement in Brew Mode feature milestone
@Entity
@Table(name = "brew_sessions")
public class BrewSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "recipe_id")
    private UUID recipeId;

    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "volume_l", nullable = false, precision = 6, scale = 2)
    private BigDecimal volumeL;

    @Column(name = "current_step", nullable = false)
    private int currentStep = 0;

    @Column(nullable = false, length = 30)
    private String status = "IN_PROGRESS";

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "started_by", nullable = false)
    private UUID startedBy;

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;

    protected BrewSession() {}

    public UUID getId() { return id; }
    public String getStatus() { return status; }
    public int getCurrentStep() { return currentStep; }
}
