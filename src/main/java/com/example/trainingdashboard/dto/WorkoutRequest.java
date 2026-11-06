package com.example.trainingdashboard.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import com.example.trainingdashboard.enums.WorkoutType;

public record WorkoutRequest(
        @NotNull(message = "El tipo de entrenamiento es obligatorio")
        WorkoutType type,

        @NotNull(message = "La duración es obligatoria")
        @Min(value = 1, message = "La duración debe ser mayor que 0")
        @Max(value = 1440, message = "La duración no puede superar 1440 minutos")
        Integer durationMinutes,

        @NotNull(message = "La fecha es obligatoria")
        LocalDate date) {
}
