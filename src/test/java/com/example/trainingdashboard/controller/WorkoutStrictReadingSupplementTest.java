package com.example.trainingdashboard.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.Map;
import java.util.stream.Stream;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.example.trainingdashboard.repository.WorkoutRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WorkoutStrictReadingSupplementTest {

    private static final String INVALID_DATA = "Los datos del entrenamiento no son válidos";
    private static final String TYPE_REQUIRED = "El tipo de entrenamiento es obligatorio";
    private static final String TYPE_INVALID = "El tipo de entrenamiento no es válido";
    private static final String DURATION_REQUIRED = "La duración es obligatoria";
    private static final String DURATION_POSITIVE = "La duración debe ser mayor que 0";
    private static final String DATE_REQUIRED = "La fecha es obligatoria";
    private static final String DATE_INVALID = "La fecha no es válida (formato AAAA-MM-DD)";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WorkoutRepository workoutRepository;

    @BeforeEach
    void deleteWorkoutsBefore() {
        workoutRepository.deleteAll();
    }

    @AfterEach
    void deleteWorkoutsAfter() {
        workoutRepository.deleteAll();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("supplementalInvalidCases")
    void rejectsRemainingStrictReadingAndNullCases(
            String label, String json, String expectedField, String expectedFieldMessage) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/workouts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andReturn();
        String responseBody = result.getResponse().getContentAsString();

        assertThat(result.getResponse().getStatus())
                .as("%s body: %s", label, responseBody)
                .isEqualTo(400);
        assertThat((String) JsonPath.read(responseBody, "$.message"))
                .as("%s body: %s", label, responseBody)
                .isEqualTo(INVALID_DATA);
        assertThat((Map<String, Object>) JsonPath.read(responseBody, "$.fieldErrors"))
                .as("%s body: %s", label, responseBody)
                .containsEntry(expectedField, expectedFieldMessage);
    }

    static Stream<Arguments> supplementalInvalidCases() {
        return Stream.of(
                Arguments.of("type null", "{\"type\":null,\"durationMinutes\":30,\"date\":\"2026-08-12\"}",
                        "type", TYPE_REQUIRED),
                Arguments.of("type object", "{\"type\":{},\"durationMinutes\":30,\"date\":\"2026-08-12\"}",
                        "type", TYPE_INVALID),
                Arguments.of("type decimal", "{\"type\":1.5,\"durationMinutes\":30,\"date\":\"2026-08-12\"}",
                        "type", TYPE_INVALID),
                Arguments.of("duration null", "{\"type\":\"RUNNING\",\"durationMinutes\":null,"
                                + "\"date\":\"2026-08-12\"}",
                        "durationMinutes", DURATION_REQUIRED),
                Arguments.of("duration negative", "{\"type\":\"RUNNING\",\"durationMinutes\":-1,"
                                + "\"date\":\"2026-08-12\"}",
                        "durationMinutes", DURATION_POSITIVE),
                Arguments.of("date null", "{\"type\":\"RUNNING\",\"durationMinutes\":30,\"date\":null}",
                        "date", DATE_REQUIRED),
                Arguments.of("date trailing space", "{\"type\":\"RUNNING\",\"durationMinutes\":30,"
                                + "\"date\":\"2026-08-12 \"}",
                        "date", DATE_INVALID));
    }
}
