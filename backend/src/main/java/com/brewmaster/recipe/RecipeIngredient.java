package com.brewmaster.recipe;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

// TODO: implement in Recipe feature milestone
@Entity
@Table(name = "recipe_ingredients")
public class RecipeIngredient {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal amount;

    @Column(nullable = false, length = 20)
    private String unit;

    @Column(name = "addition_time", length = 100)
    private String additionTime;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    protected RecipeIngredient() {}

    public UUID getId() { return id; }
    public String getName() { return name; }
    public BigDecimal getAmount() { return amount; }
    public String getUnit() { return unit; }
    public String getCategory() { return category; }
}
