package com.example.trainingdashboard.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ErrorResponseTest {

    @Test
    void replacesNullFieldErrorsWithEmptyMap() {
        ErrorResponse response = new ErrorResponse("message", null);

        assertThat(response.fieldErrors()).isEmpty();
    }

    @Test
    void doesNotReflectChangesMadeToTheOriginalMap() {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        fieldErrors.put("type", "El tipo de entrenamiento es obligatorio");

        ErrorResponse response = new ErrorResponse("message", fieldErrors);
        fieldErrors.put("date", "La fecha es obligatoria");

        assertThat(response.fieldErrors()).containsOnlyKeys("type");
    }

    @Test
    void rejectsChangesToTheStoredMap() {
        ErrorResponse response = new ErrorResponse(
                "message", Map.of("type", "El tipo de entrenamiento es obligatorio"));

        assertThatThrownBy(() -> response.fieldErrors().put("date", "La fecha es obligatoria"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void preservesInsertionOrder() {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        fieldErrors.put("durationMinutes", "La duración debe ser mayor que 0");
        fieldErrors.put("type", "El tipo de entrenamiento es obligatorio");
        fieldErrors.put("date", "La fecha es obligatoria");

        ErrorResponse response = new ErrorResponse("message", fieldErrors);

        assertThat(response.fieldErrors()).containsExactly(
                Map.entry("durationMinutes", "La duración debe ser mayor que 0"),
                Map.entry("type", "El tipo de entrenamiento es obligatorio"),
                Map.entry("date", "La fecha es obligatoria"));
    }
}
