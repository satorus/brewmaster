package com.brewmaster.brew;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "scaled_ingredients_json", columnDefinition = "jsonb")
    private String scaledIngredientsJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "scaled_steps_json", columnDefinition = "jsonb")
    private String scaledStepsJson;

    @Column(name = "boil_off_rate_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal boilOffRatePercent = BigDecimal.TEN;

    @Column(name = "water_to_grain_ratio", nullable = false, precision = 4, scale = 2)
    private BigDecimal waterToGrainRatio = new BigDecimal("3.0");

    @Column(name = "strike_water_l", precision = 8, scale = 2)
    private BigDecimal strikeWaterL;

    @Column(name = "sparge_volume_l", precision = 8, scale = 2)
    private BigDecimal spargeVolumeL;

    @Column(name = "pre_boil_volume_l", precision = 8, scale = 2)
    private BigDecimal preBoilVolumeL;

    @OneToMany(mappedBy = "sessionId", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("stepNumber ASC")
    private List<BrewSessionStepLog> stepLogs = new ArrayList<>();

    protected BrewSession() {}

    public BrewSession(UUID recipeId, UUID eventId, BigDecimal volumeL, UUID startedBy,
                       String scaledIngredientsJson, String scaledStepsJson,
                       BigDecimal boilOffRatePercent, BigDecimal waterToGrainRatio,
                       BigDecimal strikeWaterL, BigDecimal spargeVolumeL, BigDecimal preBoilVolumeL,
                       String notes) {
        this.recipeId = recipeId;
        this.eventId = eventId;
        this.volumeL = volumeL;
        this.startedBy = startedBy;
        this.scaledIngredientsJson = scaledIngredientsJson;
        this.scaledStepsJson = scaledStepsJson;
        this.boilOffRatePercent = boilOffRatePercent;
        this.waterToGrainRatio = waterToGrainRatio;
        this.strikeWaterL = strikeWaterL;
        this.spargeVolumeL = spargeVolumeL;
        this.preBoilVolumeL = preBoilVolumeL;
        this.notes = notes;
    }

    public UUID getId() { return id; }
    public UUID getRecipeId() { return recipeId; }
    public UUID getEventId() { return eventId; }
    public BigDecimal getVolumeL() { return volumeL; }
    public int getCurrentStep() { return currentStep; }
    public String getStatus() { return status; }
    public String getNotes() { return notes; }
    public UUID getStartedBy() { return startedBy; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public String getScaledIngredientsJson() { return scaledIngredientsJson; }
    public String getScaledStepsJson() { return scaledStepsJson; }
    public BigDecimal getBoilOffRatePercent() { return boilOffRatePercent; }
    public BigDecimal getWaterToGrainRatio() { return waterToGrainRatio; }
    public BigDecimal getStrikeWaterL() { return strikeWaterL; }
    public BigDecimal getSpargeVolumeL() { return spargeVolumeL; }
    public BigDecimal getPreBoilVolumeL() { return preBoilVolumeL; }
    public List<BrewSessionStepLog> getStepLogs() { return stepLogs; }

    public void setCurrentStep(int currentStep) { this.currentStep = currentStep; }
    public void setStatus(String status) { this.status = status; }
    public void setNotes(String notes) { this.notes = notes; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
