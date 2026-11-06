package com.example.trainingdashboard.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.example.trainingdashboard.entity.Workout;
import com.example.trainingdashboard.enums.WorkoutType;

@SpringBootTest
@ActiveProfiles("test")
class WorkoutRepositoryIntegrationTest {

    private static final LocalDate START = LocalDate.of(2026, 8, 10);
    private static final LocalDate END = LocalDate.of(2026, 8, 16);

    @Autowired
    private WorkoutRepository workoutRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void deleteWorkoutsBefore() {
        workoutRepository.deleteAll();
    }

    @AfterEach
    void deleteWorkoutsAfter() {
        workoutRepository.deleteAll();
    }

    @Test
    void createsWorkoutsTableWithTheExpectedColumns() {
        List<Map<String, Object>> columns = jdbcTemplate.queryForList("""
                select column_name, data_type, is_nullable, character_maximum_length
                from information_schema.columns
                where table_schema = 'public' and table_name = 'workouts'
                """);

        assertThat(columns).hasSize(5);
        assertThat(columns).extracting(column -> column.get("column_name"))
                .containsExactlyInAnyOrder("id", "type", "duration_minutes", "calories", "workout_date");
        assertColumn(columns, "id", "bigint", null);
        assertColumn(columns, "type", "character varying", 20);
        assertColumn(columns, "duration_minutes", "integer", null);
        assertColumn(columns, "calories", "integer", null);
        assertColumn(columns, "workout_date", "date", null);
    }

    @Test
    void storesWorkoutTypeAsText() {
        Workout workout = workoutRepository.save(
                new Workout(WorkoutType.RUNNING, 30, 300, LocalDate.of(2026, 8, 12)));

        String storedType = jdbcTemplate.queryForObject(
                "select type from workouts where id = ?", String.class, workout.getId());

        assertThat(storedType).isEqualTo("RUNNING");
    }

    @Test
    void findByDateBetweenIncludesBothEndsAndExcludesSurroundingDays() {
        Workout previousDay = saveWorkout(WorkoutType.WALKING, START.minusDays(1));
        Workout onStart = saveWorkout(WorkoutType.RUNNING, START);
        Workout inTheMiddle = saveWorkout(WorkoutType.CYCLING, START.plusDays(3));
        Workout onEnd = saveWorkout(WorkoutType.SWIMMING, END);
        Workout nextDay = saveWorkout(WorkoutType.WEIGHTS, END.plusDays(1));

        List<Workout> result = workoutRepository.findByDateBetween(START, END);

        assertThat(result).extracting(Workout::getId)
                .containsExactlyInAnyOrder(onStart.getId(), inTheMiddle.getId(), onEnd.getId());
        assertThat(result).extracting(Workout::getId)
                .doesNotContain(previousDay.getId(), nextDay.getId());
    }

    @Test
    void findAllByOrderByDateDescIdDescSortsByDateThenIdDescending() {
        Workout oldest = saveWorkout(WorkoutType.RUNNING, START);
        Workout newest = saveWorkout(WorkoutType.CYCLING, START.plusDays(2));
        Workout sameDayAsNewest = saveWorkout(WorkoutType.SWIMMING, START.plusDays(2));

        List<Workout> result = workoutRepository.findAllByOrderByDateDescIdDesc();

        assertThat(result).extracting(Workout::getId)
                .containsExactly(sameDayAsNewest.getId(), newest.getId(), oldest.getId());
    }

    private void assertColumn(List<Map<String, Object>> columns, String name, String dataType, Integer maximumLength) {
        Map<String, Object> column = columns.stream()
                .filter(candidate -> name.equals(candidate.get("column_name")))
                .findFirst()
                .orElseThrow();

        assertThat(column.get("data_type")).isEqualTo(dataType);
        assertThat(column.get("is_nullable")).isEqualTo("NO");
        if (maximumLength == null) {
            assertThat(column.get("character_maximum_length")).isNull();
        } else {
            assertThat(((Number) column.get("character_maximum_length")).intValue()).isEqualTo(maximumLength);
        }
    }

    private Workout saveWorkout(WorkoutType type, LocalDate date) {
        return workoutRepository.save(new Workout(type, 30, 300, date));
    }
}
