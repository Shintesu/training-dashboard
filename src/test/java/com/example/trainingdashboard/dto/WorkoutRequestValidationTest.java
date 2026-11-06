package com.example.trainingdashboard.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import com.example.trainingdashboard.enums.WorkoutType;

class WorkoutRequestValidationTest {

    private static final LocalDate DATE = LocalDate.of(2026, 8, 12);

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsValidRequest() {
        assertThat(validator.validate(new WorkoutRequest(WorkoutType.RUNNING, 30, DATE))).isEmpty();
    }

    @Test
    void acceptsMinimumDuration() {
        assertThat(validator.validate(new WorkoutRequest(WorkoutType.RUNNING, 1, DATE))).isEmpty();
    }

    @Test
    void acceptsMaximumDuration() {
        assertThat(validator.validate(new WorkoutRequest(WorkoutType.RUNNING, 1440, DATE))).isEmpty();
    }

    @Test
    void rejectsZeroDuration() {
        assertViolation(new WorkoutRequest(WorkoutType.RUNNING, 0, DATE),
                "durationMinutes", "La duración debe ser mayor que 0");
    }

    @Test
    void rejectsDurationAboveMaximum() {
        assertViolation(new WorkoutRequest(WorkoutType.RUNNING, 1441, DATE),
                "durationMinutes", "La duración no puede superar 1440 minutos");
    }

    @Test
    void rejectsNullDuration() {
        assertViolation(new WorkoutRequest(WorkoutType.RUNNING, null, DATE),
                "durationMinutes", "La duración es obligatoria");
    }

    @Test
    void rejectsNullType() {
        assertViolation(new WorkoutRequest(null, 30, DATE),
                "type", "El tipo de entrenamiento es obligatorio");
    }

    @Test
    void rejectsNullDate() {
        assertViolation(new WorkoutRequest(WorkoutType.RUNNING, 30, null),
                "date", "La fecha es obligatoria");
    }

    private void assertViolation(WorkoutRequest request, String field, String message) {
        Set<ConstraintViolation<WorkoutRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        ConstraintViolation<WorkoutRequest> violation = violations.iterator().next();
        assertThat(violation.getPropertyPath().toString()).isEqualTo(field);
        assertThat(violation.getMessage()).isEqualTo(message);
    }
}
