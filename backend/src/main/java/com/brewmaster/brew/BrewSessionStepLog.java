package com.brewmaster.brew;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "brew_session_step_log")
public class BrewSessionStepLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "step_number", nullable = false)
    private int stepNumber;

    @Column(name = "completed_at", nullable = false)
    private Instant completedAt = Instant.now();

    @Column(name = "actual_temp_c", precision = 4, scale = 1)
    private BigDecimal actualTempC;

    @Column(columnDefinition = "TEXT")
    private String notes;

    protected BrewSessionStepLog() {}

    public BrewSessionStepLog(UUID sessionId, int stepNumber, BigDecimal actualTempC, String notes) {
        this.sessionId = sessionId;
        this.stepNumber = stepNumber;
        this.actualTempC = actualTempC;
        this.notes = notes;
    }

    public UUID getId() { return id; }
    public UUID getSessionId() { return sessionId; }
    public int getStepNumber() { return stepNumber; }
    public Instant getCompletedAt() { return completedAt; }
    public BigDecimal getActualTempC() { return actualTempC; }
    public String getNotes() { return notes; }
}
