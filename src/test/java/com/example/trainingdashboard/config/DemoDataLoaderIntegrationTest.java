package com.example.trainingdashboard.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.example.trainingdashboard.dto.WeeklyComparisonResponse;
import com.example.trainingdashboard.entity.Workout;
import com.example.trainingdashboard.enums.WorkoutType;
import com.example.trainingdashboard.repository.WorkoutRepository;
import com.example.trainingdashboard.service.DashboardService;
import com.example.trainingdashboard.service.WorkoutService;

@SpringBootTest
@ActiveProfiles({"test", "demo"})
@ExtendWith(OutputCaptureExtension.class)
class DemoDataLoaderIntegrationTest {

    private static final Instant INITIAL_INSTANT = Instant.parse("2026-08-12T15:00:00Z");
    private static final ZoneId ZONE = ZoneId.of("America/Guayaquil");
    private static final LocalDate WEEK_START = LocalDate.of(2026, 8, 10);
    private static final LocalDate WEEK_END = LocalDate.of(2026, 8, 16);

    @Autowired
    private DemoDataLoader demoDataLoader;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private WorkoutRepository workoutRepository;

    @Autowired
    private MutableClock clock;

    @MockitoSpyBean
    private WorkoutService workoutService;

    @TestConfiguration
    static class FixedClockConfiguration {

        @Bean(name = "demoTestClock")
        @Primary
        MutableClock demoTestClock() {
            return new MutableClock(INITIAL_INSTANT, ZONE);
        }
    }

    @BeforeEach
    void deleteWorkoutsBefore() {
        workoutRepository.deleteAll();
    }

    @AfterEach
    void restoreClockAndDeleteWorkouts() {
        clock.reset();
        workoutRepository.deleteAll();
    }

    @Test
    void loadsNineWorkoutsWithExpectedMetricsAndDates() {
        clock.setDate(LocalDate.of(2026, 8, 12));

        demoDataLoader.refreshIfStale();

        WeeklyComparisonResponse comparison = dashboardService.weeklyComparison();
        assertThat(comparison.currentWeek().totalWorkouts()).isEqualTo(5);
        assertThat(comparison.currentWeek().totalDurationMinutes()).isEqualTo(180);
        assertThat(comparison.currentWeek().totalCalories()).isEqualTo(1200);
        assertThat(comparison.previousWeek().totalWorkouts()).isEqualTo(4);
        assertThat(comparison.previousWeek().totalDurationMinutes()).isEqualTo(150);
        assertThat(comparison.previousWeek().totalCalories()).isEqualTo(980);
        assertThat(comparison.difference().workouts()).isEqualTo(1);
        assertThat(comparison.difference().durationMinutes()).isEqualTo(30);
        assertThat(comparison.difference().calories()).isEqualTo(220);

        List<Workout> workouts = workoutRepository.findAll();
        assertThat(workouts).hasSize(9);
        assertThat(workouts)
                .extracting(Workout::getType, Workout::getDate)
                .containsExactlyInAnyOrder(
                        tuple(WorkoutType.RUNNING, LocalDate.of(2026, 8, 10)),
                        tuple(WorkoutType.WEIGHTS, LocalDate.of(2026, 8, 11)),
                        tuple(WorkoutType.CYCLING, LocalDate.of(2026, 8, 12)),
                        tuple(WorkoutType.SWIMMING, LocalDate.of(2026, 8, 12)),
                        tuple(WorkoutType.WALKING, LocalDate.of(2026, 8, 12)),
                        tuple(WorkoutType.CYCLING, LocalDate.of(2026, 8, 3)),
                        tuple(WorkoutType.WEIGHTS, LocalDate.of(2026, 8, 5)),
                        tuple(WorkoutType.SWIMMING, LocalDate.of(2026, 8, 7)),
                        tuple(WorkoutType.WALKING, LocalDate.of(2026, 8, 8)));
    }

    @Test
    void keepsCurrentWeekDatesOnMonday() {
        clock.setDate(LocalDate.of(2026, 8, 10));

        demoDataLoader.refreshIfStale();

        assertThat(workoutRepository.findByDateBetween(WEEK_START, WEEK_END))
                .hasSize(5)
                .extracting(Workout::getDate)
                .containsOnly(LocalDate.of(2026, 8, 10));
    }

    @Test
    void doesNotClipCurrentWeekDatesOnSunday() {
        clock.setDate(LocalDate.of(2026, 8, 16));

        demoDataLoader.refreshIfStale();

        assertThat(workoutRepository.findByDateBetween(WEEK_START, WEEK_END))
                .hasSize(5)
                .extracting(Workout::getDate)
                .containsExactlyInAnyOrder(
                        LocalDate.of(2026, 8, 10),
                        LocalDate.of(2026, 8, 11),
                        LocalDate.of(2026, 8, 12),
                        LocalDate.of(2026, 8, 13),
                        LocalDate.of(2026, 8, 14));
    }

    @Test
    void keepsExistingCurrentWeekWorkoutWithoutTouchingIt() {
        clock.setDate(LocalDate.of(2026, 8, 12));
        Workout existing = workoutRepository.save(
                new Workout(WorkoutType.RUNNING, 10, 100, LocalDate.of(2026, 8, 12)));

        demoDataLoader.refreshIfStale();

        assertThat(workoutRepository.findAll())
                .hasSize(1)
                .extracting(Workout::getId)
                .containsExactly(existing.getId());
    }

    @Test
    void replacesOldDataAndVisitorDataWithDemoData() {
        clock.setDate(LocalDate.of(2026, 8, 12));
        Workout oldWorkout = workoutRepository.save(
                new Workout(WorkoutType.WALKING, 20, 100, LocalDate.of(2026, 7, 27)));
        Workout visitorWorkout = workoutRepository.save(
                new Workout(WorkoutType.RUNNING, 15, 150, LocalDate.of(2026, 7, 28)));

        demoDataLoader.refreshIfStale();

        assertThat(workoutRepository.findAll())
                .hasSize(9)
                .extracting(Workout::getId)
                .doesNotContain(oldWorkout.getId(), visitorWorkout.getId());
    }

    @Test
    void callingRefreshTwiceLeavesNineWorkouts() {
        clock.setDate(LocalDate.of(2026, 8, 12));

        demoDataLoader.refreshIfStale();
        demoDataLoader.refreshIfStale();

        assertThat(workoutRepository.findAll()).hasSize(9);
    }

    @Test
    void loadsAllFiveWorkoutTypes() {
        clock.setDate(LocalDate.of(2026, 8, 12));

        demoDataLoader.refreshIfStale();

        assertThat(workoutRepository.findAll())
                .extracting(Workout::getType)
                .contains(WorkoutType.values());
    }

    @Test
    void rollsBackAndSanitizesFailureWhenRegistrationFails(CapturedOutput output) {
        clock.setDate(LocalDate.of(2026, 8, 12));
        Workout previousWorkout = workoutRepository.save(
                new Workout(WorkoutType.WALKING, 55, 275, LocalDate.of(2026, 7, 27)));
        AtomicInteger calls = new AtomicInteger();
        doAnswer(invocation -> {
            if (calls.incrementAndGet() == 3) {
                throw new IllegalStateException(
                        "jdbc:postgresql://leak.invalid:5432/demo?password=super-secret-demo");
            }
            return invocation.callRealMethod();
        }).when(workoutService).registerWorkout(any());

        assertThatCode(() -> demoDataLoader.refreshIfStale()).doesNotThrowAnyException();

        assertThat(workoutRepository.findAll())
                .extracting(Workout::getId)
                .containsExactly(previousWorkout.getId());
        assertThat(output.getAll())
                .contains("IllegalStateException")
                .doesNotContain("jdbc:postgresql://leak.invalid:5432/demo?password=super-secret-demo",
                        "super-secret-demo");
    }

    @Test
    void concurrentRefreshesAlwaysLeaveNineWorkouts() throws Exception {
        clock.setDate(LocalDate.of(2026, 8, 12));
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            for (int repetition = 0; repetition < 20; repetition++) {
                workoutRepository.deleteAll();
                CountDownLatch ready = new CountDownLatch(2);
                CountDownLatch start = new CountDownLatch(1);
                Runnable refresh = () -> {
                    ready.countDown();
                    try {
                        start.await();
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(exception);
                    }
                    demoDataLoader.refreshIfStale();
                };
                Future<?> first = executor.submit(refresh);
                Future<?> second = executor.submit(refresh);
                ready.await();
                start.countDown();
                first.get(10, TimeUnit.SECONDS);
                second.get(10, TimeUnit.SECONDS);
                assertThat(workoutRepository.findAll()).hasSize(9);
            }
        } finally {
            executor.shutdownNow();
        }
    }

    static final class MutableClock extends Clock {

        private final Instant initialInstant;
        private final ZoneId zone;
        private Clock delegate;

        MutableClock(Instant initialInstant, ZoneId zone) {
            this.initialInstant = initialInstant;
            this.zone = zone;
            this.delegate = Clock.fixed(initialInstant, zone);
        }

        void setDate(LocalDate date) {
            delegate = Clock.fixed(date.atTime(12, 0).atZone(zone).toInstant(), zone);
        }

        void reset() {
            delegate = Clock.fixed(initialInstant, zone);
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId newZone) {
            return Clock.fixed(delegate.instant(), newZone);
        }

        @Override
        public Instant instant() {
            return delegate.instant();
        }
    }
}
