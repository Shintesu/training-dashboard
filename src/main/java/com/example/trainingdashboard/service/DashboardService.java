package com.example.trainingdashboard.service;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.trainingdashboard.dto.WeeklyMetricsResponse;
import com.example.trainingdashboard.entity.Workout;
import com.example.trainingdashboard.repository.WorkoutRepository;

@Service
public class DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);

    private final WorkoutRepository workoutRepository;
    private final Clock clock;

    public DashboardService(WorkoutRepository workoutRepository, Clock clock) {
        this.workoutRepository = workoutRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public WeeklyMetricsResponse weeklyMetrics() {
        LocalDate today = LocalDate.now(clock);
        LocalDate startDate = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return metricsForWeek(startDate);
    }

    private WeeklyMetricsResponse metricsForWeek(LocalDate startDate) {
        LocalDate endDate = startDate.plusDays(6);
        List<Workout> workouts = workoutRepository.findByDateBetween(startDate, endDate);
        long totalDurationMinutes = workouts.stream()
                .mapToLong(Workout::getDurationMinutes)
                .sum();
        long totalCalories = workouts.stream()
                .mapToLong(Workout::getCalories)
                .sum();
        log.debug("Metrics for week {} to {}: {} workouts", startDate, endDate, workouts.size());
        return new WeeklyMetricsResponse(startDate, endDate, workouts.size(), totalDurationMinutes,
                totalCalories);
    }
}
