package com.example.trainingdashboard.dto;

public record WeeklyDifferenceResponse(
        long workouts,
        long durationMinutes,
        long calories) {
}
