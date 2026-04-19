package com.brewmaster.recipe;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

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

    public RecipeIngredient(Recipe recipe, String name, String category, BigDecimal amount,
                            String unit, String additionTime, String notes, int sortOrder) {
        this.recipe = recipe;
        this.name = name;
        this.category = category;
        this.amount = amount;
        this.unit = unit;
        this.additionTime = additionTime;
        this.notes = notes;
        this.sortOrder = sortOrder;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public BigDecimal getAmount() { return amount; }
    public String getUnit() { return unit; }
    public String getAdditionTime() { return additionTime; }
    public String getNotes() { return notes; }
    public int getSortOrder() { return sortOrder; }
}
