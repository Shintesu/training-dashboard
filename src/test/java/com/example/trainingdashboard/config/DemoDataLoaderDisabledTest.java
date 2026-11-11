package com.example.trainingdashboard.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.config.ScheduledTaskHolder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class DemoDataLoaderDisabledTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void doesNotRegisterLoaderOrScheduledTasksWithoutDemoProfile() {
        assertThat(applicationContext.getBeanNamesForType(DemoDataLoader.class)).isEmpty();

        Map<String, ScheduledTaskHolder> holders =
                applicationContext.getBeansOfType(ScheduledTaskHolder.class);
        assertThat(holders.values())
                .allSatisfy(holder -> assertThat(holder.getScheduledTasks()).isEmpty());
    }
}
