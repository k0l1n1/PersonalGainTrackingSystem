package com.personaltracker.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Model representing a logged meal or food item.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class NutritionEntry {
    private String id;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("food_name")
    private String foodName = "";

    @JsonProperty("meal_type")
    private String mealType = "LUNCH"; // BREAKFAST, LUNCH, DINNER, SNACK

    @JsonProperty("serving_size")
    private String servingSize = "1 serving";

    private int calories = 0;

    @JsonProperty("protein_g")
    private double proteinG = 0.0;

    @JsonProperty("carbs_g")
    private double carbsG = 0.0;

    @JsonProperty("fat_g")
    private double fatG = 0.0;

    @JsonProperty("entry_date")
    private LocalDate entryDate = LocalDate.now();

    private String notes;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    public NutritionEntry() {}

    public NutritionEntry(String foodName, String mealType, int calories, double proteinG, double carbsG, double fatG, LocalDate entryDate) {
        this.foodName = foodName;
        this.mealType = mealType;
        this.calories = calories;
        this.proteinG = proteinG;
        this.carbsG = carbsG;
        this.fatG = fatG;
        this.entryDate = entryDate != null ? entryDate : LocalDate.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getFoodName() { return foodName; }
    public void setFoodName(String foodName) { this.foodName = foodName; }

    public String getMealType() { return mealType; }
    public void setMealType(String mealType) { this.mealType = mealType; }

    public String getServingSize() { return servingSize; }
    public void setServingSize(String servingSize) { this.servingSize = servingSize; }

    public int getCalories() { return calories; }
    public void setCalories(int calories) { this.calories = calories; }

    public double getProteinG() { return proteinG; }
    public void setProteinG(double proteinG) { this.proteinG = proteinG; }

    public double getCarbsG() { return carbsG; }
    public void setCarbsG(double carbsG) { this.carbsG = carbsG; }

    public double getFatG() { return fatG; }
    public void setFatG(double fatG) { this.fatG = fatG; }

    public LocalDate getEntryDate() { return entryDate; }
    public void setEntryDate(LocalDate entryDate) { this.entryDate = entryDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
