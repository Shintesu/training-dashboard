package com.example.trainingdashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.trainingdashboard.dto.WeeklyMetricsResponse;
import com.example.trainingdashboard.entity.Workout;
import com.example.trainingdashboard.enums.WorkoutType;
import com.example.trainingdashboard.repository.WorkoutRepository;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    private static final ZoneId GUAYAQUIL = ZoneId.of("America/Guayaquil");
    private static final LocalDate WEEK_START = LocalDate.of(2026, 8, 10);
    private static final LocalDate WEEK_END = LocalDate.of(2026, 8, 16);

    @Mock
    private WorkoutRepository workoutRepository;

    @Test
    void usesMondayToSundayWeekWhenTodayIsMonday() {
        DashboardService dashboardService = serviceWithClockOn(LocalDate.of(2026, 8, 10));

        WeeklyMetricsResponse response = dashboardService.weeklyMetrics();

        assertThat(response.startDate()).isEqualTo(WEEK_START);
        assertThat(response.endDate()).isEqualTo(WEEK_END);
        verify(workoutRepository).findByDateBetween(WEEK_START, WEEK_END);
        verifyNoMoreInteractions(workoutRepository);
    }

    @Test
    void usesMondayToSundayWeekWhenTodayIsWednesday() {
        DashboardService dashboardService = serviceWithClockOn(LocalDate.of(2026, 8, 12));

        WeeklyMetricsResponse response = dashboardService.weeklyMetrics();

        assertThat(response.startDate()).isEqualTo(WEEK_START);
        assertThat(response.endDate()).isEqualTo(WEEK_END);
        verify(workoutRepository).findByDateBetween(WEEK_START, WEEK_END);
        verifyNoMoreInteractions(workoutRepository);
    }

    @Test
    void usesMondayToSundayWeekWhenTodayIsSunday() {
        DashboardService dashboardService = serviceWithClockOn(LocalDate.of(2026, 8, 16));

        WeeklyMetricsResponse response = dashboardService.weeklyMetrics();

        assertThat(response.startDate()).isEqualTo(WEEK_START);
        assertThat(response.endDate()).isEqualTo(WEEK_END);
        verify(workoutRepository).findByDateBetween(WEEK_START, WEEK_END);
        verifyNoMoreInteractions(workoutRepository);
    }

    @Test
    void crossesMonthBoundary() {
        DashboardService dashboardService = serviceWithClockOn(LocalDate.of(2026, 9, 2));

        WeeklyMetricsResponse response = dashboardService.weeklyMetrics();

        assertThat(response.startDate()).isEqualTo(LocalDate.of(2026, 8, 31));
        assertThat(response.endDate()).isEqualTo(LocalDate.of(2026, 9, 6));
        verify(workoutRepository).findByDateBetween(LocalDate.of(2026, 8, 31), LocalDate.of(2026, 9, 6));
        verifyNoMoreInteractions(workoutRepository);
    }

    @Test
    void crossesYearBoundary() {
        DashboardService dashboardService = serviceWithClockOn(LocalDate.of(2027, 1, 1));

        WeeklyMetricsResponse response = dashboardService.weeklyMetrics();

        assertThat(response.startDate()).isEqualTo(LocalDate.of(2026, 12, 28));
        assertThat(response.endDate()).isEqualTo(LocalDate.of(2027, 1, 3));
        verify(workoutRepository).findByDateBetween(LocalDate.of(2026, 12, 28), LocalDate.of(2027, 1, 3));
        verifyNoMoreInteractions(workoutRepository);
    }

    @Test
    void usesGuayaquilZoneToDetermineTheWeek() {
        DashboardService dashboardService = serviceWithFixedInstant(
                Instant.parse("2026-08-10T02:00:00Z"), GUAYAQUIL);

        WeeklyMetricsResponse response = dashboardService.weeklyMetrics();

        assertThat(response.startDate()).isEqualTo(LocalDate.of(2026, 8, 3));
        assertThat(response.endDate()).isEqualTo(LocalDate.of(2026, 8, 9));
        verify(workoutRepository).findByDateBetween(LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 9));
        verifyNoMoreInteractions(workoutRepository);
    }

    @Test
    void usesUtcZoneToDetermineTheWeek() {
        DashboardService dashboardService = serviceWithFixedInstant(
                Instant.parse("2026-08-10T02:00:00Z"), ZoneId.of("UTC"));

        WeeklyMetricsResponse response = dashboardService.weeklyMetrics();

        assertThat(response.startDate()).isEqualTo(WEEK_START);
        assertThat(response.endDate()).isEqualTo(WEEK_END);
        verify(workoutRepository).findByDateBetween(WEEK_START, WEEK_END);
        verifyNoMoreInteractions(workoutRepository);
    }

    @Test
    void sumsWorkoutCountDurationAndCalories() {
        DashboardService dashboardService = serviceWithClockOn(LocalDate.of(2026, 8, 12));
        when(workoutRepository.findByDateBetween(WEEK_START, WEEK_END)).thenReturn(List.of(
                new Workout(WorkoutType.RUNNING, 30, 300, LocalDate.of(2026, 8, 10)),
                new Workout(WorkoutType.CYCLING, 20, 160, LocalDate.of(2026, 8, 12)),
                new Workout(WorkoutType.SWIMMING, 45, 405, LocalDate.of(2026, 8, 16))));

        WeeklyMetricsResponse response = dashboardService.weeklyMetrics();

        assertThat(response.totalWorkouts()).isEqualTo(3);
        assertThat(response.totalDurationMinutes()).isEqualTo(95);
        assertThat(response.totalCalories()).isEqualTo(865);
    }

    @Test
    void keepsCaloriesTotalAsLongWithoutOverflow() {
        DashboardService dashboardService = serviceWithClockOn(LocalDate.of(2026, 8, 12));
        when(workoutRepository.findByDateBetween(WEEK_START, WEEK_END)).thenReturn(List.of(
                new Workout(WorkoutType.RUNNING, 30, 2_000_000_000, LocalDate.of(2026, 8, 10)),
                new Workout(WorkoutType.CYCLING, 30, 2_000_000_000, LocalDate.of(2026, 8, 11))));

        WeeklyMetricsResponse response = dashboardService.weeklyMetrics();

        assertThat(response.totalCalories()).isEqualTo(4_000_000_000L);
    }

    @Test
    void returnsZerosWhenTheWeekIsEmpty() {
        DashboardService dashboardService = serviceWithClockOn(LocalDate.of(2026, 8, 12));
        when(workoutRepository.findByDateBetween(WEEK_START, WEEK_END)).thenReturn(List.of());

        WeeklyMetricsResponse response = dashboardService.weeklyMetrics();

        assertThat(response).isEqualTo(new WeeklyMetricsResponse(WEEK_START, WEEK_END, 0, 0, 0));
    }

    private DashboardService serviceWithClockOn(LocalDate date) {
        return serviceWithFixedInstant(date.atTime(12, 0).atZone(GUAYAQUIL).toInstant(), GUAYAQUIL);
    }

    private DashboardService serviceWithFixedInstant(Instant instant, ZoneId zone) {
        return new DashboardService(workoutRepository, Clock.fixed(instant, zone));
    }
}
