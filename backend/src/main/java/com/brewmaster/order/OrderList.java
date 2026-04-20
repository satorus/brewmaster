package com.brewmaster.order;

import com.brewmaster.recipe.Recipe;
import com.brewmaster.user.User;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "order_lists")
public class OrderList {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    @Column(name = "volume_l", nullable = false, precision = 6, scale = 2)
    private BigDecimal volumeL;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ai_result", columnDefinition = "jsonb")
    private String aiResult;

    @Column(name = "estimated_total_min", precision = 10, scale = 2)
    private BigDecimal estimatedTotalMin;

    @Column(name = "estimated_total_max", precision = 10, scale = 2)
    private BigDecimal estimatedTotalMax;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected OrderList() {}

    public OrderList(Recipe recipe, BigDecimal volumeL, String aiResult,
                     BigDecimal estimatedTotalMin, BigDecimal estimatedTotalMax, User createdBy) {
        this.recipe = recipe;
        this.volumeL = volumeL;
        this.aiResult = aiResult;
        this.estimatedTotalMin = estimatedTotalMin;
        this.estimatedTotalMax = estimatedTotalMax;
        this.createdBy = createdBy;
    }

    public UUID getId() { return id; }
    public Recipe getRecipe() { return recipe; }
    public BigDecimal getVolumeL() { return volumeL; }
    public String getAiResult() { return aiResult; }
    public BigDecimal getEstimatedTotalMin() { return estimatedTotalMin; }
    public BigDecimal getEstimatedTotalMax() { return estimatedTotalMax; }
    public User getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
}
