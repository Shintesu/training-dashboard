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

    private static String body(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString();
    }
}
