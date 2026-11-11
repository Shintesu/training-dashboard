package com.example.trainingdashboard.config;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.trainingdashboard.dto.WorkoutRequest;
import com.example.trainingdashboard.dto.WeeklyMetricsResponse;
import com.example.trainingdashboard.enums.WorkoutType;
import com.example.trainingdashboard.exception.SanitizedStackTrace;
import com.example.trainingdashboard.repository.WorkoutRepository;
import com.example.trainingdashboard.service.DashboardService;
import com.example.trainingdashboard.service.WorkoutService;

@Component
@Profile("demo")
@Lazy(false)
@EnableScheduling
public class DemoDataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataLoader.class);

    private static final List<DemoWorkout> PREVIOUS_WEEK_WORKOUTS = List.of(
            new DemoWorkout(WorkoutType.CYCLING, 30, 0),
            new DemoWorkout(WorkoutType.WEIGHTS, 40, 2),
            new DemoWorkout(WorkoutType.SWIMMING, 25, 4),
            new DemoWorkout(WorkoutType.WALKING, 55, 5));

    private static final List<DemoWorkout> CURRENT_WEEK_WORKOUTS = List.of(
            new DemoWorkout(WorkoutType.RUNNING, 20, 0),
            new DemoWorkout(WorkoutType.WEIGHTS, 60, 1),
            new DemoWorkout(WorkoutType.CYCLING, 20, 2),
            new DemoWorkout(WorkoutType.SWIMMING, 20, 3),
            new DemoWorkout(WorkoutType.WALKING, 60, 4));

    private static final int DEMO_WORKOUT_COUNT =
            PREVIOUS_WEEK_WORKOUTS.size() + CURRENT_WEEK_WORKOUTS.size();

    private final WorkoutService workoutService;
    private final DashboardService dashboardService;
    private final WorkoutRepository workoutRepository;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;

    public DemoDataLoader(WorkoutService workoutService, DashboardService dashboardService,
            WorkoutRepository workoutRepository, Clock clock, TransactionTemplate transactionTemplate) {
        this.workoutService = workoutService;
        this.dashboardService = dashboardService;
        this.workoutRepository = workoutRepository;
        this.clock = clock;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public void run(String... args) {
        refreshIfStale();
    }

    @Scheduled(cron = "0 5 0 * * *", zone = "${app.time-zone}")
    void refreshOnSchedule() {
        refreshIfStale();
    }

    public synchronized void refreshIfStale() {
        try {
            WeeklyMetricsResponse currentWeek = dashboardService.weeklyMetrics();
            if (currentWeek.totalWorkouts() > 0) {
                log.debug("Demo data is current for week starting {}", currentWeek.startDate());
                return;
            }
            transactionTemplate.executeWithoutResult(status -> replaceWithDemoData(currentWeek.startDate()));
            log.info("Loaded {} demo workouts", DEMO_WORKOUT_COUNT);
        } catch (RuntimeException exception) {
            log.error("Demo data refresh failed");
            SanitizedStackTrace.log(log, exception);
        }
    }

    private void replaceWithDemoData(LocalDate start) {
        workoutRepository.deleteAllInBatch();
        LocalDate today = LocalDate.now(clock);
        LocalDate previousWeekStart = start.minusDays(7);
        for (DemoWorkout demoWorkout : PREVIOUS_WEEK_WORKOUTS) {
            register(demoWorkout, previousWeekStart.plusDays(demoWorkout.dayOffset()));
        }
        for (DemoWorkout demoWorkout : CURRENT_WEEK_WORKOUTS) {
            LocalDate scheduledDate = start.plusDays(demoWorkout.dayOffset());
            LocalDate date = scheduledDate.isAfter(today) ? today : scheduledDate;
            register(demoWorkout, date);
        }
    }

    private void register(DemoWorkout demoWorkout, LocalDate date) {
        workoutService.registerWorkout(
                new WorkoutRequest(demoWorkout.type(), demoWorkout.durationMinutes(), date));
    }

    private record DemoWorkout(WorkoutType type, int durationMinutes, int dayOffset) {
    }
}
