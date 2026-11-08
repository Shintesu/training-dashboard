package com.example.trainingdashboard.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
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
class DashboardControllerTest {

    private static final LocalDate WEEK_START = LocalDate.of(2026, 8, 10);
    private static final LocalDate WEEK_END = LocalDate.of(2026, 8, 16);
    private static final LocalDate PREVIOUS_WEEK_START = LocalDate.of(2026, 8, 3);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WorkoutRepository workoutRepository;

    @TestConfiguration
    static class FixedClockConfiguration {

        @Bean
        @Primary
        Clock fixedDashboardClock() {
            return Clock.fixed(Instant.parse("2026-08-12T15:00:00Z"), ZoneId.of("America/Guayaquil"));
        }
    }

    @BeforeEach
    void deleteWorkoutsBefore() {
        workoutRepository.deleteAll();
    }

    @AfterEach
    void deleteWorkoutsAfter() {
        workoutRepository.deleteAll();
    }

    @Test
    void returnsZeroMetricsAndWeekDatesWhenThereAreNoWorkouts() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/dashboard/weekly")).andReturn();
        String responseBody = body(result);

        assertThat(result.getResponse().getStatus())
                .as("body: %s", responseBody)
                .isEqualTo(200);
        assertThat((String) JsonPath.read(responseBody, "$.startDate")).isEqualTo("2026-08-10");
        assertThat((String) JsonPath.read(responseBody, "$.endDate")).isEqualTo("2026-08-16");
        assertThat(((Number) JsonPath.read(responseBody, "$.totalWorkouts")).longValue()).isZero();
        assertThat(((Number) JsonPath.read(responseBody, "$.totalDurationMinutes")).longValue()).isZero();
        assertThat(((Number) JsonPath.read(responseBody, "$.totalCalories")).longValue()).isZero();
    }

    @Test
    void countsOnlyWorkoutsInsideTheCurrentWeek() throws Exception {
        workoutRepository.save(new Workout(WorkoutType.WEIGHTS, 60, 360, LocalDate.of(2026, 8, 9)));
        workoutRepository.save(new Workout(WorkoutType.RUNNING, 30, 300, LocalDate.of(2026, 8, 10)));
        workoutRepository.save(new Workout(WorkoutType.CYCLING, 20, 160, LocalDate.of(2026, 8, 12)));
        workoutRepository.save(new Workout(WorkoutType.SWIMMING, 30, 270, LocalDate.of(2026, 8, 15)));
        workoutRepository.save(new Workout(WorkoutType.WALKING, 10, 50, LocalDate.of(2026, 8, 16)));
        workoutRepository.save(new Workout(WorkoutType.RUNNING, 10, 100, LocalDate.of(2026, 8, 17)));

        MvcResult result = mockMvc.perform(get("/api/dashboard/weekly")).andReturn();
        String responseBody = body(result);

        assertThat(result.getResponse().getStatus())
                .as("body: %s", responseBody)
                .isEqualTo(200);
        assertThat((String) JsonPath.read(responseBody, "$.startDate")).isEqualTo("2026-08-10");
        assertThat((String) JsonPath.read(responseBody, "$.endDate")).isEqualTo("2026-08-16");
        assertThat(((Number) JsonPath.read(responseBody, "$.totalWorkouts")).longValue()).isEqualTo(4);
        assertThat(((Number) JsonPath.read(responseBody, "$.totalDurationMinutes")).longValue()).isEqualTo(90);
        assertThat(((Number) JsonPath.read(responseBody, "$.totalCalories")).longValue()).isEqualTo(780);
    }

    @Test
    void returnsMetricsAsJsonNumbers() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/dashboard/weekly")).andReturn();
        String responseBody = body(result);

        assertThat((Object) JsonPath.read(responseBody, "$.totalWorkouts")).isInstanceOf(Number.class);
        assertThat((Object) JsonPath.read(responseBody, "$.totalDurationMinutes")).isInstanceOf(Number.class);
        assertThat((Object) JsonPath.read(responseBody, "$.totalCalories")).isInstanceOf(Number.class);
    }

    @Test
    void ignoresUnknownQueryParameters() throws Exception {
        MvcResult result = mockMvc
                .perform(get("/api/dashboard/weekly?startDate=2020-01-01&endDate=2020-01-07"))
                .andReturn();
        String responseBody = body(result);

        assertThat(result.getResponse().getStatus())
                .as("body: %s", responseBody)
                .isEqualTo(200);
        assertThat((String) JsonPath.read(responseBody, "$.startDate")).isEqualTo("2026-08-10");
        assertThat((String) JsonPath.read(responseBody, "$.endDate")).isEqualTo("2026-08-16");
    }

    @Test
    void reflectsWorkoutsRegisteredThroughTheApi() throws Exception {
        mockMvc.perform(post("/api/workouts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"RUNNING\",\"durationMinutes\":30,\"date\":\"2026-08-12\"}"))
                .andReturn();

        MvcResult result = mockMvc.perform(get("/api/dashboard/weekly")).andReturn();
        String responseBody = body(result);

        assertThat(result.getResponse().getStatus())
                .as("body: %s", responseBody)
                .isEqualTo(200);
        assertThat(((Number) JsonPath.read(responseBody, "$.totalWorkouts")).longValue()).isEqualTo(1);
        assertThat(((Number) JsonPath.read(responseBody, "$.totalDurationMinutes")).longValue()).isEqualTo(30);
        assertThat(((Number) JsonPath.read(responseBody, "$.totalCalories")).longValue()).isEqualTo(300);
    }

    @Test
    void returnsZeroComparisonWhenThereAreNoWorkouts() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/dashboard/comparison")).andReturn();
        String responseBody = body(result);

        assertThat(result.getResponse().getStatus())
                .as("body: %s", responseBody)
                .isEqualTo(200);
        assertThat((String) JsonPath.read(responseBody, "$.currentWeek.startDate")).isEqualTo("2026-08-10");
        assertThat((String) JsonPath.read(responseBody, "$.currentWeek.endDate")).isEqualTo("2026-08-16");
        assertThat(((Number) JsonPath.read(responseBody, "$.currentWeek.totalWorkouts")).longValue()).isZero();
        assertThat(((Number) JsonPath.read(responseBody, "$.currentWeek.totalDurationMinutes")).longValue())
                .isZero();
        assertThat(((Number) JsonPath.read(responseBody, "$.currentWeek.totalCalories")).longValue()).isZero();
        assertThat((String) JsonPath.read(responseBody, "$.previousWeek.startDate")).isEqualTo("2026-08-03");
        assertThat((String) JsonPath.read(responseBody, "$.previousWeek.endDate")).isEqualTo("2026-08-09");
        assertThat(((Number) JsonPath.read(responseBody, "$.previousWeek.totalWorkouts")).longValue()).isZero();
        assertThat(((Number) JsonPath.read(responseBody, "$.previousWeek.totalDurationMinutes")).longValue())
                .isZero();
        assertThat(((Number) JsonPath.read(responseBody, "$.previousWeek.totalCalories")).longValue()).isZero();
        assertThat(((Number) JsonPath.read(responseBody, "$.difference.workouts")).longValue()).isZero();
        assertThat(((Number) JsonPath.read(responseBody, "$.difference.durationMinutes")).longValue()).isZero();
        assertThat(((Number) JsonPath.read(responseBody, "$.difference.calories")).longValue()).isZero();
    }

    @Test
    void returnsComparisonForTheDemoDataset() throws Exception {
        seedDemoWorkouts();

        MvcResult result = mockMvc.perform(get("/api/dashboard/comparison")).andReturn();
        String responseBody = body(result);

        assertThat(result.getResponse().getStatus())
                .as("body: %s", responseBody)
                .isEqualTo(200);
        assertThat((String) JsonPath.read(responseBody, "$.currentWeek.startDate")).isEqualTo("2026-08-10");
        assertThat((String) JsonPath.read(responseBody, "$.currentWeek.endDate")).isEqualTo("2026-08-16");
        assertThat(((Number) JsonPath.read(responseBody, "$.currentWeek.totalWorkouts")).longValue())
                .isEqualTo(5);
        assertThat(((Number) JsonPath.read(responseBody, "$.currentWeek.totalDurationMinutes")).longValue())
                .isEqualTo(180);
        assertThat(((Number) JsonPath.read(responseBody, "$.currentWeek.totalCalories")).longValue())
                .isEqualTo(1200);
        assertThat((String) JsonPath.read(responseBody, "$.previousWeek.startDate")).isEqualTo("2026-08-03");
        assertThat((String) JsonPath.read(responseBody, "$.previousWeek.endDate")).isEqualTo("2026-08-09");
        assertThat(((Number) JsonPath.read(responseBody, "$.previousWeek.totalWorkouts")).longValue())
                .isEqualTo(4);
        assertThat(((Number) JsonPath.read(responseBody, "$.previousWeek.totalDurationMinutes")).longValue())
                .isEqualTo(150);
        assertThat(((Number) JsonPath.read(responseBody, "$.previousWeek.totalCalories")).longValue())
                .isEqualTo(980);
        assertThat(((Number) JsonPath.read(responseBody, "$.difference.workouts")).longValue()).isEqualTo(1);
        assertThat(((Number) JsonPath.read(responseBody, "$.difference.durationMinutes")).longValue())
                .isEqualTo(30);
        assertThat(((Number) JsonPath.read(responseBody, "$.difference.calories")).longValue()).isEqualTo(220);
    }

    @Test
    void countsOnlyWorkoutsInsideEitherWeek() throws Exception {
        workoutRepository.save(new Workout(WorkoutType.RUNNING, 10, 100, LocalDate.of(2026, 8, 2)));
        workoutRepository.save(new Workout(WorkoutType.RUNNING, 10, 100, LocalDate.of(2026, 8, 17)));
        workoutRepository.save(new Workout(WorkoutType.CYCLING, 10, 80, LocalDate.of(2026, 8, 9)));
        workoutRepository.save(new Workout(WorkoutType.RUNNING, 10, 100, LocalDate.of(2026, 8, 10)));
        workoutRepository.save(new Workout(WorkoutType.WALKING, 10, 50, LocalDate.of(2026, 8, 16)));

        MvcResult result = mockMvc.perform(get("/api/dashboard/comparison")).andReturn();
        String responseBody = body(result);

        assertThat(result.getResponse().getStatus())
                .as("body: %s", responseBody)
                .isEqualTo(200);
        assertThat(((Number) JsonPath.read(responseBody, "$.currentWeek.totalWorkouts")).longValue())
                .isEqualTo(2);
        assertThat(((Number) JsonPath.read(responseBody, "$.currentWeek.totalDurationMinutes")).longValue())
                .isEqualTo(20);
        assertThat(((Number) JsonPath.read(responseBody, "$.currentWeek.totalCalories")).longValue())
                .isEqualTo(150);
        assertThat(((Number) JsonPath.read(responseBody, "$.previousWeek.totalWorkouts")).longValue())
                .isEqualTo(1);
        assertThat(((Number) JsonPath.read(responseBody, "$.previousWeek.totalDurationMinutes")).longValue())
                .isEqualTo(10);
        assertThat(((Number) JsonPath.read(responseBody, "$.previousWeek.totalCalories")).longValue())
                .isEqualTo(80);
        assertThat(((Number) JsonPath.read(responseBody, "$.difference.workouts")).longValue()).isEqualTo(1);
        assertThat(((Number) JsonPath.read(responseBody, "$.difference.durationMinutes")).longValue())
                .isEqualTo(10);
        assertThat(((Number) JsonPath.read(responseBody, "$.difference.calories")).longValue()).isEqualTo(70);
    }

    @Test
    void serializesNegativeDifferencesAsNumbers() throws Exception {
        seedPreviousWeekDemoWorkouts();
        workoutRepository.save(new Workout(WorkoutType.RUNNING, 30, 300, LocalDate.of(2026, 8, 12)));

        MvcResult result = mockMvc.perform(get("/api/dashboard/comparison")).andReturn();
        String responseBody = body(result);

        assertThat(result.getResponse().getStatus())
                .as("body: %s", responseBody)
                .isEqualTo(200);
        assertThat((Object) JsonPath.read(responseBody, "$.difference.workouts")).isInstanceOf(Number.class);
        assertThat((Object) JsonPath.read(responseBody, "$.difference.durationMinutes"))
                .isInstanceOf(Number.class);
        assertThat((Object) JsonPath.read(responseBody, "$.difference.calories")).isInstanceOf(Number.class);
        assertThat(((Number) JsonPath.read(responseBody, "$.difference.workouts")).longValue()).isEqualTo(-3);
        assertThat(((Number) JsonPath.read(responseBody, "$.difference.durationMinutes")).longValue())
                .isEqualTo(-120);
        assertThat(((Number) JsonPath.read(responseBody, "$.difference.calories")).longValue()).isEqualTo(-680);
    }

    @Test
    void ignoresUnknownQueryParametersOnComparison() throws Exception {
        MvcResult result = mockMvc
                .perform(get("/api/dashboard/comparison?startDate=2020-01-01&endDate=2020-01-07"))
                .andReturn();
        String responseBody = body(result);

        assertThat(result.getResponse().getStatus())
                .as("body: %s", responseBody)
                .isEqualTo(200);
        assertThat((String) JsonPath.read(responseBody, "$.currentWeek.startDate")).isEqualTo("2026-08-10");
        assertThat((String) JsonPath.read(responseBody, "$.previousWeek.startDate")).isEqualTo("2026-08-03");
    }

    @Test
    void rejectsPostOnComparison() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/dashboard/comparison")).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(405);
    }

    @Test
    void reflectsWorkoutsRegisteredThroughTheApiInTheComparison() throws Exception {
        seedDemoWorkouts();

        mockMvc.perform(post("/api/workouts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"RUNNING\",\"durationMinutes\":30,\"date\":\"2026-08-12\"}"))
                .andReturn();
        MvcResult result = mockMvc.perform(get("/api/dashboard/comparison")).andReturn();
        String responseBody = body(result);

        assertThat(result.getResponse().getStatus())
                .as("body: %s", responseBody)
                .isEqualTo(200);
        assertThat(((Number) JsonPath.read(responseBody, "$.currentWeek.totalWorkouts")).longValue())
                .isEqualTo(6);
        assertThat(((Number) JsonPath.read(responseBody, "$.currentWeek.totalDurationMinutes")).longValue())
                .isEqualTo(210);
        assertThat(((Number) JsonPath.read(responseBody, "$.currentWeek.totalCalories")).longValue())
                .isEqualTo(1500);
        assertThat(((Number) JsonPath.read(responseBody, "$.difference.workouts")).longValue()).isEqualTo(2);
        assertThat(((Number) JsonPath.read(responseBody, "$.difference.durationMinutes")).longValue())
                .isEqualTo(60);
        assertThat(((Number) JsonPath.read(responseBody, "$.difference.calories")).longValue()).isEqualTo(520);
    }

    private void seedDemoWorkouts() {
        seedCurrentWeekDemoWorkouts();
        seedPreviousWeekDemoWorkouts();
    }

    private void seedCurrentWeekDemoWorkouts() {
        workoutRepository.save(new Workout(WorkoutType.RUNNING, 20, 200, WEEK_START));
        workoutRepository.save(new Workout(WorkoutType.WEIGHTS, 60, 360, WEEK_START.plusDays(1)));
        workoutRepository.save(new Workout(WorkoutType.CYCLING, 20, 160, WEEK_START.plusDays(2)));
        workoutRepository.save(new Workout(WorkoutType.SWIMMING, 20, 180, WEEK_START.plusDays(3)));
        workoutRepository.save(new Workout(WorkoutType.WALKING, 60, 300, WEEK_START.plusDays(4)));
    }

    private void seedPreviousWeekDemoWorkouts() {
        workoutRepository.save(new Workout(WorkoutType.CYCLING, 30, 240, PREVIOUS_WEEK_START));
        workoutRepository.save(new Workout(WorkoutType.WEIGHTS, 40, 240, PREVIOUS_WEEK_START.plusDays(2)));
        workoutRepository.save(new Workout(WorkoutType.SWIMMING, 25, 225, PREVIOUS_WEEK_START.plusDays(4)));
        workoutRepository.save(new Workout(WorkoutType.WALKING, 55, 275, PREVIOUS_WEEK_START.plusDays(5)));
    }

    private static String body(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString();
    }
}
