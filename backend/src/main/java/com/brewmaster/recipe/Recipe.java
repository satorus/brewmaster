package com.brewmaster.recipe;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
    private BigDecimal baseVolumeL;

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
    private boolean aiGenerated = false;

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

    public Recipe(String name, String style, String description, String sourceUrl,
                  BigDecimal baseVolumeL, BigDecimal originalGravity, BigDecimal finalGravity,
                  BigDecimal abv, Integer ibu, BigDecimal srm, BigDecimal mashTempC,
                  Integer mashDurationMin, Integer boilDurationMin, BigDecimal fermentationTempC,
                  Integer fermentationDays, String notes, boolean aiGenerated, UUID createdBy) {
        this.name = name;
        this.style = style;
        this.description = description;
        this.sourceUrl = sourceUrl;
        this.baseVolumeL = baseVolumeL;
        this.originalGravity = originalGravity;
        this.finalGravity = finalGravity;
        this.abv = abv;
        this.ibu = ibu;
        this.srm = srm;
        this.mashTempC = mashTempC;
        this.mashDurationMin = mashDurationMin;
        this.boilDurationMin = boilDurationMin;
        this.fermentationTempC = fermentationTempC;
        this.fermentationDays = fermentationDays;
        this.notes = notes;
        this.aiGenerated = aiGenerated;
        this.createdBy = createdBy;
    }

    @PreUpdate
    void onUpdate() { this.updatedAt = Instant.now(); }

    public void update(String name, String style, String description, String sourceUrl,
                       BigDecimal baseVolumeL, BigDecimal originalGravity, BigDecimal finalGravity,
                       BigDecimal abv, Integer ibu, BigDecimal srm, BigDecimal mashTempC,
                       Integer mashDurationMin, Integer boilDurationMin,
                       BigDecimal fermentationTempC, Integer fermentationDays, String notes) {
        this.name = name;
        this.style = style;
        this.description = description;
        this.sourceUrl = sourceUrl;
        this.baseVolumeL = baseVolumeL;
        this.originalGravity = originalGravity;
        this.finalGravity = finalGravity;
        this.abv = abv;
        this.ibu = ibu;
        this.srm = srm;
        this.mashTempC = mashTempC;
        this.mashDurationMin = mashDurationMin;
        this.boilDurationMin = boilDurationMin;
        this.fermentationTempC = fermentationTempC;
        this.fermentationDays = fermentationDays;
        this.notes = notes;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getStyle() { return style; }
    public String getDescription() { return description; }
    public String getSourceUrl() { return sourceUrl; }
    public BigDecimal getBaseVolumeL() { return baseVolumeL; }
    public BigDecimal getOriginalGravity() { return originalGravity; }
    public BigDecimal getFinalGravity() { return finalGravity; }
    public BigDecimal getAbv() { return abv; }
    public Integer getIbu() { return ibu; }
    public BigDecimal getSrm() { return srm; }
    public BigDecimal getMashTempC() { return mashTempC; }
    public Integer getMashDurationMin() { return mashDurationMin; }
    public Integer getBoilDurationMin() { return boilDurationMin; }
    public BigDecimal getFermentationTempC() { return fermentationTempC; }
    public Integer getFermentationDays() { return fermentationDays; }
    public String getNotes() { return notes; }
    public boolean isAiGenerated() { return aiGenerated; }
    public UUID getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<RecipeIngredient> getIngredients() { return ingredients; }
    public List<RecipeStep> getSteps() { return steps; }
}
