package com.example.trainingdashboard.dto;

import java.time.LocalDate;

public record WeeklyMetricsResponse(
        LocalDate startDate,
        LocalDate endDate,
        long totalWorkouts,
        long totalDurationMinutes,
        long totalCalories) {
}
