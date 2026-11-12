package com.example.trainingdashboard.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

import com.example.trainingdashboard.dto.WeeklyComparisonResponse;
import com.example.trainingdashboard.dto.WeeklyMetricsResponse;
import com.example.trainingdashboard.repository.WorkoutRepository;
import com.example.trainingdashboard.service.DashboardService;

@SpringBootTest
@ActiveProfiles({"test", "demo"})
class DemoDataLoaderFreshDatabaseTest {

    private static final ZoneId ZONE = ZoneId.of("America/Guayaquil");
    private static final LocalDate WEEK_START = LocalDate.of(2026, 8, 10);
    private static final LocalDate WEEK_END = LocalDate.of(2026, 8, 16);
    private static final LocalDate PREVIOUS_WEEK_START = LocalDate.of(2026, 8, 3);
    private static final LocalDate PREVIOUS_WEEK_END = LocalDate.of(2026, 8, 9);

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private WorkoutRepository workoutRepository;

    @TestConfiguration
    static class FixedClockConfiguration {

        @Bean(name = "freshDatabaseClock")
        @Primary
        Clock freshDatabaseClock() {
            return Clock.fixed(Instant.parse("2026-08-12T15:00:00Z"), ZONE);
        }
    }

    @AfterEach
    void deleteWorkoutsAfter() {
        workoutRepository.deleteAll();
    }

    @Test
    void loadsDemoDataOnStartupAgainstFreshSchema() {
        WeeklyComparisonResponse comparison = dashboardService.weeklyComparison();

        assertThat(comparison.currentWeek())
                .isEqualTo(new WeeklyMetricsResponse(WEEK_START, WEEK_END, 5, 180, 1200));
        assertThat(comparison.previousWeek())
                .isEqualTo(new WeeklyMetricsResponse(PREVIOUS_WEEK_START, PREVIOUS_WEEK_END, 4, 150, 980));
    }
}
