package com.example.trainingdashboard.dto;

public record WeeklyComparisonResponse(
        WeeklyMetricsResponse currentWeek,
        WeeklyMetricsResponse previousWeek,
        WeeklyDifferenceResponse difference) {
}
