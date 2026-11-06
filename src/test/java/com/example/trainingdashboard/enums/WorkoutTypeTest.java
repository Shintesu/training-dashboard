package com.example.trainingdashboard.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class WorkoutTypeTest {

    @ParameterizedTest
    @CsvSource({
            "RUNNING, Correr, 10",
            "CYCLING, Ciclismo, 8",
            "SWIMMING, Natación, 9",
            "WEIGHTS, Pesas, 6",
            "WALKING, Caminata, 5"
    })
    void exposesLabelAndCaloriesPerMinute(WorkoutType type, String label, int caloriesPerMinute) {
        assertThat(type.getLabel()).isEqualTo(label);
        assertThat(type.getCaloriesPerMinute()).isEqualTo(caloriesPerMinute);
    }

    @Test
    void declaresValuesInDisplayOrder() {
        assertThat(WorkoutType.values()).containsExactly(
                WorkoutType.RUNNING,
                WorkoutType.CYCLING,
                WorkoutType.SWIMMING,
                WorkoutType.WEIGHTS,
                WorkoutType.WALKING);
    }
}
