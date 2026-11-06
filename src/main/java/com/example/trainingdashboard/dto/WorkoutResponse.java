package com.example.trainingdashboard.dto;

import java.time.LocalDate;

import com.example.trainingdashboard.enums.WorkoutType;

public record WorkoutResponse(
        Long id,
        WorkoutType type,
        Integer durationMinutes,
        Integer calories,
        LocalDate date) {
}
