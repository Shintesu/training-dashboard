package com.example.trainingdashboard.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.trainingdashboard.repository.WorkoutRepository;
import com.example.trainingdashboard.service.DashboardService;
import com.example.trainingdashboard.service.WorkoutService;

@ExtendWith(OutputCaptureExtension.class)
class DemoDataLoaderStartupTest {

    private static final String CONNECTION_STRING =
            "jdbc:postgresql://leak.invalid:5432/demo?password=super-secret-startup";

    @Test
    void doesNotThrowAndLogsSanitizedErrorWhenDashboardFails(CapturedOutput output) {
        DashboardService dashboardService = mock(DashboardService.class);
        when(dashboardService.weeklyMetrics()).thenThrow(new IllegalStateException(CONNECTION_STRING));
        DemoDataLoader loader = new DemoDataLoader(
                mock(WorkoutService.class),
                dashboardService,
                mock(WorkoutRepository.class),
                Clock.fixed(Instant.parse("2026-08-12T15:00:00Z"), ZoneId.of("America/Guayaquil")),
                mock(TransactionTemplate.class));

        assertThatCode(() -> loader.run()).doesNotThrowAnyException();

        assertThat(output.getAll())
                .contains("ERROR", "IllegalStateException")
                .doesNotContain(CONNECTION_STRING, "super-secret-startup");
    }
}
