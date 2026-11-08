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
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.trainingdashboard.dto.WeeklyComparisonResponse;
import com.example.trainingdashboard.dto.WeeklyDifferenceResponse;
import com.example.trainingdashboard.dto.WeeklyMetricsResponse;
import com.example.trainingdashboard.entity.Workout;
import com.example.trainingdashboard.enums.WorkoutType;
import com.example.trainingdashboard.repository.WorkoutRepository;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    private static final ZoneId GUAYAQUIL = ZoneId.of("America/Guayaquil");
    private static final LocalDate WEEK_START = LocalDate.of(2026, 8, 10);
    private static final LocalDate WEEK_END = LocalDate.of(2026, 8, 16);
    private static final LocalDate PREVIOUS_WEEK_START = LocalDate.of(2026, 8, 3);
    private static final LocalDate PREVIOUS_WEEK_END = LocalDate.of(2026, 8, 9);

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

    @Test
    void queriesCurrentAndPreviousWeeksWhenTodayIsWednesday() {
        DashboardService dashboardService = serviceWithClockOn(LocalDate.of(2026, 8, 12));
        stubWeek(WEEK_START, List.of());
        stubWeek(PREVIOUS_WEEK_START, List.of());

        WeeklyComparisonResponse response = dashboardService.weeklyComparison();

        assertThat(response.currentWeek()).isEqualTo(new WeeklyMetricsResponse(WEEK_START, WEEK_END, 0, 0, 0));
        assertThat(response.previousWeek())
                .isEqualTo(new WeeklyMetricsResponse(PREVIOUS_WEEK_START, PREVIOUS_WEEK_END, 0, 0, 0));
        verify(workoutRepository).findByDateBetween(WEEK_START, WEEK_END);
        verify(workoutRepository).findByDateBetween(PREVIOUS_WEEK_START, PREVIOUS_WEEK_END);
        verifyNoMoreInteractions(workoutRepository);
    }

    @Test
    void usesPreviousWeekAcrossMonthBoundary() {
        DashboardService dashboardService = serviceWithClockOn(LocalDate.of(2026, 9, 2));
        LocalDate currentStart = LocalDate.of(2026, 8, 31);
        LocalDate currentEnd = LocalDate.of(2026, 9, 6);
        LocalDate previousStart = LocalDate.of(2026, 8, 24);
        LocalDate previousEnd = LocalDate.of(2026, 8, 30);
        stubWeek(currentStart, List.of());
        stubWeek(previousStart, List.of());

        WeeklyComparisonResponse response = dashboardService.weeklyComparison();

        assertThat(response.currentWeek())
                .isEqualTo(new WeeklyMetricsResponse(currentStart, currentEnd, 0, 0, 0));
        assertThat(response.previousWeek())
                .isEqualTo(new WeeklyMetricsResponse(previousStart, previousEnd, 0, 0, 0));
        verify(workoutRepository).findByDateBetween(currentStart, currentEnd);
        verify(workoutRepository).findByDateBetween(previousStart, previousEnd);
        verifyNoMoreInteractions(workoutRepository);
    }

    @Test
    void usesPreviousWeekAcrossYearBoundary() {
        DashboardService dashboardService = serviceWithClockOn(LocalDate.of(2027, 1, 1));
        LocalDate currentStart = LocalDate.of(2026, 12, 28);
        LocalDate currentEnd = LocalDate.of(2027, 1, 3);
        LocalDate previousStart = LocalDate.of(2026, 12, 21);
        LocalDate previousEnd = LocalDate.of(2026, 12, 27);
        stubWeek(currentStart, List.of());
        stubWeek(previousStart, List.of());

        WeeklyComparisonResponse response = dashboardService.weeklyComparison();

        assertThat(response.currentWeek())
                .isEqualTo(new WeeklyMetricsResponse(currentStart, currentEnd, 0, 0, 0));
        assertThat(response.previousWeek())
                .isEqualTo(new WeeklyMetricsResponse(previousStart, previousEnd, 0, 0, 0));
        verify(workoutRepository).findByDateBetween(currentStart, currentEnd);
        verify(workoutRepository).findByDateBetween(previousStart, previousEnd);
        verifyNoMoreInteractions(workoutRepository);
    }

    @Test
    void usesClockZoneToDetermineBothWeeks() {
        DashboardService dashboardService = serviceWithFixedInstant(
                Instant.parse("2026-08-10T02:00:00Z"), GUAYAQUIL);
        LocalDate currentStart = LocalDate.of(2026, 8, 3);
        LocalDate currentEnd = LocalDate.of(2026, 8, 9);
        LocalDate previousStart = LocalDate.of(2026, 7, 27);
        LocalDate previousEnd = LocalDate.of(2026, 8, 2);
        stubWeek(currentStart, List.of());
        stubWeek(previousStart, List.of());

        WeeklyComparisonResponse response = dashboardService.weeklyComparison();

        assertThat(response.currentWeek())
                .isEqualTo(new WeeklyMetricsResponse(currentStart, currentEnd, 0, 0, 0));
        assertThat(response.previousWeek())
                .isEqualTo(new WeeklyMetricsResponse(previousStart, previousEnd, 0, 0, 0));
        verify(workoutRepository).findByDateBetween(currentStart, currentEnd);
        verify(workoutRepository).findByDateBetween(previousStart, previousEnd);
        verifyNoMoreInteractions(workoutRepository);
    }

    @Test
    void calculatesPositiveDifferencesFromDemoTotals() {
        DashboardService dashboardService = serviceWithClockOn(LocalDate.of(2026, 8, 12));
        stubWeek(WEEK_START, currentWeekDemoWorkouts());
        stubWeek(PREVIOUS_WEEK_START, previousWeekDemoWorkouts());

        WeeklyComparisonResponse response = dashboardService.weeklyComparison();

        assertThat(response.currentWeek())
                .isEqualTo(new WeeklyMetricsResponse(WEEK_START, WEEK_END, 5, 180, 1200));
        assertThat(response.previousWeek())
                .isEqualTo(new WeeklyMetricsResponse(PREVIOUS_WEEK_START, PREVIOUS_WEEK_END, 4, 150, 980));
        assertThat(response.difference()).isEqualTo(new WeeklyDifferenceResponse(1, 30, 220));
    }

    @Test
    void calculatesNegativeDifferencesWhenCurrentWeekIsLower() {
        DashboardService dashboardService = serviceWithClockOn(LocalDate.of(2026, 8, 12));
        stubWeek(WEEK_START, List.of(
                new Workout(WorkoutType.RUNNING, 30, 300, LocalDate.of(2026, 8, 12))));
        stubWeek(PREVIOUS_WEEK_START, previousWeekDemoWorkouts());

        WeeklyComparisonResponse response = dashboardService.weeklyComparison();

        assertThat(response.difference()).isEqualTo(new WeeklyDifferenceResponse(-3, -120, -680));
    }

    @Test
    void calculatesZeroDifferencesWhenTotalsAreEqual() {
        DashboardService dashboardService = serviceWithClockOn(LocalDate.of(2026, 8, 12));
        stubWeek(WEEK_START, List.of(
                new Workout(WorkoutType.RUNNING, 30, 300, LocalDate.of(2026, 8, 10))));
        stubWeek(PREVIOUS_WEEK_START, List.of(
                new Workout(WorkoutType.RUNNING, 30, 300, LocalDate.of(2026, 8, 3))));

        WeeklyComparisonResponse response = dashboardService.weeklyComparison();

        assertThat(response.difference()).isEqualTo(new WeeklyDifferenceResponse(0, 0, 0));
    }

    @Test
    void usesCurrentTotalsAsDifferenceWhenPreviousWeekIsEmpty() {
        DashboardService dashboardService = serviceWithClockOn(LocalDate.of(2026, 8, 12));
        stubWeek(WEEK_START, List.of(
                new Workout(WorkoutType.RUNNING, 30, 300, LocalDate.of(2026, 8, 12))));
        stubWeek(PREVIOUS_WEEK_START, List.of());

        WeeklyComparisonResponse response = dashboardService.weeklyComparison();

        assertThat(response.previousWeek())
                .isEqualTo(new WeeklyMetricsResponse(PREVIOUS_WEEK_START, PREVIOUS_WEEK_END, 0, 0, 0));
        assertThat(response.difference()).isEqualTo(new WeeklyDifferenceResponse(1, 30, 300));
    }

    @Test
    void negatesPreviousTotalsWhenCurrentWeekIsEmpty() {
        DashboardService dashboardService = serviceWithClockOn(LocalDate.of(2026, 8, 12));
        stubWeek(WEEK_START, List.of());
        stubWeek(PREVIOUS_WEEK_START, previousWeekDemoWorkouts());

        WeeklyComparisonResponse response = dashboardService.weeklyComparison();

        assertThat(response.currentWeek()).isEqualTo(new WeeklyMetricsResponse(WEEK_START, WEEK_END, 0, 0, 0));
        assertThat(response.difference()).isEqualTo(new WeeklyDifferenceResponse(-4, -150, -980));
    }

    @Test
    void returnsZeroDifferencesWhenBothWeeksAreEmpty() {
        DashboardService dashboardService = serviceWithClockOn(LocalDate.of(2026, 8, 12));
        stubWeek(WEEK_START, List.of());
        stubWeek(PREVIOUS_WEEK_START, List.of());

        WeeklyComparisonResponse response = dashboardService.weeklyComparison();

        assertThat(response.difference()).isEqualTo(new WeeklyDifferenceResponse(0, 0, 0));
    }

    @Test
    void keepsDifferenceAsLongWithoutOverflow() {
        DashboardService dashboardService = serviceWithClockOn(LocalDate.of(2026, 8, 12));
        stubWeek(WEEK_START, List.of(
                new Workout(WorkoutType.RUNNING, 30, 2_000_000_000, LocalDate.of(2026, 8, 10)),
                new Workout(WorkoutType.CYCLING, 30, 2_000_000_000, LocalDate.of(2026, 8, 11))));
        stubWeek(PREVIOUS_WEEK_START, List.of());

        WeeklyComparisonResponse response = dashboardService.weeklyComparison();

        assertThat(response.currentWeek().totalCalories()).isEqualTo(4_000_000_000L);
        assertThat(response.difference().calories()).isEqualTo(4_000_000_000L);
    }

    @Test
    void readsTheClockOnlyOnceForTheComparison() {
        CountingClock clock = new CountingClock(Clock.fixed(
                LocalDate.of(2026, 8, 12).atTime(12, 0).atZone(GUAYAQUIL).toInstant(), GUAYAQUIL));
        DashboardService dashboardService = new DashboardService(workoutRepository, clock);

        dashboardService.weeklyComparison();

        assertThat(clock.instantCalls()).isEqualTo(1);
    }

    private void stubWeek(LocalDate start, List<Workout> workouts) {
        when(workoutRepository.findByDateBetween(start, start.plusDays(6))).thenReturn(workouts);
    }

    private static List<Workout> currentWeekDemoWorkouts() {
        return List.of(
                new Workout(WorkoutType.RUNNING, 20, 200, LocalDate.of(2026, 8, 10)),
                new Workout(WorkoutType.WEIGHTS, 60, 360, LocalDate.of(2026, 8, 11)),
                new Workout(WorkoutType.CYCLING, 20, 160, LocalDate.of(2026, 8, 12)),
                new Workout(WorkoutType.SWIMMING, 20, 180, LocalDate.of(2026, 8, 13)),
                new Workout(WorkoutType.WALKING, 60, 300, LocalDate.of(2026, 8, 14)));
    }

    private static List<Workout> previousWeekDemoWorkouts() {
        return List.of(
                new Workout(WorkoutType.CYCLING, 30, 240, LocalDate.of(2026, 8, 3)),
                new Workout(WorkoutType.WEIGHTS, 40, 240, LocalDate.of(2026, 8, 5)),
                new Workout(WorkoutType.SWIMMING, 25, 225, LocalDate.of(2026, 8, 7)),
                new Workout(WorkoutType.WALKING, 55, 275, LocalDate.of(2026, 8, 8)));
    }

    private DashboardService serviceWithClockOn(LocalDate date) {
        return serviceWithFixedInstant(date.atTime(12, 0).atZone(GUAYAQUIL).toInstant(), GUAYAQUIL);
    }

    private static final class CountingClock extends Clock {

        private final Clock delegate;
        private final AtomicInteger instantCalls = new AtomicInteger();

        private CountingClock(Clock delegate) {
            this.delegate = delegate;
        }

        @Override
        public ZoneId getZone() {
            return delegate.getZone();
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new CountingClock(delegate.withZone(zone));
        }

        @Override
        public Instant instant() {
            instantCalls.incrementAndGet();
            return delegate.instant();
        }

        private int instantCalls() {
            return instantCalls.get();
        }
    }

    private DashboardService serviceWithFixedInstant(Instant instant, ZoneId zone) {
        return new DashboardService(workoutRepository, Clock.fixed(instant, zone));
    }
}
