package com.example.trainingdashboard;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class TrainingDashboardApplicationTests {

    @Autowired
    private Clock clock;

    @Test
    void contextLoadsWithClockInConfiguredZone() {
        assertThat(clock.getZone()).isEqualTo(ZoneId.of("America/Guayaquil"));
    }
}
