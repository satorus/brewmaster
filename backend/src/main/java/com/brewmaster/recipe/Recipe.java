package com.brewmaster.recipe;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// TODO: implement in Recipe feature milestone
@Entity
@Table(name = "recipes")
public class Recipe {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 100)
    private String style;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "source_url", columnDefinition = "TEXT")
    private String sourceUrl;

    @Column(name = "base_volume_l", nullable = false, precision = 6, scale = 2)
    private BigDecimal baseVolumeL = BigDecimal.valueOf(20);

    @Column(name = "original_gravity", precision = 5, scale = 4)
    private BigDecimal originalGravity;

    @Column(name = "final_gravity", precision = 5, scale = 4)
    private BigDecimal finalGravity;

    @Column(precision = 4, scale = 2)
    private BigDecimal abv;

    private Integer ibu;

    @Column(precision = 5, scale = 2)
    private BigDecimal srm;

    @Column(name = "mash_temp_c", precision = 4, scale = 1)
    private BigDecimal mashTempC;

    @Column(name = "mash_duration_min")
    private Integer mashDurationMin;

    @Column(name = "boil_duration_min")
    private Integer boilDurationMin;

    @Column(name = "fermentation_temp_c", precision = 4, scale = 1)
    private BigDecimal fermentationTempC;

    @Column(name = "fermentation_days")
    private Integer fermentationDays;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "is_ai_generated", nullable = false)
    private boolean isAiGenerated = false;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<RecipeIngredient> ingredients = new ArrayList<>();

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stepNumber ASC")
    private List<RecipeStep> steps = new ArrayList<>();

    protected Recipe() {}

    public UUID getId() { return id; }
    public String getName() { return name; }
    public List<RecipeIngredient> getIngredients() { return ingredients; }
    public List<RecipeStep> getSteps() { return steps; }
}
