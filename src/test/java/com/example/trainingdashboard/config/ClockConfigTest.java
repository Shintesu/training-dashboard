package com.example.trainingdashboard.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Properties;

import org.junit.jupiter.api.Test;

class ClockConfigTest {

    private static final Path APPLICATION_PROPERTIES = Path.of("src/main/resources/application.properties");

    @Test
    void createsClockWithConfiguredZone() {
        Clock clock = new ClockConfig().clock("Europe/Madrid");

        assertThat(clock.getZone()).isEqualTo(ZoneId.of("Europe/Madrid"));
    }

    @Test
    void usesAmericaGuayaquilAsDefaultZone() throws IOException {
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(APPLICATION_PROPERTIES, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }

        assertThat(properties.getProperty("app.time-zone"))
                .isEqualTo("${APP_TIME_ZONE:America/Guayaquil}");
    }

    @Test
    void failsAtStartupForInvalidZone() {
        assertThatThrownBy(() -> new ClockConfig().clock("Not/AZone"))
                .isInstanceOf(DateTimeException.class);
    }
}
