package com.example.trainingdashboard.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfig {

    @Bean
    public Clock clock(@Value("${app.time-zone}") String timeZone) {
        // An invalid zone fails here, at startup, instead of on the first request.
        return Clock.system(ZoneId.of(timeZone));
    }
}
