package com.brewmaster.recipe;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

// TODO: implement in Recipe feature milestone
@Entity
@Table(name = "recipe_steps")
public class RecipeStep {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    @Column(name = "step_number", nullable = false)
    private int stepNumber;

    @Column(nullable = false, length = 50)
    private String phase;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String instructions;

    @Column(name = "duration_min")
    private Integer durationMin;

    @Column(name = "target_temp_c", precision = 4, scale = 1)
    private BigDecimal targetTempC;

    @Column(name = "timer_required", nullable = false)
    private boolean timerRequired = false;

    @Column(columnDefinition = "TEXT")
    private String notes;

    protected RecipeStep() {}

    public UUID getId() { return id; }
    public int getStepNumber() { return stepNumber; }
    public String getPhase() { return phase; }
    public String getTitle() { return title; }
    public String getInstructions() { return instructions; }
}
