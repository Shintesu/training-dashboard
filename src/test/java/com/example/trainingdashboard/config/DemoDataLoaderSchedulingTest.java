package com.example.trainingdashboard.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.TriggerContext;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.test.context.ActiveProfiles;

import com.example.trainingdashboard.repository.WorkoutRepository;

@SpringBootTest(properties = "spring.main.lazy-initialization=true")
@ActiveProfiles({"test", "demo"})
class DemoDataLoaderSchedulingTest {

    private static final String CRON_EXPRESSION = "0 5 0 * * *";

    @Autowired
    private ScheduledAnnotationBeanPostProcessor scheduledAnnotationBeanPostProcessor;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private WorkoutRepository workoutRepository;

    @AfterEach
    void deleteWorkoutsAfter() {
        workoutRepository.deleteAll();
    }

    @Test
    void registersDailyRefreshTaskWithConfiguredZone() {
        assertThat(applicationContext.getBeanNamesForType(DemoDataLoader.class)).isNotEmpty();

        Set<ScheduledTask> tasks = scheduledAnnotationBeanPostProcessor.getScheduledTasks();
        CronTask cronTask = tasks.stream()
                .map(ScheduledTask::getTask)
                .filter(CronTask.class::isInstance)
                .map(CronTask.class::cast)
                .filter(task -> CRON_EXPRESSION.equals(task.getExpression()))
                .findFirst()
                .orElseThrow();

        assertThat(cronTask.getExpression()).isEqualTo(CRON_EXPRESSION);
        CronTrigger trigger = (CronTrigger) cronTask.getTrigger();
        Instant nextExecution = trigger.nextExecution(new FixedTriggerContext(
                Instant.parse("2026-08-12T00:10:00Z"), ZoneId.of("America/Guayaquil")));
        assertThat(nextExecution).isEqualTo(Instant.parse("2026-08-12T05:05:00Z"));
    }

    private static final class FixedTriggerContext implements TriggerContext {

        private final Clock clock;

        FixedTriggerContext(Instant instant, ZoneId zone) {
            this.clock = Clock.fixed(instant, zone);
        }

        @Override
        public Instant lastScheduledExecution() {
            return null;
        }

        @Override
        public Instant lastActualExecution() {
            return null;
        }

        @Override
        public Instant lastCompletion() {
            return null;
        }

        @Override
        public Clock getClock() {
            return clock;
        }
    }
}
