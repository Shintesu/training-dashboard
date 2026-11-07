package com.example.trainingdashboard.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.example.trainingdashboard.entity.Workout;
import com.example.trainingdashboard.enums.WorkoutType;
import com.example.trainingdashboard.repository.WorkoutRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WorkoutControllerTest {

    private static final String INVALID_DATA = "Los datos del entrenamiento no son válidos";
    private static final String INVALID_BODY = "El cuerpo de la solicitud no es válido";
    private static final String INVALID_ID = "El identificador del entrenamiento no es válido";
    private static final String INTEGER_DURATION = "La duración debe ser un número entero de minutos";
    private static final String INVALID_DATE = "La fecha no es válida (formato AAAA-MM-DD)";

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

    @Test
    void registersWorkoutWithCalculatedCaloriesAndWithoutLocation() throws Exception {
        MvcResult result = postJson(validBody());
        String responseBody = body(result);

        assertThat(result.getResponse().getStatus())
                .as("body: %s", responseBody)
                .isEqualTo(201);
        assertThat(result.getResponse().getHeader("Location")).isNull();
        assertThat(((Number) JsonPath.read(responseBody, "$.id")).longValue()).isPositive();
        assertThat((String) JsonPath.read(responseBody, "$.type")).isEqualTo("RUNNING");
        assertThat((Integer) JsonPath.read(responseBody, "$.durationMinutes")).isEqualTo(30);
        assertThat((Integer) JsonPath.read(responseBody, "$.calories")).isEqualTo(300);
        assertThat((String) JsonPath.read(responseBody, "$.date")).isEqualTo("2026-08-12");
    }

    @Test
    void ignoresClientProvidedIdAndCalories() throws Exception {
        MvcResult result = postJson(
                "{\"id\":999999,\"type\":\"RUNNING\",\"durationMinutes\":30,\"calories\":9999,"
                        + "\"date\":\"2026-08-12\"}");
        String responseBody = body(result);

        assertThat(result.getResponse().getStatus())
                .as("body: %s", responseBody)
                .isEqualTo(201);
        assertThat((Integer) JsonPath.read(responseBody, "$.calories")).isEqualTo(300);
        assertThat(((Number) JsonPath.read(responseBody, "$.id")).longValue()).isNotEqualTo(999999L);
    }

    @Test
    void acceptsMinimumDuration() throws Exception {
        MvcResult result = postJson(workoutJson("\"RUNNING\"", "1", "\"2026-08-12\""));
        String responseBody = body(result);

        assertThat(result.getResponse().getStatus())
                .as("body: %s", responseBody)
                .isEqualTo(201);
        assertThat((Integer) JsonPath.read(responseBody, "$.calories")).isEqualTo(10);
    }

    @Test
    void acceptsMaximumDuration() throws Exception {
        MvcResult result = postJson(workoutJson("\"RUNNING\"", "1440", "\"2026-08-12\""));
        String responseBody = body(result);

        assertThat(result.getResponse().getStatus())
                .as("body: %s", responseBody)
                .isEqualTo(201);
        assertThat((Integer) JsonPath.read(responseBody, "$.calories")).isEqualTo(14400);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidFieldCases")
    void rejectsInvalidFieldValues(String label, String json, String expectedField, String expectedFieldMessage)
            throws Exception {
        assertFieldError(postJson(json), expectedField, expectedFieldMessage);
    }

    @Test
    void rejectsMissingType() throws Exception {
        assertFieldError(postJson("{\"durationMinutes\":30,\"date\":\"2026-08-12\"}"),
                "type", "El tipo de entrenamiento es obligatorio");
    }

    @Test
    void rejectsMissingDuration() throws Exception {
        assertFieldError(postJson("{\"type\":\"RUNNING\",\"date\":\"2026-08-12\"}"),
                "durationMinutes", "La duración es obligatoria");
    }

    @Test
    void rejectsZeroDuration() throws Exception {
        assertFieldError(postJson("{\"type\":\"RUNNING\",\"durationMinutes\":0,\"date\":\"2026-08-12\"}"),
                "durationMinutes", "La duración debe ser mayor que 0");
    }

    @Test
    void rejectsDurationAboveMaximum() throws Exception {
        assertFieldError(postJson("{\"type\":\"RUNNING\",\"durationMinutes\":1441,\"date\":\"2026-08-12\"}"),
                "durationMinutes", "La duración no puede superar 1440 minutos");
    }

    @Test
    void rejectsMissingDate() throws Exception {
        assertFieldError(postJson("{\"type\":\"RUNNING\",\"durationMinutes\":30}"),
                "date", "La fecha es obligatoria");
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"99999999999", "-99999999999"})
    void rejectsDurationsOutsideIntegerRange(String duration) throws Exception {
        MvcResult result = postJson(workoutJson("\"RUNNING\"", duration, "\"2026-08-12\""));
        String responseBody = body(result);

        assertThat(result.getResponse().getStatus())
                .as("body: %s", responseBody)
                .isEqualTo(400);
        String message = JsonPath.read(responseBody, "$.message");
        if (INVALID_BODY.equals(message)) {
            assertThat((Map<String, Object>) JsonPath.read(responseBody, "$.fieldErrors")).isEmpty();
        } else {
            assertThat(message).isEqualTo(INVALID_DATA);
            assertThat((Map<String, Object>) JsonPath.read(responseBody, "$.fieldErrors"))
                    .containsEntry("durationMinutes", INTEGER_DURATION);
        }
    }

    @Test
    void rejectsEmptyBody() throws Exception {
        assertInvalidBody(postJson(""));
    }

    @Test
    void rejectsMalformedJson() throws Exception {
        assertInvalidBody(postJson("{\"type\":\"RUNNING\","));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"null", "[]", "\"texto\""})
    void rejectsBodiesThatAreNotAnObject(String json) throws Exception {
        assertInvalidBody(postJson(json));
    }

    @Test
    void rejectsMultipleInvalidFieldsTogether() throws Exception {
        MvcResult result = postJson("{\"durationMinutes\":0}");
        String responseBody = body(result);

        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat((String) JsonPath.read(responseBody, "$.message")).isEqualTo(INVALID_DATA);
        assertThat((Map<String, Object>) JsonPath.read(responseBody, "$.fieldErrors"))
                .containsEntry("type", "El tipo de entrenamiento es obligatorio")
                .containsEntry("durationMinutes", "La duración debe ser mayor que 0")
                .containsEntry("date", "La fecha es obligatoria");
    }

    @Test
    void rejectsNonNumericId() throws Exception {
        MvcResult result = mockMvc.perform(delete("/api/workouts/abc")).andReturn();
        String responseBody = body(result);

        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat((String) JsonPath.read(responseBody, "$.message")).isEqualTo(INVALID_ID);
        assertThat((Map<String, Object>) JsonPath.read(responseBody, "$.fieldErrors")).isEmpty();
    }

    @Test
    void rejectsUnknownId() throws Exception {
        MvcResult result = mockMvc.perform(delete("/api/workouts/999999")).andReturn();
        String responseBody = body(result);

        assertThat(result.getResponse().getStatus()).isEqualTo(404);
        assertThat((String) JsonPath.read(responseBody, "$.message"))
                .isEqualTo("No se encontró el entrenamiento con id 999999");
        assertThat((Map<String, Object>) JsonPath.read(responseBody, "$.fieldErrors")).isEmpty();
    }

    @Test
    void returnsEmptyListWhenThereAreNoWorkouts() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/workouts")).andReturn();
        String responseBody = body(result);

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat((List<?>) JsonPath.read(responseBody, "$")).isEmpty();
    }

    @Test
    void listsWorkoutsByDateDescendingThenIdDescending() throws Exception {
        Workout oldest = workoutRepository.save(
                new Workout(WorkoutType.RUNNING, 30, 300, LocalDate.of(2026, 8, 10)));
        Workout newerEarlierId = workoutRepository.save(
                new Workout(WorkoutType.CYCLING, 30, 240, LocalDate.of(2026, 8, 12)));
        Workout newerLaterId = workoutRepository.save(
                new Workout(WorkoutType.SWIMMING, 30, 270, LocalDate.of(2026, 8, 12)));

        MvcResult result = mockMvc.perform(get("/api/workouts")).andReturn();
        List<Number> ids = JsonPath.read(body(result), "$[*].id");

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(ids).extracting(Number::longValue)
                .containsExactly(newerLaterId.getId(), newerEarlierId.getId(), oldest.getId());
    }

    @Test
    void deletesExistingWorkoutAndRemovesItFromTheList() throws Exception {
        MvcResult created = postJson(validBody());
        long id = ((Number) JsonPath.read(body(created), "$.id")).longValue();

        MvcResult deleted = mockMvc.perform(delete("/api/workouts/{id}", id)).andReturn();
        MvcResult list = mockMvc.perform(get("/api/workouts")).andReturn();

        assertThat(deleted.getResponse().getStatus()).isEqualTo(204);
        assertThat(body(deleted)).isEmpty();
        assertThat((List<?>) JsonPath.read(body(list), "$")).isEmpty();
    }

    @Test
    void listsWorkoutTypesInDeclarationOrder() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/workout-types")).andReturn();
        List<Map<String, Object>> types = JsonPath.read(body(result), "$");

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(types).extracting(type -> type.get("code"))
                .containsExactly("RUNNING", "CYCLING", "SWIMMING", "WEIGHTS", "WALKING");
        assertThat(types).extracting(type -> type.get("label"))
                .containsExactly("Correr", "Ciclismo", "Natación", "Pesas", "Caminata");
        assertThat(types).extracting(type -> ((Number) type.get("caloriesPerMinute")).intValue())
                .containsExactly(10, 8, 9, 6, 5);
    }

    @Test
    void registersListsDeletesAndListsAgain() throws Exception {
        MvcResult first = postJson(workoutJson("\"RUNNING\"", "30", "\"2026-08-10\""));
        MvcResult second = postJson(workoutJson("\"CYCLING\"", "20", "\"2026-08-12\""));
        long firstId = ((Number) JsonPath.read(body(first), "$.id")).longValue();
        long secondId = ((Number) JsonPath.read(body(second), "$.id")).longValue();

        MvcResult afterRegistration = mockMvc.perform(get("/api/workouts")).andReturn();
        MvcResult deleted = mockMvc.perform(delete("/api/workouts/{id}", firstId)).andReturn();
        MvcResult afterDeletion = mockMvc.perform(get("/api/workouts")).andReturn();
        List<Number> remainingIds = JsonPath.read(body(afterDeletion), "$[*].id");

        assertThat((List<?>) JsonPath.read(body(afterRegistration), "$")).hasSize(2);
        assertThat(deleted.getResponse().getStatus()).isEqualTo(204);
        assertThat(remainingIds).extracting(Number::longValue).containsExactly(secondId);
    }

    @Test
    void persistsCalculatedCalories() throws Exception {
        MvcResult result = postJson(validBody());
        long id = ((Number) JsonPath.read(body(result), "$.id")).longValue();

        Workout persisted = workoutRepository.findById(id).orElseThrow();

        assertThat(persisted.getCalories()).isEqualTo(300);
    }

    static Stream<Arguments> invalidFieldCases() {
        return Stream.of(
                Arguments.of("duration 30.5", workoutJson("\"RUNNING\"", "30.5", "\"2026-08-12\""),
                        "durationMinutes", INTEGER_DURATION),
                Arguments.of("duration 30.0", workoutJson("\"RUNNING\"", "30.0", "\"2026-08-12\""),
                        "durationMinutes", INTEGER_DURATION),
                Arguments.of("duration \"30\"", workoutJson("\"RUNNING\"", "\"30\"", "\"2026-08-12\""),
                        "durationMinutes", INTEGER_DURATION),
                Arguments.of("duration \"abc\"", workoutJson("\"RUNNING\"", "\"abc\"", "\"2026-08-12\""),
                        "durationMinutes", INTEGER_DURATION),
                Arguments.of("duration true", workoutJson("\"RUNNING\"", "true", "\"2026-08-12\""),
                        "durationMinutes", INTEGER_DURATION),
                Arguments.of("duration [30]", workoutJson("\"RUNNING\"", "[30]", "\"2026-08-12\""),
                        "durationMinutes", INTEGER_DURATION),
                Arguments.of("duration {}", workoutJson("\"RUNNING\"", "{}", "\"2026-08-12\""),
                        "durationMinutes", INTEGER_DURATION),
                Arguments.of("duration \"\"", workoutJson("\"RUNNING\"", "\"\"", "\"2026-08-12\""),
                        "durationMinutes", "La duración es obligatoria"),
                Arguments.of("type \"running\"", workoutJson("\"running\"", "30", "\"2026-08-12\""),
                        "type", "El tipo de entrenamiento no es válido"),
                Arguments.of("type 0", workoutJson("0", "30", "\"2026-08-12\""),
                        "type", "El tipo de entrenamiento no es válido"),
                Arguments.of("type true", workoutJson("true", "30", "\"2026-08-12\""),
                        "type", "El tipo de entrenamiento no es válido"),
                Arguments.of("type [\"RUNNING\"]", workoutJson("[\"RUNNING\"]", "30", "\"2026-08-12\""),
                        "type", "El tipo de entrenamiento no es válido"),
                Arguments.of("type \"\"", workoutJson("\"\"", "30", "\"2026-08-12\""),
                        "type", "El tipo de entrenamiento es obligatorio"),
                Arguments.of("date \"\"", workoutJson("\"RUNNING\"", "30", "\"\""),
                        "date", "La fecha es obligatoria"),
                Arguments.of("date 20260812", workoutJson("\"RUNNING\"", "30", "20260812"),
                        "date", INVALID_DATE),
                Arguments.of("date [2026,8,12]", workoutJson("\"RUNNING\"", "30", "[2026,8,12]"),
                        "date", INVALID_DATE),
                Arguments.of("date true", workoutJson("\"RUNNING\"", "30", "true"),
                        "date", INVALID_DATE),
                Arguments.of("date {}", workoutJson("\"RUNNING\"", "30", "{}"),
                        "date", INVALID_DATE),
                Arguments.of("date \"2026-8-12\"", workoutJson("\"RUNNING\"", "30", "\"2026-8-12\""),
                        "date", INVALID_DATE),
                Arguments.of("date \"2026-08-12T10:00:00\"",
                        workoutJson("\"RUNNING\"", "30", "\"2026-08-12T10:00:00\""),
                        "date", INVALID_DATE),
                Arguments.of("date \"2026-02-30\"", workoutJson("\"RUNNING\"", "30", "\"2026-02-30\""),
                        "date", INVALID_DATE),
                Arguments.of("date \"+57442-03-22\"",
                        workoutJson("\"RUNNING\"", "30", "\"+57442-03-22\""),
                        "date", INVALID_DATE),
                Arguments.of("date \" 2026-08-12\"",
                        workoutJson("\"RUNNING\"", "30", "\" 2026-08-12\""),
                        "date", INVALID_DATE));
    }

    private static String workoutJson(String type, String duration, String date) {
        return "{\"type\":" + type + ",\"durationMinutes\":" + duration + ",\"date\":" + date + "}";
    }

    private static String validBody() {
        return workoutJson("\"RUNNING\"", "30", "\"2026-08-12\"");
    }

    private MvcResult postJson(String json) throws Exception {
        return mockMvc.perform(post("/api/workouts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andReturn();
    }

    private void assertFieldError(MvcResult result, String field, String fieldMessage) throws Exception {
        String responseBody = body(result);

        assertThat(result.getResponse().getStatus())
                .as("body: %s", responseBody)
                .isEqualTo(400);
        assertThat((String) JsonPath.read(responseBody, "$.message"))
                .as("body: %s", responseBody)
                .isEqualTo(INVALID_DATA);
        assertThat((Map<String, Object>) JsonPath.read(responseBody, "$.fieldErrors"))
                .as("body: %s", responseBody)
                .containsEntry(field, fieldMessage);
    }

    private void assertInvalidBody(MvcResult result) throws Exception {
        String responseBody = body(result);

        assertThat(result.getResponse().getStatus())
                .as("body: %s", responseBody)
                .isEqualTo(400);
        assertThat((String) JsonPath.read(responseBody, "$.message"))
                .as("body: %s", responseBody)
                .isEqualTo(INVALID_BODY);
        assertThat((Map<String, Object>) JsonPath.read(responseBody, "$.fieldErrors")).isEmpty();
    }

    private static String body(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString();
    }
}
