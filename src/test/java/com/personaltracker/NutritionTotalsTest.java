package com.personaltracker;

import com.personaltracker.model.NutritionEntry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Nutrition Daily Totals Unit Tests")
public class NutritionTotalsTest {

    @Test
    @DisplayName("Should accurately sum daily calories and macronutrients")
    void testDailyNutritionTotals() {
        LocalDate date = LocalDate.now();

        List<NutritionEntry> entries = Arrays.asList(
                new NutritionEntry("Eggs & Toast", "BREAKFAST", 450, 24.0, 38.0, 18.0, date),
                new NutritionEntry("Chicken & Rice", "LUNCH", 650, 52.0, 72.0, 12.0, date),
                new NutritionEntry("Salmon & Sweet Potato", "DINNER", 580, 42.0, 45.0, 20.0, date),
                new NutritionEntry("Protein Shake", "SNACK", 180, 28.0, 6.0, 3.0, date)
        );

        int totalCalories = entries.stream().mapToInt(NutritionEntry::getCalories).sum();
        double totalProtein = entries.stream().mapToDouble(NutritionEntry::getProteinG).sum();
        double totalCarbs = entries.stream().mapToDouble(NutritionEntry::getCarbsG).sum();
        double totalFat = entries.stream().mapToDouble(NutritionEntry::getFatG).sum();

        assertEquals(1860, totalCalories);
        assertEquals(146.0, totalProtein, 0.01);
        assertEquals(161.0, totalCarbs, 0.01);
        assertEquals(53.0, totalFat, 0.01);
    }
}
