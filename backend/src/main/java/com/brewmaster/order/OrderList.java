package com.brewmaster.order;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

// TODO: implement in Order feature milestone
@Entity
@Table(name = "order_lists")
public class OrderList {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "recipe_id", nullable = false)
    private UUID recipeId;

    @Column(name = "volume_l", nullable = false, precision = 6, scale = 2)
    private BigDecimal volumeL;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ai_result", columnDefinition = "jsonb")
    private Map<String, Object> aiResult;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected OrderList() {}

    public UUID getId() { return id; }
    public UUID getRecipeId() { return recipeId; }
    public Map<String, Object> getAiResult() { return aiResult; }
}
